package cl.usach.tingeso.integration;

import cl.usach.tingeso.dto.EnrollmentRequestDTO;
import cl.usach.tingeso.dto.EnrollmentResponseDTO;
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
import cl.usach.tingeso.service.EnrollmentService;
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
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Integration test suite for Process 1: Academic Enrollment (Epic 6).
 * Validates full multi-layer interaction (Controller/Service, JPA entities, and database constraints).
 * Includes 3 success scenarios and 3 rejection scenarios as mandated by the evaluation rubric.
 */
@SpringBootTest
@ActiveProfiles("test")
@TestPropertySource(locations = "classpath:application-test.properties")
@Transactional
public class EnrollmentProcessIntegrationTest {

    @Autowired
    private EnrollmentService enrollmentService;

    @Autowired
    private EnrollmentRepository enrollmentRepository;

    @Autowired
    private StudentRepository studentRepository;

    @Autowired
    private TeacherRepository teacherRepository;

    @Autowired
    private CourseRepository courseRepository;

    @Autowired
    private SectionRepository sectionRepository;

    @Autowired
    private AcademicPeriodRepository academicPeriodRepository;

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

    private StudentEntity createStudent(String run, String email, boolean isMatriculated) {
        StudentEntity student = StudentEntity.builder()
                .run(RunValidatorUtil.cleanFormat(run))
                .firstName("Test")
                .paternalLastName("Student")
                .maternalLastName("Usach")
                .email(email)
                .career(career)
                .studyPlan(studyPlan)
                .academicStatus(AcademicStatus.REGULAR)
                .isMatriculated(isMatriculated)
                .build();
        return studentRepository.save(student);
    }

    private SectionEntity createSection(CourseEntity course, String sectionCode, int capacity, List<String> blockCodes, AcademicPeriodEntity period) {
        Set<TimeBlockEntity> blocks = new HashSet<>();
        for (String code : blockCodes) {
            blocks.add(timeBlockRepository.findById(code).orElseThrow());
        }

        SectionEntity section = SectionEntity.builder()
                .sectionCode(sectionCode)
                .course(course)
                .academicPeriod(period)
                .teacher(teacher)
                .capacity(capacity)
                .enrolledCount(0)
                .timeBlocks(blocks)
                .build();
        return sectionRepository.save(section);
    }

    // =========================================================================
    // ESCENARIOS EXITOSOS (3 CASOS)
    // =========================================================================

    @Test
    @DisplayName("Éxito 1: Inscripción estándar de 3 asignaturas válidas incrementa cupos ocupados")
    void scenario01_successfulEnrollment_standardCourses_incrementsEnrolledCount() {
        StudentEntity student = createStudent("12345678-5", "student.enroll.1@sigaipt.cl", true);

        CourseEntity c1 = getCourse("TAP101");
        CourseEntity c2 = getCourse("TAP102");
        CourseEntity c3 = getCourse("TAP103");

        SectionEntity s1 = createSection(c1, "TAP101-A-1", 30, List.of("L1", "M1", "W1"), openPeriod);
        SectionEntity s2 = createSection(c2, "TAP102-A-1", 30, List.of("L2", "M2"), openPeriod);
        SectionEntity s3 = createSection(c3, "TAP103-A-1", 30, List.of("L3", "M3"), openPeriod);

        EnrollmentRequestDTO request = EnrollmentRequestDTO.builder()
                .studentRun(student.getRun())
                .sectionIds(List.of(s1.getId(), s2.getId(), s3.getId()))
                .build();

        List<EnrollmentResponseDTO> responses = enrollmentService.registerEnrollment(request, student.getRun(), false);

        assertNotNull(responses);
        assertEquals(3, responses.size());
        assertEquals(EnrollmentStatus.INSCRITA, responses.get(0).getStatus());

        // Verify section capacity in database updated
        SectionEntity updatedS1 = sectionRepository.findById(s1.getId()).orElseThrow();
        assertEquals(1, updatedS1.getEnrolledCount());
    }

    @Test
    @DisplayName("Éxito 2: Inscripción de asignatura superior con prerrequisito previamente aprobado")
    void scenario02_successfulEnrollment_withApprovedPrerequisites_succeeds() {
        StudentEntity student = createStudent("12345679-3", "student.enroll.2@sigaipt.cl", true);

        // Pre-approve TAP101..TAP106 in academic history with past period
        AcademicPeriodEntity pastPeriod = academicPeriodRepository.save(
                AcademicPeriodEntity.builder().id("2026-2").year(2026).semester(2).isOpen(false).build()
        );

        List<String> sem1Codes = List.of("TAP101", "TAP102", "TAP103", "TAP104", "TAP105", "TAP106");
        for (String code : sem1Codes) {
            CourseEntity c = getCourse(code);
            SectionEntity pastSection = createSection(c, code + "-PAST", 30, List.of("L1", "M1"), pastPeriod);

            enrollmentRepository.save(EnrollmentEntity.builder()
                    .student(student)
                    .course(c)
                    .section(pastSection)
                    .academicPeriod(pastPeriod)
                    .enrollmentDate(LocalDateTime.now().minusMonths(6))
                    .status(EnrollmentStatus.APROBADA)
                    .finalGrade(5.5)
                    .build());
        }

        // TAP201 requires TAP101 (already approved)
        CourseEntity tap201 = getCourse("TAP201");
        CourseEntity tap202 = getCourse("TAP202");
        CourseEntity tap203 = getCourse("TAP203");

        SectionEntity s1 = createSection(tap201, "TAP201-A-1", 30, List.of("L1", "M1", "W1"), openPeriod);
        SectionEntity s2 = createSection(tap202, "TAP202-A-1", 30, List.of("L2", "M2", "W2"), openPeriod);
        SectionEntity s3 = createSection(tap203, "TAP203-A-1", 30, List.of("L3", "M3"), openPeriod);

        EnrollmentRequestDTO request = EnrollmentRequestDTO.builder()
                .studentRun(student.getRun())
                .sectionIds(List.of(s1.getId(), s2.getId(), s3.getId()))
                .build();

        List<EnrollmentResponseDTO> responses = enrollmentService.registerEnrollment(request, student.getRun(), false);

        assertNotNull(responses);
        assertEquals(3, responses.size());
    }

    @Test
    @DisplayName("Éxito 3: Anulación de inscripción libera el cupo en la sección y pasa a estado ANULADA")
    void scenario03_successfulCancellation_freesCapacityAndMarksAnulada() {
        StudentEntity student = createStudent("12345680-7", "student.enroll.3@sigaipt.cl", true);

        CourseEntity c1 = getCourse("TAP101");
        CourseEntity c2 = getCourse("TAP102");
        CourseEntity c3 = getCourse("TAP103");

        SectionEntity s1 = createSection(c1, "TAP101-A-1", 30, List.of("L1", "M1", "W1"), openPeriod);
        SectionEntity s2 = createSection(c2, "TAP102-A-1", 30, List.of("L2", "M2"), openPeriod);
        SectionEntity s3 = createSection(c3, "TAP103-A-1", 30, List.of("L3", "M3"), openPeriod);

        EnrollmentRequestDTO request = EnrollmentRequestDTO.builder()
                .studentRun(student.getRun())
                .sectionIds(List.of(s1.getId(), s2.getId(), s3.getId()))
                .build();

        List<EnrollmentResponseDTO> enrollments = enrollmentService.registerEnrollment(request, student.getRun(), false);
        Long enrollmentToCancelId = enrollments.get(0).getId();

        assertEquals(1, sectionRepository.findById(s1.getId()).orElseThrow().getEnrolledCount());

        // Cancel enrollment
        EnrollmentResponseDTO cancelled = enrollmentService.cancelEnrollment(enrollmentToCancelId, student.getRun(), false);

        assertEquals(EnrollmentStatus.ANULADA, cancelled.getStatus());
        assertEquals(0, sectionRepository.findById(s1.getId()).orElseThrow().getEnrolledCount());
    }

    // =========================================================================
    // ESCENARIOS DE RECHAZO (3 CASOS)
    // =========================================================================

    @Test
    @DisplayName("Rechazo 1: Conflicto de horario entre secciones seleccionadas es bloqueado")
    void scenario04_rejection_scheduleConflictBetweenSections_throwsException() {
        StudentEntity student = createStudent("12345681-5", "student.enroll.4@sigaipt.cl", true);

        CourseEntity c1 = getCourse("TAP101");
        CourseEntity c2 = getCourse("TAP102");
        CourseEntity c3 = getCourse("TAP103");

        // Notice s1 and s2 both share time block "L1"
        SectionEntity s1 = createSection(c1, "TAP101-A-1", 30, List.of("L1", "M1", "W1"), openPeriod);
        SectionEntity s2 = createSection(c2, "TAP102-A-1", 30, List.of("L1", "M2"), openPeriod);
        SectionEntity s3 = createSection(c3, "TAP103-A-1", 30, List.of("L3", "M3"), openPeriod);

        EnrollmentRequestDTO request = EnrollmentRequestDTO.builder()
                .studentRun(student.getRun())
                .sectionIds(List.of(s1.getId(), s2.getId(), s3.getId()))
                .build();

        BusinessRuleException exception = assertThrows(BusinessRuleException.class, () ->
                enrollmentService.registerEnrollment(request, student.getRun(), false));

        assertTrue(exception.getMessage().contains("Conflicto de horario"));
        assertEquals(0, enrollmentRepository.findByStudent_Run(student.getRun()).size());
    }

    @Test
    @DisplayName("Rechazo 2: Intento de inscripción sin cumplir prerrequisitos es bloqueado")
    void scenario05_rejection_missingPrerequisite_throwsException() {
        StudentEntity student = createStudent("12345682-3", "student.enroll.5@sigaipt.cl", true);

        // TAP201 requires TAP101. Student has not approved TAP101.
        CourseEntity tap201 = getCourse("TAP201");
        CourseEntity tap102 = getCourse("TAP102");
        CourseEntity tap103 = getCourse("TAP103");

        SectionEntity s1 = createSection(tap201, "TAP201-A-1", 30, List.of("L1", "M1", "W1"), openPeriod);
        SectionEntity s2 = createSection(tap102, "TAP102-A-1", 30, List.of("L2", "M2"), openPeriod);
        SectionEntity s3 = createSection(tap103, "TAP103-A-1", 30, List.of("L3", "M3"), openPeriod);

        EnrollmentRequestDTO request = EnrollmentRequestDTO.builder()
                .studentRun(student.getRun())
                .sectionIds(List.of(s1.getId(), s2.getId(), s3.getId()))
                .build();

        BusinessRuleException exception = assertThrows(BusinessRuleException.class, () ->
                enrollmentService.registerEnrollment(request, student.getRun(), false));

        assertTrue(exception.getMessage().contains("prerrequisito") || exception.getMessage().contains("semestre"));
    }

    @Test
    @DisplayName("Rechazo 3: Intento de inscripción en sección sin cupos disponibles es bloqueado")
    void scenario06_rejection_sectionCapacityFull_throwsException() {
        StudentEntity student1 = createStudent("12345683-1", "student.enroll.6a@sigaipt.cl", true);
        StudentEntity student2 = createStudent("12345684-K", "student.enroll.6b@sigaipt.cl", true);

        CourseEntity c1 = getCourse("TAP101");
        CourseEntity c2 = getCourse("TAP102");
        CourseEntity c3 = getCourse("TAP103");

        // s1 has capacity = 1
        SectionEntity s1 = createSection(c1, "TAP101-A-1", 1, List.of("L1", "M1", "W1"), openPeriod);
        SectionEntity s2 = createSection(c2, "TAP102-A-1", 30, List.of("L2", "M2"), openPeriod);
        SectionEntity s3 = createSection(c3, "TAP103-A-1", 30, List.of("L3", "M3"), openPeriod);

        // Student 1 enrolls first, occupying the only seat in s1
        EnrollmentRequestDTO request1 = EnrollmentRequestDTO.builder()
                .studentRun(student1.getRun())
                .sectionIds(List.of(s1.getId(), s2.getId(), s3.getId()))
                .build();
        enrollmentService.registerEnrollment(request1, student1.getRun(), false);

        assertEquals(1, sectionRepository.findById(s1.getId()).orElseThrow().getEnrolledCount());

        // Student 2 attempts to enroll in the full section s1
        EnrollmentRequestDTO request2 = EnrollmentRequestDTO.builder()
                .studentRun(student2.getRun())
                .sectionIds(List.of(s1.getId(), s2.getId(), s3.getId()))
                .build();

        BusinessRuleException exception = assertThrows(BusinessRuleException.class, () ->
                enrollmentService.registerEnrollment(request2, student2.getRun(), false));

        assertTrue(exception.getMessage().contains("cupos"));
    }
}
