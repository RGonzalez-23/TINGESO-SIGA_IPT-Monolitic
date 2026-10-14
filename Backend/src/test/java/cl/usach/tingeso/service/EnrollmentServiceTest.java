package cl.usach.tingeso.service;

import cl.usach.tingeso.dto.AvailableCourseOfferDTO;
import cl.usach.tingeso.dto.EnrolledStudentDTO;
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
import cl.usach.tingeso.exception.ResourceNotFoundException;
import cl.usach.tingeso.repository.AcademicPeriodRepository;
import cl.usach.tingeso.repository.CourseRepository;
import cl.usach.tingeso.repository.EnrollmentRepository;
import cl.usach.tingeso.repository.SectionRepository;
import cl.usach.tingeso.repository.StudentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for EnrollmentService.
 * Enforces all business rules and ensures >= 90% JaCoCo coverage.
 */
@ExtendWith(MockitoExtension.class)
class EnrollmentServiceTest {

    @Mock
    private EnrollmentRepository enrollmentRepository;

    @Mock
    private StudentRepository studentRepository;

    @Mock
    private SectionRepository sectionRepository;

    @Mock
    private CourseRepository courseRepository;

    @Mock
    private AcademicPeriodRepository academicPeriodRepository;

    @InjectMocks
    private EnrollmentService enrollmentService;

    private AcademicPeriodEntity openPeriod;
    private StudentEntity sampleStudent;
    private CareerEntity sampleCareer;
    private StudyPlanEntity samplePlan;
    private CourseEntity course1;
    private CourseEntity course2;
    private CourseEntity course3;
    private CourseEntity course4;
    private CourseEntity course5;
    private CourseEntity course6;
    private CourseEntity courseAdvanced;
    private SectionEntity section1;
    private SectionEntity section2;
    private SectionEntity section3;
    private TeacherEntity teacher1;
    private TimeBlockEntity blockL1;
    private TimeBlockEntity blockL2;
    private TimeBlockEntity blockM1;

    @BeforeEach
    void setUp() {
        openPeriod = AcademicPeriodEntity.builder()
                .id("2027-1")
                .year(2027)
                .semester(1)
                .isOpen(true)
                .build();

        sampleCareer = CareerEntity.builder()
                .code("10450")
                .name("Técnico Analista Programador")
                .durationSemesters(4)
                .isActive(true)
                .build();

        samplePlan = StudyPlanEntity.builder()
                .id(1L)
                .code("2021.3")
                .isActive(true)
                .career(sampleCareer)
                .build();

        sampleStudent = StudentEntity.builder()
                .run("12345678-5")
                .firstName("Juan")
                .paternalLastName("Pérez")
                .maternalLastName("Gómez")
                .email("juan.perez@sigaipt.cl")
                .academicStatus(AcademicStatus.REGULAR)
                .isMatriculated(true)
                .career(sampleCareer)
                .studyPlan(samplePlan)
                .build();

        teacher1 = TeacherEntity.builder()
                .run("11111111-1")
                .firstName("Pedro")
                .paternalLastName("González")
                .isActive(true)
                .build();

        blockL1 = TimeBlockEntity.builder().code("L1").dayOfWeek("L").moduleNumber(1).build();
        blockL2 = TimeBlockEntity.builder().code("L2").dayOfWeek("L").moduleNumber(2).build();
        blockM1 = TimeBlockEntity.builder().code("M1").dayOfWeek("M").moduleNumber(1).build();

        course1 = CourseEntity.builder().id(101L).code("TAP101").name("Fundamentos Prog").semester(1).studyPlan(samplePlan).prerequisites(Set.of()).build();
        course2 = CourseEntity.builder().id(102L).code("TAP102").name("Matemática Aplicada").semester(1).studyPlan(samplePlan).prerequisites(Set.of()).build();
        course3 = CourseEntity.builder().id(103L).code("TAP103").name("Fund Computación").semester(1).studyPlan(samplePlan).prerequisites(Set.of()).build();
        course4 = CourseEntity.builder().id(104L).code("TAP104").name("Bases de Datos I").semester(1).studyPlan(samplePlan).prerequisites(Set.of()).build();
        course5 = CourseEntity.builder().id(105L).code("TAP105").name("Comunicación").semester(1).studyPlan(samplePlan).prerequisites(Set.of()).build();
        course6 = CourseEntity.builder().id(106L).code("TAP106").name("Inglés I").semester(1).studyPlan(samplePlan).prerequisites(Set.of()).build();

        courseAdvanced = CourseEntity.builder().id(201L).code("TAP201").name("POO").semester(2).studyPlan(samplePlan).prerequisites(Set.of(course1)).build();

        section1 = SectionEntity.builder()
                .id(1L)
                .sectionCode("TAP101-A-1")
                .course(course1)
                .academicPeriod(openPeriod)
                .teacher(teacher1)
                .capacity(30)
                .enrolledCount(0)
                .timeBlocks(new HashSet<>(Set.of(blockL1)))
                .build();

        section2 = SectionEntity.builder()
                .id(2L)
                .sectionCode("TAP102-A-1")
                .course(course2)
                .academicPeriod(openPeriod)
                .teacher(teacher1)
                .capacity(30)
                .enrolledCount(0)
                .timeBlocks(new HashSet<>(Set.of(blockL2)))
                .build();

        section3 = SectionEntity.builder()
                .id(3L)
                .sectionCode("TAP103-A-1")
                .course(course3)
                .academicPeriod(openPeriod)
                .teacher(teacher1)
                .capacity(30)
                .enrolledCount(0)
                .timeBlocks(new HashSet<>(Set.of(blockM1)))
                .build();
    }

    // =========================================================================
    // 1. registerEnrollment Tests: Pre-conditions and Business Validations
    // =========================================================================

    @Test
    @DisplayName("Should throw BusinessRuleException when no academic period is open")
    void testRegister_NoOpenPeriod() {
        when(academicPeriodRepository.findByIsOpenTrue()).thenReturn(Optional.empty());

        EnrollmentRequestDTO req = EnrollmentRequestDTO.builder().sectionIds(List.of(1L, 2L, 3L)).build();

        assertThrows(BusinessRuleException.class, () ->
                enrollmentService.registerEnrollment(req, "12345678-5", false));
    }

    @Test
    @DisplayName("Should throw ResourceNotFoundException when student does not exist")
    void testRegister_StudentNotFound() {
        when(academicPeriodRepository.findByIsOpenTrue()).thenReturn(Optional.of(openPeriod));
        when(studentRepository.findByRun("12345678-5")).thenReturn(Optional.empty());

        EnrollmentRequestDTO req = EnrollmentRequestDTO.builder().sectionIds(List.of(1L, 2L, 3L)).build();

        assertThrows(ResourceNotFoundException.class, () ->
                enrollmentService.registerEnrollment(req, "12345678-5", false));
    }

    @Test
    @DisplayName("Should throw BusinessRuleException when student status is not REGULAR")
    void testRegister_StudentNotRegular() {
        sampleStudent.setAcademicStatus(AcademicStatus.POSTERGACION);
        when(academicPeriodRepository.findByIsOpenTrue()).thenReturn(Optional.of(openPeriod));
        when(studentRepository.findByRun("12345678-5")).thenReturn(Optional.of(sampleStudent));

        EnrollmentRequestDTO req = EnrollmentRequestDTO.builder().sectionIds(List.of(1L, 2L, 3L)).build();

        assertThrows(BusinessRuleException.class, () ->
                enrollmentService.registerEnrollment(req, "12345678-5", false));
    }

    @Test
    @DisplayName("Should throw BusinessRuleException when student is not matriculated")
    void testRegister_StudentNotMatriculated() {
        sampleStudent.setIsMatriculated(false);
        when(academicPeriodRepository.findByIsOpenTrue()).thenReturn(Optional.of(openPeriod));
        when(studentRepository.findByRun("12345678-5")).thenReturn(Optional.of(sampleStudent));

        EnrollmentRequestDTO req = EnrollmentRequestDTO.builder().sectionIds(List.of(1L, 2L, 3L)).build();

        assertThrows(BusinessRuleException.class, () ->
                enrollmentService.registerEnrollment(req, "12345678-5", false));
    }

    @Test
    @DisplayName("Should throw BusinessRuleException when section belongs to different period")
    void testRegister_SectionDifferentPeriod() {
        AcademicPeriodEntity closedPeriod = AcademicPeriodEntity.builder().id("2026-2").isOpen(false).build();
        section1.setAcademicPeriod(closedPeriod);

        when(academicPeriodRepository.findByIsOpenTrue()).thenReturn(Optional.of(openPeriod));
        when(studentRepository.findByRun("12345678-5")).thenReturn(Optional.of(sampleStudent));
        when(enrollmentRepository.findByStudent_Run("12345678-5")).thenReturn(List.of());
        when(sectionRepository.findById(1L)).thenReturn(Optional.of(section1));

        EnrollmentRequestDTO req = EnrollmentRequestDTO.builder().sectionIds(List.of(1L)).build();

        assertThrows(BusinessRuleException.class, () ->
                enrollmentService.registerEnrollment(req, "12345678-5", false));
    }

    @Test
    @DisplayName("Should throw BusinessRuleException when course does not belong to student's study plan")
    void testRegister_CourseNotInStudyPlan() {
        StudyPlanEntity otherPlan = StudyPlanEntity.builder().id(999L).build();
        CourseEntity otherCourse = CourseEntity.builder().id(999L).code("OTH999").name("Other").studyPlan(otherPlan).build();
        section1.setCourse(otherCourse);

        when(academicPeriodRepository.findByIsOpenTrue()).thenReturn(Optional.of(openPeriod));
        when(studentRepository.findByRun("12345678-5")).thenReturn(Optional.of(sampleStudent));
        when(enrollmentRepository.findByStudent_Run("12345678-5")).thenReturn(List.of());
        when(sectionRepository.findById(1L)).thenReturn(Optional.of(section1));

        EnrollmentRequestDTO req = EnrollmentRequestDTO.builder().sectionIds(List.of(1L)).build();

        assertThrows(BusinessRuleException.class, () ->
                enrollmentService.registerEnrollment(req, "12345678-5", false));
    }

    @Test
    @DisplayName("Should throw BusinessRuleException when course is already approved")
    void testRegister_CourseAlreadyApproved() {
        EnrollmentEntity pastApproved = EnrollmentEntity.builder()
                .course(course1)
                .status(EnrollmentStatus.APROBADA)
                .build();

        when(academicPeriodRepository.findByIsOpenTrue()).thenReturn(Optional.of(openPeriod));
        when(studentRepository.findByRun("12345678-5")).thenReturn(Optional.of(sampleStudent));
        when(enrollmentRepository.findByStudent_Run("12345678-5")).thenReturn(List.of(pastApproved));
        when(sectionRepository.findById(1L)).thenReturn(Optional.of(section1));

        EnrollmentRequestDTO req = EnrollmentRequestDTO.builder().sectionIds(List.of(1L)).build();

        assertThrows(BusinessRuleException.class, () ->
                enrollmentService.registerEnrollment(req, "12345678-5", false));
    }

    @Test
    @DisplayName("Should throw BusinessRuleException when prerequisites are not approved")
    void testRegister_PrerequisitesNotMet() {
        SectionEntity secAdv = SectionEntity.builder()
                .id(10L)
                .sectionCode("TAP201-A-1")
                .course(courseAdvanced)
                .academicPeriod(openPeriod)
                .capacity(30)
                .enrolledCount(0)
                .build();

        when(academicPeriodRepository.findByIsOpenTrue()).thenReturn(Optional.of(openPeriod));
        when(studentRepository.findByRun("12345678-5")).thenReturn(Optional.of(sampleStudent));
        when(enrollmentRepository.findByStudent_Run("12345678-5")).thenReturn(List.of()); // No approved TAP101
        when(sectionRepository.findById(10L)).thenReturn(Optional.of(secAdv));

        EnrollmentRequestDTO req = EnrollmentRequestDTO.builder().sectionIds(List.of(10L)).build();

        assertThrows(BusinessRuleException.class, () ->
                enrollmentService.registerEnrollment(req, "12345678-5", false));
    }

    @Test
    @DisplayName("Should throw BusinessRuleException when section has no capacity left")
    void testRegister_NoCapacity() {
        section1.setCapacity(30);
        section1.setEnrolledCount(30);

        when(academicPeriodRepository.findByIsOpenTrue()).thenReturn(Optional.of(openPeriod));
        when(studentRepository.findByRun("12345678-5")).thenReturn(Optional.of(sampleStudent));
        when(enrollmentRepository.findByStudent_Run("12345678-5")).thenReturn(List.of());
        when(sectionRepository.findById(1L)).thenReturn(Optional.of(section1));

        EnrollmentRequestDTO req = EnrollmentRequestDTO.builder().sectionIds(List.of(1L)).build();

        assertThrows(BusinessRuleException.class, () ->
                enrollmentService.registerEnrollment(req, "12345678-5", false));
    }

    @Test
    @DisplayName("Should throw BusinessRuleException when course count is less than 3 and remaining plan courses >= 3")
    void testRegister_FewerThanThreeCourses_PlanHasMany() {
        when(academicPeriodRepository.findByIsOpenTrue()).thenReturn(Optional.of(openPeriod));
        when(studentRepository.findByRun("12345678-5")).thenReturn(Optional.of(sampleStudent));
        when(enrollmentRepository.findByStudent_Run("12345678-5")).thenReturn(List.of());
        when(sectionRepository.findById(1L)).thenReturn(Optional.of(section1));
        when(sectionRepository.findById(2L)).thenReturn(Optional.of(section2));
        when(courseRepository.findByStudyPlan_Id(1L)).thenReturn(List.of(course1, course2, course3, course4, course5, course6));

        EnrollmentRequestDTO req = EnrollmentRequestDTO.builder().sectionIds(List.of(1L, 2L)).build();

        assertThrows(BusinessRuleException.class, () ->
                enrollmentService.registerEnrollment(req, "12345678-5", false));
    }

    @Test
    @DisplayName("Should allow fewer than 3 courses when remaining unapproved courses in plan is less than 3")
    void testRegister_AllowFewerThanThree_WhenRemainingLessThanThree() {
        when(academicPeriodRepository.findByIsOpenTrue()).thenReturn(Optional.of(openPeriod));
        when(studentRepository.findByRun("12345678-5")).thenReturn(Optional.of(sampleStudent));
        when(enrollmentRepository.findByStudent_Run("12345678-5")).thenReturn(List.of());
        when(sectionRepository.findById(1L)).thenReturn(Optional.of(section1));
        when(sectionRepository.findById(2L)).thenReturn(Optional.of(section2));
        when(courseRepository.findByStudyPlan_Id(1L)).thenReturn(List.of(course1, course2)); // Only 2 remaining!
        when(enrollmentRepository.save(any(EnrollmentEntity.class))).thenAnswer(inv -> inv.getArgument(0));

        EnrollmentRequestDTO req = EnrollmentRequestDTO.builder().sectionIds(List.of(1L, 2L)).build();

        List<EnrollmentResponseDTO> res = enrollmentService.registerEnrollment(req, "12345678-5", false);
        assertEquals(2, res.size());
    }

    @Test
    @DisplayName("Should throw BusinessRuleException when lower semester pending course is available but omitted")
    void testRegister_LowerSemesterProgressionViolation() {
        // Student tries to enroll in TAP201 (sem 2), while TAP102 (sem 1) is unapproved, has open section with capacity and met prereqs
        EnrollmentEntity tap101Approved = EnrollmentEntity.builder()
                .course(course1)
                .status(EnrollmentStatus.APROBADA)
                .build();

        SectionEntity secAdv = SectionEntity.builder()
                .id(10L)
                .sectionCode("TAP201-A-1")
                .course(courseAdvanced)
                .academicPeriod(openPeriod)
                .capacity(30)
                .enrolledCount(0)
                .timeBlocks(Set.of(blockL1))
                .build();

        SectionEntity section4 = SectionEntity.builder()
                .id(4L)
                .sectionCode("TAP104-A-1")
                .course(course4)
                .academicPeriod(openPeriod)
                .teacher(teacher1)
                .capacity(30)
                .enrolledCount(0)
                .timeBlocks(new HashSet<>(Set.of(TimeBlockEntity.builder().code("J1").dayOfWeek("J").moduleNumber(1).build())))
                .build();

        when(academicPeriodRepository.findByIsOpenTrue()).thenReturn(Optional.of(openPeriod));
        when(studentRepository.findByRun("12345678-5")).thenReturn(Optional.of(sampleStudent));
        when(enrollmentRepository.findByStudent_Run("12345678-5")).thenReturn(List.of(tap101Approved));
        when(sectionRepository.findById(10L)).thenReturn(Optional.of(secAdv));
        when(sectionRepository.findById(3L)).thenReturn(Optional.of(section3));
        when(sectionRepository.findById(4L)).thenReturn(Optional.of(section4));
        when(courseRepository.findByStudyPlan_Id(1L)).thenReturn(List.of(course1, course2, course3, course4, course5, courseAdvanced));

        // When checking lower semester course2 (TAP102), an open section is available:
        when(sectionRepository.findByCourse_IdAndAcademicPeriod_Id(102L, "2027-1")).thenReturn(Optional.of(section2));

        EnrollmentRequestDTO req = EnrollmentRequestDTO.builder().sectionIds(List.of(10L, 3L, 4L)).build();

        BusinessRuleException ex = assertThrows(BusinessRuleException.class, () ->
                enrollmentService.registerEnrollment(req, "12345678-5", false));
        assertTrue(ex.getMessage().contains("semestre superior"));
    }

    @Test
    @DisplayName("Should throw BusinessRuleException on schedule conflict between selected sections")
    void testRegister_ScheduleConflict() {
        // section1 and section2 both share blockL1
        section2.setTimeBlocks(new HashSet<>(Set.of(blockL1)));

        when(academicPeriodRepository.findByIsOpenTrue()).thenReturn(Optional.of(openPeriod));
        when(studentRepository.findByRun("12345678-5")).thenReturn(Optional.of(sampleStudent));
        when(enrollmentRepository.findByStudent_Run("12345678-5")).thenReturn(List.of());
        when(sectionRepository.findById(1L)).thenReturn(Optional.of(section1));
        when(sectionRepository.findById(2L)).thenReturn(Optional.of(section2));
        when(sectionRepository.findById(3L)).thenReturn(Optional.of(section3));
        when(courseRepository.findByStudyPlan_Id(1L)).thenReturn(List.of(course1, course2, course3, course4, course5, course6));

        EnrollmentRequestDTO req = EnrollmentRequestDTO.builder().sectionIds(List.of(1L, 2L, 3L)).build();

        assertThrows(BusinessRuleException.class, () ->
                enrollmentService.registerEnrollment(req, "12345678-5", false));
    }

    @Test
    @DisplayName("Should successfully enroll in 3 courses with no conflicts")
    void testRegister_Success() {
        when(academicPeriodRepository.findByIsOpenTrue()).thenReturn(Optional.of(openPeriod));
        when(studentRepository.findByRun("12345678-5")).thenReturn(Optional.of(sampleStudent));
        when(enrollmentRepository.findByStudent_Run("12345678-5")).thenReturn(List.of());
        when(sectionRepository.findById(1L)).thenReturn(Optional.of(section1));
        when(sectionRepository.findById(2L)).thenReturn(Optional.of(section2));
        when(sectionRepository.findById(3L)).thenReturn(Optional.of(section3));
        when(courseRepository.findByStudyPlan_Id(1L)).thenReturn(List.of(course1, course2, course3, course4, course5, course6));
        when(enrollmentRepository.save(any(EnrollmentEntity.class))).thenAnswer(inv -> {
            EnrollmentEntity e = inv.getArgument(0);
            e.setId(99L);
            return e;
        });

        EnrollmentRequestDTO req = EnrollmentRequestDTO.builder().sectionIds(List.of(1L, 2L, 3L)).build();

        List<EnrollmentResponseDTO> results = enrollmentService.registerEnrollment(req, "12345678-5", false);

        assertEquals(3, results.size());
        assertEquals(1, section1.getEnrolledCount());
        assertEquals(1, section2.getEnrolledCount());
        assertEquals(1, section3.getEnrolledCount());
        verify(sectionRepository).save(section1);
        verify(sectionRepository).save(section2);
        verify(sectionRepository).save(section3);
    }

    // =========================================================================
    // 2. cancelEnrollment Tests
    // =========================================================================

    @Test
    @DisplayName("Should throw BusinessRuleException when cancelling outside open period")
    void testCancel_NoOpenPeriod() {
        when(academicPeriodRepository.findByIsOpenTrue()).thenReturn(Optional.empty());

        assertThrows(BusinessRuleException.class, () ->
                enrollmentService.cancelEnrollment(1L, "12345678-5", false));
    }

    @Test
    @DisplayName("Should throw ResourceNotFoundException when enrollment ID is invalid")
    void testCancel_NotFound() {
        when(academicPeriodRepository.findByIsOpenTrue()).thenReturn(Optional.of(openPeriod));
        when(enrollmentRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () ->
                enrollmentService.cancelEnrollment(99L, "12345678-5", false));
    }

    @Test
    @DisplayName("Should throw BusinessRuleException when another student attempts to cancel without admin")
    void testCancel_UnauthorizedStudent() {
        EnrollmentEntity enrollment = EnrollmentEntity.builder()
                .id(1L)
                .student(sampleStudent)
                .academicPeriod(openPeriod)
                .status(EnrollmentStatus.INSCRITA)
                .build();

        when(academicPeriodRepository.findByIsOpenTrue()).thenReturn(Optional.of(openPeriod));
        when(enrollmentRepository.findById(1L)).thenReturn(Optional.of(enrollment));

        assertThrows(BusinessRuleException.class, () ->
                enrollmentService.cancelEnrollment(1L, "99999999-9", false));
    }

    @Test
    @DisplayName("Should successfully cancel enrollment and release seat capacity")
    void testCancel_Success() {
        section1.setEnrolledCount(5);
        EnrollmentEntity enrollment = EnrollmentEntity.builder()
                .id(1L)
                .student(sampleStudent)
                .section(section1)
                .academicPeriod(openPeriod)
                .status(EnrollmentStatus.INSCRITA)
                .build();

        when(academicPeriodRepository.findByIsOpenTrue()).thenReturn(Optional.of(openPeriod));
        when(enrollmentRepository.findById(1L)).thenReturn(Optional.of(enrollment));
        when(enrollmentRepository.save(any(EnrollmentEntity.class))).thenAnswer(inv -> inv.getArgument(0));

        EnrollmentResponseDTO res = enrollmentService.cancelEnrollment(1L, "12345678-5", false);

        assertEquals(EnrollmentStatus.ANULADA, res.getStatus());
        assertNotNull(res.getCancellationDate());
        assertEquals(4, section1.getEnrolledCount());
        verify(sectionRepository).save(section1);
    }

    // =========================================================================
    // 3. getAvailableOfferForStudent Tests
    // =========================================================================

    @Test
    @DisplayName("Should generate available offer with eligibility flags correctly")
    void testGetAvailableOffer() {
        when(studentRepository.findByRun("12345678-5")).thenReturn(Optional.of(sampleStudent));
        when(academicPeriodRepository.findByIsOpenTrue()).thenReturn(Optional.of(openPeriod));
        when(courseRepository.findByStudyPlan_Id(1L)).thenReturn(List.of(course1, courseAdvanced));
        when(enrollmentRepository.findByStudent_Run("12345678-5")).thenReturn(List.of());
        when(sectionRepository.findByCourse_IdAndAcademicPeriod_Id(101L, "2027-1")).thenReturn(Optional.of(section1));
        when(sectionRepository.findByCourse_IdAndAcademicPeriod_Id(201L, "2027-1")).thenReturn(Optional.empty());

        List<AvailableCourseOfferDTO> offer = enrollmentService.getAvailableOfferForStudent("12345678-5");

        assertEquals(2, offer.size());
        AvailableCourseOfferDTO o1 = offer.stream().filter(o -> o.getCourseCode().equals("TAP101")).findFirst().orElseThrow();
        assertTrue(o1.getIsSelectable());
        assertTrue(o1.getIsOffered());
        assertEquals(30, o1.getAvailableQuota());

        AvailableCourseOfferDTO o2 = offer.stream().filter(o -> o.getCourseCode().equals("TAP201")).findFirst().orElseThrow();
        assertFalse(o2.getIsSelectable());
        assertFalse(o2.getIsOffered());
    }

    // =========================================================================
    // 4. getEnrolledStudentsForSection Tests
    // =========================================================================

    @Test
    @DisplayName("Should throw BusinessRuleException when teacher requests roster of another section")
    void testGetEnrolledStudents_UnauthorizedTeacher() {
        TeacherEntity otherTeacher = TeacherEntity.builder().run("99999999-9").build();
        section1.setTeacher(otherTeacher);

        when(sectionRepository.findById(1L)).thenReturn(Optional.of(section1));

        assertThrows(BusinessRuleException.class, () ->
                enrollmentService.getEnrolledStudentsForSection(1L, "11111111-1", false));
    }

    @Test
    @DisplayName("Should return enrolled students for assigned teacher or admin")
    void testGetEnrolledStudents_Success() {
        EnrollmentEntity e1 = EnrollmentEntity.builder()
                .student(sampleStudent)
                .section(section1)
                .enrollmentDate(LocalDateTime.now())
                .status(EnrollmentStatus.INSCRITA)
                .build();

        when(sectionRepository.findById(1L)).thenReturn(Optional.of(section1));
        when(enrollmentRepository.findBySection_IdAndStatus(1L, EnrollmentStatus.INSCRITA)).thenReturn(List.of(e1));

        List<EnrolledStudentDTO> roster = enrollmentService.getEnrolledStudentsForSection(1L, "11111111-1", false);

        assertEquals(1, roster.size());
        assertEquals("12345678-5", roster.get(0).getRun());
        assertEquals("Juan Pérez Gómez", roster.get(0).getFullName());
    }

    @Test
    @DisplayName("Should seed past approved course successfully")
    void testSeedPastApprovedCourse() {
        when(studentRepository.findByRun("12345678-5")).thenReturn(Optional.of(sampleStudent));
        when(courseRepository.findByCode("TAP101")).thenReturn(Optional.of(course1));
        when(academicPeriodRepository.findById("2026-2")).thenReturn(Optional.empty());
        when(academicPeriodRepository.save(any(AcademicPeriodEntity.class))).thenAnswer(inv -> inv.getArgument(0));
        when(enrollmentRepository.save(any(EnrollmentEntity.class))).thenAnswer(inv -> {
            EnrollmentEntity e = inv.getArgument(0);
            e.setId(555L);
            return e;
        });

        EnrollmentResponseDTO dto = enrollmentService.seedPastApprovedCourse("12345678-5", "TAP101", "2026-2", 6.2);

        assertNotNull(dto);
        assertEquals(EnrollmentStatus.APROBADA, dto.getStatus());
        assertEquals(6.2, dto.getFinalGrade());
    }
}
