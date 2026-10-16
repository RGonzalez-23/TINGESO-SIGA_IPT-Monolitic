package cl.usach.tingeso.service;

import cl.usach.tingeso.dto.CourseHistoryEntryDTO;
import cl.usach.tingeso.dto.CoursePrerequisiteDTO;
import cl.usach.tingeso.dto.CurriculumCourseProgressDTO;
import cl.usach.tingeso.dto.PeriodHistoryDTO;
import cl.usach.tingeso.dto.StudentAcademicHistoryDTO;
import cl.usach.tingeso.dto.StudentCurriculumProgressDTO;
import cl.usach.tingeso.entity.AcademicPeriodEntity;
import cl.usach.tingeso.entity.CourseEntity;
import cl.usach.tingeso.entity.EnrollmentEntity;
import cl.usach.tingeso.entity.EnrollmentStatus;
import cl.usach.tingeso.entity.StudentEntity;
import cl.usach.tingeso.exception.ResourceNotFoundException;
import cl.usach.tingeso.repository.CourseRepository;
import cl.usach.tingeso.repository.EnrollmentRepository;
import cl.usach.tingeso.repository.StudentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * AcademicHistoryService generates academic transcripts, period histories, and curriculum progress maps.
 * Corresponds to Epic 7: Final Grades Registration and Semester Close.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AcademicHistoryService {

    private final StudentRepository studentRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final CourseRepository courseRepository;

    /**
     * Generates a complete chronological transcript of a student's academic history grouped by period.
     */
    @Transactional(readOnly = true)
    public StudentAcademicHistoryDTO getStudentAcademicHistory(String studentRun) {
        StudentEntity student = getStudentOrThrow(studentRun);

        List<EnrollmentEntity> enrollments = enrollmentRepository.findByStudent_RunOrderByAcademicPeriod_IdAscEnrollmentDateAsc(studentRun);

        // Group enrollments by academic period keeping chronological order
        Map<String, List<EnrollmentEntity>> byPeriod = enrollments.stream()
                .filter(e -> e.getStatus() != EnrollmentStatus.ANULADA)
                .collect(Collectors.groupingBy(
                        e -> e.getAcademicPeriod().getId(),
                        LinkedHashMap::new,
                        Collectors.toList()
                ));

        List<PeriodHistoryDTO> periodHistories = new ArrayList<>();
        double totalGradePoints = 0.0;
        int totalGradedCourses = 0;
        int totalApprovedCredits = 0;
        Set<Long> approvedCourseIds = new java.util.HashSet<>();

        for (Map.Entry<String, List<EnrollmentEntity>> entry : byPeriod.entrySet()) {
            List<EnrollmentEntity> periodEnrollments = entry.getValue();
            AcademicPeriodEntity period = periodEnrollments.getFirst().getAcademicPeriod();

            List<CourseHistoryEntryDTO> courseEntries = new ArrayList<>();
            double periodGradeSum = 0.0;
            int periodGradedCount = 0;
            int periodEnrolledCredits = 0;
            int periodApprovedCredits = 0;

            for (EnrollmentEntity e : periodEnrollments) {
                int credits = e.getCourse().getSctCredits() != null ? e.getCourse().getSctCredits() : 0;
                periodEnrolledCredits += credits;

                if (e.getFinalGrade() != null) {
                    periodGradeSum += e.getFinalGrade();
                    periodGradedCount++;
                    totalGradePoints += e.getFinalGrade();
                    totalGradedCourses++;
                }

                if (e.getStatus() == EnrollmentStatus.APROBADA) {
                    periodApprovedCredits += credits;
                    totalApprovedCredits += credits;
                    approvedCourseIds.add(e.getCourse().getId());
                }

                courseEntries.add(CourseHistoryEntryDTO.builder()
                        .enrollmentId(e.getId())
                        .courseId(e.getCourse().getId())
                        .courseCode(e.getCourse().getCode())
                        .courseName(e.getCourse().getName())
                        .semester(e.getCourse().getSemester())
                        .sctCredits(credits)
                        .sectionCode(e.getSection() != null ? e.getSection().getSectionCode() : "N/A")
                        .teacherName(e.getSection() != null && e.getSection().getTeacher() != null
                                ? e.getSection().getTeacher().getFirstName() + " " + e.getSection().getTeacher().getPaternalLastName()
                                : "Docente por asignar")
                        .finalGrade(e.getFinalGrade())
                        .status(e.getStatus().name())
                        .build());
            }

            Double periodGpa = periodGradedCount > 0
                    ? BigDecimal.valueOf(periodGradeSum / periodGradedCount).setScale(1, RoundingMode.HALF_UP).doubleValue()
                    : null;

            periodHistories.add(PeriodHistoryDTO.builder()
                    .periodId(period.getId())
                    .year(period.getYear())
                    .semester(period.getSemester())
                    .isOpen(period.getIsOpen())
                    .periodGpa(periodGpa)
                    .enrolledCredits(periodEnrolledCredits)
                    .approvedCredits(periodApprovedCredits)
                    .courses(courseEntries)
                    .build());
        }

        Double cumulativeGpa = totalGradedCourses > 0
                ? BigDecimal.valueOf(totalGradePoints / totalGradedCourses).setScale(1, RoundingMode.HALF_UP).doubleValue()
                : null;

        List<CourseEntity> planCourses = student.getStudyPlan() != null
                ? courseRepository.findByStudyPlan_Id(student.getStudyPlan().getId())
                : List.of();

        int totalPlanCredits = planCourses.stream()
                .mapToInt(c -> c.getSctCredits() != null ? c.getSctCredits() : 0)
                .sum();

        return StudentAcademicHistoryDTO.builder()
                .studentRun(student.getRun())
                .studentFullName(student.getFirstName() + " " + student.getPaternalLastName() + " " + student.getMaternalLastName())
                .careerCode(student.getCareer() != null ? student.getCareer().getCode() : "N/A")
                .careerName(student.getCareer() != null ? student.getCareer().getName() : "N/A")
                .studyPlanCode(student.getStudyPlan() != null ? student.getStudyPlan().getCode() : "N/A")
                .academicStatus(student.getAcademicStatus())
                .isMatriculated(student.getIsMatriculated())
                .cumulativeGpa(cumulativeGpa)
                .totalApprovedCredits(totalApprovedCredits)
                .totalPlanCredits(totalPlanCredits)
                .totalApprovedCourses(approvedCourseIds.size())
                .totalPlanCourses(planCourses.size())
                .periods(periodHistories)
                .build();
    }

    /**
     * Generates the curriculum progress map for a student, calculating color-coded status for each course.
     * Statuses:
     * - "APPROVED": Verde (passed)
     * - "FAILED": Rojo (failed, includes failure count)
     * - "ENROLLED": Amarillo (currently registered in an active period)
     * - "PENDING": Blanco (not yet taken)
     */
    @Transactional(readOnly = true)
    public StudentCurriculumProgressDTO getStudentCurriculumProgress(String studentRun) {
        StudentEntity student = getStudentOrThrow(studentRun);

        if (student.getStudyPlan() == null) {
            throw new ResourceNotFoundException("Student does not have an assigned study plan.");
        }

        List<CourseEntity> planCourses = courseRepository.findByStudyPlan_IdOrderBySemesterAscCodeAsc(student.getStudyPlan().getId());
        List<EnrollmentEntity> enrollments = enrollmentRepository.findByStudent_Run(studentRun);

        Map<Long, List<EnrollmentEntity>> enrollmentsByCourse = enrollments.stream()
                .filter(e -> e.getStatus() != EnrollmentStatus.ANULADA)
                .collect(Collectors.groupingBy(e -> e.getCourse().getId()));

        List<CurriculumCourseProgressDTO> courseProgressList = new ArrayList<>();
        int approvedCount = 0;
        int failedCountTotal = 0;
        int enrolledCountTotal = 0;

        for (CourseEntity course : planCourses) {
            List<EnrollmentEntity> courseEnrollments = enrollmentsByCourse.getOrDefault(course.getId(), List.of());

            // Count how many times failed
            int failCount = (int) courseEnrollments.stream()
                    .filter(e -> e.getStatus() == EnrollmentStatus.REPROBADA)
                    .count();

            boolean isApproved = courseEnrollments.stream().anyMatch(e -> e.getStatus() == EnrollmentStatus.APROBADA);
            boolean isCurrentlyEnrolled = courseEnrollments.stream().anyMatch(e -> e.getStatus() == EnrollmentStatus.INSCRITA);

            // Latest grade
            Double latestGrade = courseEnrollments.stream()
                    .filter(e -> e.getFinalGrade() != null)
                    .max(Comparator.comparing(EnrollmentEntity::getEnrollmentDate))
                    .map(EnrollmentEntity::getFinalGrade)
                    .orElse(null);

            String visualStatus;
            if (isApproved) {
                visualStatus = "APPROVED"; // Verde
                approvedCount++;
            } else if (isCurrentlyEnrolled) {
                visualStatus = "ENROLLED"; // Amarillo
                enrolledCountTotal++;
            } else if (failCount > 0) {
                visualStatus = "FAILED"; // Rojo
                failedCountTotal++;
            } else {
                visualStatus = "PENDING"; // Blanco
            }

            List<CoursePrerequisiteDTO> prereqs = course.getPrerequisites().stream()
                    .map(p -> CoursePrerequisiteDTO.builder()
                            .id(p.getId())
                            .code(p.getCode())
                            .name(p.getName())
                            .build())
                    .toList();

            courseProgressList.add(CurriculumCourseProgressDTO.builder()
                    .courseId(course.getId())
                    .code(course.getCode())
                    .name(course.getName())
                    .semester(course.getSemester())
                    .sctCredits(course.getSctCredits())
                    .theoryHours(course.getTheoryHours())
                    .exerciseHours(course.getExerciseHours())
                    .laboratoryHours(course.getLaboratoryHours())
                    .status(visualStatus)
                    .failCount(failCount)
                    .latestGrade(latestGrade)
                    .prerequisites(prereqs)
                    .build());
        }

        return StudentCurriculumProgressDTO.builder()
                .studentRun(student.getRun())
                .studentFullName(student.getFirstName() + " " + student.getPaternalLastName() + " " + student.getMaternalLastName())
                .careerName(student.getCareer() != null ? student.getCareer().getName() : "N/A")
                .studyPlanId(student.getStudyPlan().getId())
                .studyPlanCode(student.getStudyPlan().getCode())
                .academicStatus(student.getAcademicStatus())
                .approvedCoursesCount(approvedCount)
                .failedCoursesCount(failedCountTotal)
                .enrolledCoursesCount(enrolledCountTotal)
                .totalPlanCoursesCount(planCourses.size())
                .courses(courseProgressList)
                .build();
    }

    private StudentEntity getStudentOrThrow(String run) {
        return studentRepository.findById(run)
                .orElseThrow(() -> new ResourceNotFoundException("Student not found with RUN: " + run));
    }
}
