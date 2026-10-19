package cl.usach.tingeso.integration;

import cl.usach.tingeso.dto.PeriodCloseResultDTO;
import cl.usach.tingeso.dto.SectionGradesSubmissionDTO;
import cl.usach.tingeso.dto.StudentGradeEntryDTO;
import cl.usach.tingeso.entity.AcademicPeriodEntity;
import cl.usach.tingeso.entity.AcademicStatus;
import cl.usach.tingeso.entity.CareerEntity;
import cl.usach.tingeso.entity.CourseEntity;
import cl.usach.tingeso.entity.EnrollmentEntity;
import cl.usach.tingeso.entity.EnrollmentStatus;
import cl.usach.tingeso.entity.SectionEntity;
import cl.usach.tingeso.entity.StudentEntity;
import cl.usach.tingeso.entity.StudyPlanEntity;
import cl.usach.tingeso.entity.TeacherEntity;
import cl.usach.tingeso.entity.TimeBlockEntity;
import cl.usach.tingeso.exception.BusinessRuleException;
import cl.usach.tingeso.repository.AcademicPeriodRepository;
import cl.usach.tingeso.repository.CareerRepository;
import cl.usach.tingeso.repository.CourseRepository;
import cl.usach.tingeso.repository.EnrollmentRepository;
import cl.usach.tingeso.repository.SectionRepository;
import cl.usach.tingeso.repository.StudentRepository;
import cl.usach.tingeso.repository.StudyPlanRepository;
import cl.usach.tingeso.repository.TeacherRepository;
import cl.usach.tingeso.repository.TimeBlockRepository;
import cl.usach.tingeso.service.GradeService;
import cl.usach.tingeso.service.PeriodCloseService;
import cl.usach.tingeso.util.RunValidatorUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Integration test suite for Process 2: Semester Close and Final Grades (Epic 7).
 * Tests complete database persistence, JPA cascades, academic progression, and business rules.
 * Includes 3 success scenarios and 3 rejection scenarios as mandated by the evaluation rubric.
 */
@SpringBootTest
@ActiveProfiles("test")
@TestPropertySource(locations = "classpath:application-test.properties")
@Transactional
public class PeriodCloseProcessIntegrationTest {

    @Autowired
    private PeriodCloseService periodCloseService;

    @Autowired
    private GradeService gradeService;

    @Autowired
    private AcademicPeriodRepository academicPeriodRepository;

    @Autowired
    private EnrollmentRepository enrollmentRepository;

    @Autowired
    private StudentRepository studentRepository;

    @Autowired
    private CourseRepository courseRepository;

    @Autowired
    private SectionRepository sectionRepository;

    @Autowired
    private TeacherRepository teacherRepository;

    @Autowired
    private TimeBlockRepository timeBlockRepository;

    @Autowired
    private CareerRepository careerRepository;

    @Autowired
    private StudyPlanRepository studyPlanRepository;

    private AcademicPeriodEntity openPeriod;
    private CareerEntity career;
    private StudyPlanEntity studyPlan;
    private TeacherEntity teacher;

    @BeforeEach
    void setUp() {
        openPeriod = academicPeriodRepository.findByIsOpenTrue()
                .orElseGet(() -> academicPeriodRepository.save(
                        AcademicPeriodEntity.builder().id("2027-1").year(2027).semester(1).isOpen(true).build()
                ));

        career = careerRepository.findById("10450")
                .orElseThrow(() -> new IllegalStateException("Career 10450 not found in database"));

        studyPlan = studyPlanRepository.findByCareer_CodeAndIsActiveTrue("10450")
                .orElseThrow(() -> new IllegalStateException("Study plan not found for 10450"));

        teacher = teacherRepository.findById("11111111-1")
                .orElseThrow(() -> new IllegalStateException("Teacher 11111111-1 not found"));
    }

    private CourseEntity getCourse(String code) {
        return courseRepository.findByStudyPlan_IdAndCode(studyPlan.getId(), code)
                .orElseThrow(() -> new IllegalStateException("Course " + code + " not found"));
    }

    private StudentEntity createStudent(String run, String email) {
        StudentEntity student = StudentEntity.builder()
                .run(RunValidatorUtil.cleanFormat(run))
                .firstName("Close")
                .paternalLastName("Student")
                .maternalLastName("Usach")
                .email(email)
                .career(career)
                .studyPlan(studyPlan)
                .academicStatus(AcademicStatus.REGULAR)
                .isMatriculated(true)
                .build();
        return studentRepository.save(student);
    }

    private SectionEntity createSection(CourseEntity course, String sectionCode, AcademicPeriodEntity period) {
        Set<TimeBlockEntity> blocks = new HashSet<>();
        blocks.add(timeBlockRepository.findById("L1").orElseThrow());
        blocks.add(timeBlockRepository.findById("M1").orElseThrow());

        SectionEntity section = SectionEntity.builder()
                .sectionCode(sectionCode)
                .course(course)
                .academicPeriod(period)
                .teacher(teacher)
                .capacity(30)
                .enrolledCount(1)
                .timeBlocks(blocks)
                .build();
        return sectionRepository.save(section);
    }

    private EnrollmentEntity createEnrollment(StudentEntity student, CourseEntity course, SectionEntity section,
                                              AcademicPeriodEntity period, Double finalGrade, EnrollmentStatus status) {
        EnrollmentEntity enrollment = EnrollmentEntity.builder()
                .student(student)
                .course(course)
                .section(section)
                .academicPeriod(period)
                .enrollmentDate(LocalDateTime.now())
                .status(status)
                .finalGrade(finalGrade)
                .build();
        return enrollmentRepository.save(enrollment);
    }

    // =========================================================================
    // ESCENARIOS EXITOSOS (3 CASOS)
    // =========================================================================

    @Test
    @DisplayName("Éxito 1: Cierre formal con 100% de notas congela el período y consolida Aprobadas/Reprobadas")
    void scenario01_successfulPeriodClose_withAllGradesSubmitted_closesPeriodAndConsolidatesEnrollments() {
        StudentEntity s1 = createStudent("12345678-5", "close.std.1@sigaipt.cl");
        StudentEntity s2 = createStudent("12345679-3", "close.std.2@sigaipt.cl");

        CourseEntity c1 = getCourse("TAP101");
        CourseEntity c2 = getCourse("TAP102");

        SectionEntity sec1 = createSection(c1, "TAP101-A-1", openPeriod);
        SectionEntity sec2 = createSection(c2, "TAP102-A-1", openPeriod);

        // Student 1 has passing grade 5.5; Student 2 has failing grade 3.2
        EnrollmentEntity e1 = createEnrollment(s1, c1, sec1, openPeriod, 5.5, EnrollmentStatus.INSCRITA);
        EnrollmentEntity e2 = createEnrollment(s2, c2, sec2, openPeriod, 3.2, EnrollmentStatus.INSCRITA);

        PeriodCloseResultDTO result = periodCloseService.executePeriodClose(openPeriod.getId());

        assertNotNull(result);
        assertEquals(2, result.getTotalProcessedEnrollments());
        assertEquals(1, result.getApprovedCount());
        assertEquals(1, result.getFailedCount());

        // Verify period is now closed in database
        AcademicPeriodEntity updatedPeriod = academicPeriodRepository.findById(openPeriod.getId()).orElseThrow();
        assertFalse(updatedPeriod.getIsOpen());

        // Verify enrollment statuses consolidated in database
        EnrollmentEntity updatedE1 = enrollmentRepository.findById(e1.getId()).orElseThrow();
        assertEquals(EnrollmentStatus.APROBADA, updatedE1.getStatus());

        EnrollmentEntity updatedE2 = enrollmentRepository.findById(e2.getId()).orElseThrow();
        assertEquals(EnrollmentStatus.REPROBADA, updatedE2.getStatus());
    }

    @Test
    @DisplayName("Éxito 2: Cierre formal detecta aprobación total del plan y gradúa automáticamente a EGRESADO")
    void scenario02_successfulPeriodClose_graduatesStudentWhenAllCoursesApproved() {
        StudentEntity student = createStudent("12345680-7", "grad.std@sigaipt.cl");

        AcademicPeriodEntity pastPeriod = academicPeriodRepository.save(
                AcademicPeriodEntity.builder().id("2026-2").year(2026).semester(2).isOpen(false).build()
        );

        List<CourseEntity> planCourses = courseRepository.findByStudyPlan_Id(studyPlan.getId());
        assertTrue(planCourses.size() > 1, "Plan should have multiple courses");

        // Pre-approve all courses in study plan except the last one
        CourseEntity lastCourse = planCourses.get(planCourses.size() - 1);
        for (int i = 0; i < planCourses.size() - 1; i++) {
            CourseEntity pastCourse = planCourses.get(i);
            SectionEntity pastSection = createSection(pastCourse, pastCourse.getCode() + "-PAST", pastPeriod);
            createEnrollment(student, pastCourse, pastSection, pastPeriod, 6.0, EnrollmentStatus.APROBADA);
        }

        // In active period 2027-1, student enrolls and passes the final remaining course
        SectionEntity activeSection = createSection(lastCourse, lastCourse.getCode() + "-FINAL", openPeriod);
        createEnrollment(student, lastCourse, activeSection, openPeriod, 6.5, EnrollmentStatus.INSCRITA);

        PeriodCloseResultDTO result = periodCloseService.executePeriodClose(openPeriod.getId());

        assertEquals(1, result.getGraduatedStudentsCount());
        assertTrue(result.getGraduatedStudentRuns().contains(student.getRun()));

        // Verify student state in database transitioned to EGRESADO
        StudentEntity updatedStudent = studentRepository.findByRun(student.getRun()).orElseThrow();
        assertEquals(AcademicStatus.EGRESADO, updatedStudent.getAcademicStatus());
    }

    @Test
    @DisplayName("Éxito 3: Cierre formal detecta segunda reprobación en misma asignatura y pasa a ELIMINADO")
    void scenario03_successfulPeriodClose_eliminatesStudentOnSecondFailureOfSameCourse() {
        StudentEntity student = createStudent("12345681-5", "dismiss.std@sigaipt.cl");

        AcademicPeriodEntity pastPeriod = academicPeriodRepository.save(
                AcademicPeriodEntity.builder().id("2026-2").year(2026).semester(2).isOpen(false).build()
        );

        CourseEntity tap101 = getCourse("TAP101");

        // First failure: Reprobada in past period 2026-2
        SectionEntity pastSection = createSection(tap101, "TAP101-PAST", pastPeriod);
        createEnrollment(student, tap101, pastSection, pastPeriod, 2.5, EnrollmentStatus.REPROBADA);

        // Second failure: Reprobada in current period 2027-1
        SectionEntity activeSection = createSection(tap101, "TAP101-A-1", openPeriod);
        createEnrollment(student, tap101, activeSection, openPeriod, 3.0, EnrollmentStatus.INSCRITA);

        PeriodCloseResultDTO result = periodCloseService.executePeriodClose(openPeriod.getId());

        assertEquals(1, result.getDismissedStudentsCount());
        assertTrue(result.getDismissedStudentRuns().contains(student.getRun()));

        // Verify student state in database transitioned to ELIMINADO
        StudentEntity updatedStudent = studentRepository.findByRun(student.getRun()).orElseThrow();
        assertEquals(AcademicStatus.ELIMINADO, updatedStudent.getAcademicStatus());
    }

    // =========================================================================
    // ESCENARIOS DE RECHAZO (3 CASOS)
    // =========================================================================

    @Test
    @DisplayName("Rechazo 1: Intento de cierre de período con notas pendientes es bloqueado")
    void scenario04_rejection_closePeriodWithPendingGrades_throwsException() {
        StudentEntity student = createStudent("12345682-3", "pending.std@sigaipt.cl");
        CourseEntity c1 = getCourse("TAP101");
        SectionEntity sec = createSection(c1, "TAP101-A-1", openPeriod);

        // Active enrollment with NULL final grade
        createEnrollment(student, c1, sec, openPeriod, null, EnrollmentStatus.INSCRITA);

        BusinessRuleException exception = assertThrows(BusinessRuleException.class, () ->
                periodCloseService.executePeriodClose(openPeriod.getId()));

        assertTrue(exception.getMessage().contains("without a registered final grade"));

        // Period must remain open
        AcademicPeriodEntity period = academicPeriodRepository.findById(openPeriod.getId()).orElseThrow();
        assertTrue(period.getIsOpen());
    }

    @Test
    @DisplayName("Rechazo 2: Intento de cierre sobre un período ya CERRADO es bloqueado")
    void scenario05_rejection_closeAlreadyClosedPeriod_throwsException() {
        AcademicPeriodEntity closedPeriod = academicPeriodRepository.save(
                AcademicPeriodEntity.builder().id("2026-1").year(2026).semester(1).isOpen(false).build()
        );

        BusinessRuleException exception = assertThrows(BusinessRuleException.class, () ->
                periodCloseService.executePeriodClose(closedPeriod.getId()));

        assertTrue(exception.getMessage().contains("already CLOSED"));
    }

    @Test
    @DisplayName("Rechazo 3: Modificación o ingreso de notas en sección de período cerrado es bloqueada")
    void scenario06_rejection_gradeSubmissionInClosedPeriod_throwsException() {
        AcademicPeriodEntity closedPeriod = academicPeriodRepository.save(
                AcademicPeriodEntity.builder().id("2025-2").year(2025).semester(2).isOpen(false).build()
        );

        StudentEntity student = createStudent("12345683-1", "frozen.std@sigaipt.cl");
        CourseEntity c1 = getCourse("TAP101");
        SectionEntity sec = createSection(c1, "TAP101-FROZEN", closedPeriod);
        EnrollmentEntity enrollment = createEnrollment(student, c1, sec, closedPeriod, 4.0, EnrollmentStatus.APROBADA);

        SectionGradesSubmissionDTO submissionDTO = SectionGradesSubmissionDTO.builder()
                .grades(List.of(StudentGradeEntryDTO.builder()
                        .enrollmentId(enrollment.getId())
                        .studentRun(student.getRun())
                        .finalGrade(6.0)
                        .build()))
                .build();

        BusinessRuleException exception = assertThrows(BusinessRuleException.class, () ->
                gradeService.submitSectionGrades(sec.getId(), submissionDTO, teacher.getRun(), false));

        assertTrue(exception.getMessage().contains("is CLOSED"));
    }
}
