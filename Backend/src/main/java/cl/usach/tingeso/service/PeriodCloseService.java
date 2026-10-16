package cl.usach.tingeso.service;

import cl.usach.tingeso.dto.IncompleteSectionDTO;
import cl.usach.tingeso.dto.PeriodCloseResultDTO;
import cl.usach.tingeso.dto.PeriodCloseValidationDTO;
import cl.usach.tingeso.entity.AcademicPeriodEntity;
import cl.usach.tingeso.entity.AcademicStatus;
import cl.usach.tingeso.entity.CourseEntity;
import cl.usach.tingeso.entity.EnrollmentEntity;
import cl.usach.tingeso.entity.EnrollmentStatus;
import cl.usach.tingeso.entity.SectionEntity;
import cl.usach.tingeso.entity.StudentEntity;
import cl.usach.tingeso.exception.BusinessRuleException;
import cl.usach.tingeso.exception.ResourceNotFoundException;
import cl.usach.tingeso.repository.AcademicPeriodRepository;
import cl.usach.tingeso.repository.CourseRepository;
import cl.usach.tingeso.repository.EnrollmentRepository;
import cl.usach.tingeso.repository.StudentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * PeriodCloseService orchestrates the validation and execution of academic semester closing.
 * Corresponds to Epic 7: Final Grades Registration and Semester Close.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class PeriodCloseService {

    private final AcademicPeriodRepository academicPeriodRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final StudentRepository studentRepository;
    private final CourseRepository courseRepository;

    /**
     * Audits an academic period to verify whether it meets all conditions for semester closure.
     * All active enrollments must have a registered final grade.
     */
    @Transactional(readOnly = true)
    public PeriodCloseValidationDTO checkCloseReadiness(String periodId) {
        AcademicPeriodEntity period = getPeriodOrThrow(periodId);

        if (Boolean.FALSE.equals(period.getIsOpen())) {
            return PeriodCloseValidationDTO.builder()
                    .periodId(periodId)
                    .isOpen(false)
                    .canClose(false)
                    .totalActiveEnrollments(0)
                    .gradedEnrollmentsCount(0)
                    .pendingEnrollmentsCount(0)
                    .incompleteSections(List.of())
                    .build();
        }

        List<EnrollmentEntity> activeEnrollments = enrollmentRepository.findByAcademicPeriod_IdAndStatus(periodId, EnrollmentStatus.INSCRITA);

        List<EnrollmentEntity> pendingEnrollments = activeEnrollments.stream()
                .filter(e -> e.getFinalGrade() == null)
                .toList();

        Map<Long, List<EnrollmentEntity>> sectionPendingMap = pendingEnrollments.stream()
                .collect(Collectors.groupingBy(e -> e.getSection().getId()));

        List<IncompleteSectionDTO> incompleteSections = new ArrayList<>();
        for (Map.Entry<Long, List<EnrollmentEntity>> entry : sectionPendingMap.entrySet()) {
            EnrollmentEntity sample = entry.getValue().getFirst();
            SectionEntity sec = sample.getSection();

            incompleteSections.add(IncompleteSectionDTO.builder()
                    .sectionId(sec.getId())
                    .sectionCode(sec.getSectionCode())
                    .courseCode(sec.getCourse().getCode())
                    .courseName(sec.getCourse().getName())
                    .teacherRun(sec.getTeacher() != null ? sec.getTeacher().getRun() : "N/A")
                    .teacherName(sec.getTeacher() != null ? sec.getTeacher().getFirstName() + " " + sec.getTeacher().getPaternalLastName() : "Sin Docente")
                    .pendingGradesCount(entry.getValue().size())
                    .totalEnrolledCount(sec.getEnrolledCount())
                    .build());
        }

        int totalCount = activeEnrollments.size();
        int pendingCount = pendingEnrollments.size();
        int gradedCount = totalCount - pendingCount;
        boolean canClose = pendingCount == 0;

        return PeriodCloseValidationDTO.builder()
                .periodId(periodId)
                .isOpen(true)
                .canClose(canClose)
                .totalActiveEnrollments(totalCount)
                .gradedEnrollmentsCount(gradedCount)
                .pendingEnrollmentsCount(pendingCount)
                .incompleteSections(incompleteSections)
                .build();
    }

    /**
     * Executes the formal semester close:
     * 1. Validates that 100% of active enrollments have final grades.
     * 2. Sets enrollment status to APROBADA (>= 4.0) or REPROBADA (< 4.0).
     * 3. Updates student academic status: detects second failure of same course (ELIMINADO) or all courses approved (EGRESADO).
     * 4. Freezes the period (isOpen = false).
     */
    @Transactional
    public PeriodCloseResultDTO executePeriodClose(String periodId) {
        AcademicPeriodEntity period = getPeriodOrThrow(periodId);

        if (Boolean.FALSE.equals(period.getIsOpen())) {
            throw new BusinessRuleException("Cannot close academic period " + periodId + ": it is already CLOSED.");
        }

        PeriodCloseValidationDTO validation = checkCloseReadiness(periodId);
        if (!validation.getCanClose()) {
            throw new BusinessRuleException(
                    "Cannot close period " + periodId + ": there are " + validation.getPendingEnrollmentsCount()
                            + " enrollments without a registered final grade across " + validation.getIncompleteSections().size()
                            + " sections."
            );
        }

        List<EnrollmentEntity> activeEnrollments = enrollmentRepository.findByAcademicPeriod_IdAndStatus(periodId, EnrollmentStatus.INSCRITA);

        int approvedCount = 0;
        int failedCount = 0;
        Set<StudentEntity> involvedStudents = new HashSet<>();

        // 1. Transition enrollment statuses based on finalGrade
        for (EnrollmentEntity enrollment : activeEnrollments) {
            Double grade = enrollment.getFinalGrade();
            if (grade >= 4.0) {
                enrollment.setStatus(EnrollmentStatus.APROBADA);
                approvedCount++;
            } else {
                enrollment.setStatus(EnrollmentStatus.REPROBADA);
                failedCount++;
            }
            enrollmentRepository.save(enrollment);
            involvedStudents.add(enrollment.getStudent());
        }

        // 2. Evaluate academic status updates for involved students
        List<String> graduatedStudentRuns = new ArrayList<>();
        List<String> dismissedStudentRuns = new ArrayList<>();

        for (StudentEntity student : involvedStudents) {
            // Check dismissal causal (failed same subject twice or more)
            boolean isDismissed = checkDismissalCondition(student);
            if (isDismissed) {
                student.setAcademicStatus(AcademicStatus.ELIMINADO);
                studentRepository.save(student);
                dismissedStudentRuns.add(student.getRun());
                log.warn("Student {} marked as ELIMINADO due to repeated failure of the same subject.", student.getRun());
                continue;
            }

            // Check graduation condition (approved 100% of study plan courses)
            boolean isGraduated = checkGraduationCondition(student);
            if (isGraduated) {
                student.setAcademicStatus(AcademicStatus.EGRESADO);
                studentRepository.save(student);
                graduatedStudentRuns.add(student.getRun());
                log.info("Student {} marked as EGRESADO: successfully passed all study plan subjects.", student.getRun());
            }
        }

        // 3. Mark academic period as permanently CLOSED
        period.setIsOpen(false);
        academicPeriodRepository.save(period);

        log.info("Academic period {} successfully CLOSED. Processed: {}, Passed: {}, Failed: {}, Graduated: {}, Dismissed: {}",
                periodId, activeEnrollments.size(), approvedCount, failedCount, graduatedStudentRuns.size(), dismissedStudentRuns.size());

        return PeriodCloseResultDTO.builder()
                .periodId(periodId)
                .message("Academic period " + periodId + " successfully closed and finalized.")
                .totalProcessedEnrollments(activeEnrollments.size())
                .approvedCount(approvedCount)
                .failedCount(failedCount)
                .graduatedStudentsCount(graduatedStudentRuns.size())
                .graduatedStudentRuns(graduatedStudentRuns)
                .dismissedStudentsCount(dismissedStudentRuns.size())
                .dismissedStudentRuns(dismissedStudentRuns)
                .build();
    }

    /**
     * Checks if a student has failed the same subject 2 or more times across their historical records.
     */
    private boolean checkDismissalCondition(StudentEntity student) {
        List<EnrollmentEntity> failedEnrollments = enrollmentRepository.findByStudent_RunAndStatus(
                student.getRun(),
                EnrollmentStatus.REPROBADA
        );

        Map<Long, Long> failureCountByCourse = failedEnrollments.stream()
                .collect(Collectors.groupingBy(e -> e.getCourse().getId(), Collectors.counting()));

        return failureCountByCourse.values().stream().anyMatch(count -> count >= 2);
    }

    /**
     * Checks if a student has approved every subject in their active study plan.
     */
    private boolean checkGraduationCondition(StudentEntity student) {
        if (student.getStudyPlan() == null) {
            return false;
        }

        List<CourseEntity> planCourses = courseRepository.findByStudyPlan_Id(student.getStudyPlan().getId());
        if (planCourses.isEmpty()) {
            return false;
        }

        List<EnrollmentEntity> approvedEnrollments = enrollmentRepository.findByStudent_RunAndStatus(
                student.getRun(),
                EnrollmentStatus.APROBADA
        );

        Set<Long> approvedCourseIds = approvedEnrollments.stream()
                .map(e -> e.getCourse().getId())
                .collect(Collectors.toSet());

        Set<Long> planCourseIds = planCourses.stream()
                .map(CourseEntity::getId)
                .collect(Collectors.toSet());

        return approvedCourseIds.containsAll(planCourseIds);
    }

    private AcademicPeriodEntity getPeriodOrThrow(String periodId) {
        return academicPeriodRepository.findById(periodId)
                .orElseThrow(() -> new ResourceNotFoundException("Academic period not found with ID: " + periodId));
    }
}
