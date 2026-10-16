package cl.usach.tingeso.service;

import cl.usach.tingeso.dto.PeriodCloseResultDTO;
import cl.usach.tingeso.dto.PeriodCloseValidationDTO;
import cl.usach.tingeso.entity.AcademicPeriodEntity;
import cl.usach.tingeso.entity.AcademicStatus;
import cl.usach.tingeso.entity.CourseEntity;
import cl.usach.tingeso.entity.EnrollmentEntity;
import cl.usach.tingeso.entity.EnrollmentStatus;
import cl.usach.tingeso.entity.SectionEntity;
import cl.usach.tingeso.entity.StudentEntity;
import cl.usach.tingeso.entity.StudyPlanEntity;
import cl.usach.tingeso.exception.BusinessRuleException;
import cl.usach.tingeso.repository.AcademicPeriodRepository;
import cl.usach.tingeso.repository.CourseRepository;
import cl.usach.tingeso.repository.EnrollmentRepository;
import cl.usach.tingeso.repository.StudentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PeriodCloseServiceTest {

    @Mock
    private AcademicPeriodRepository academicPeriodRepository;

    @Mock
    private EnrollmentRepository enrollmentRepository;

    @Mock
    private StudentRepository studentRepository;

    @Mock
    private CourseRepository courseRepository;

    @InjectMocks
    private PeriodCloseService periodCloseService;

    private AcademicPeriodEntity period;
    private StudentEntity student;
    private CourseEntity course1;
    private CourseEntity course2;
    private SectionEntity section1;
    private StudyPlanEntity studyPlan;

    @BeforeEach
    void setUp() {
        period = AcademicPeriodEntity.builder()
                .id("2027-1")
                .year(2027)
                .semester(1)
                .isOpen(true)
                .build();

        studyPlan = StudyPlanEntity.builder()
                .id(1L)
                .code("2021.3")
                .isActive(true)
                .build();

        student = StudentEntity.builder()
                .run("20123456-5")
                .firstName("Sofia")
                .paternalLastName("Morales")
                .academicStatus(AcademicStatus.REGULAR)
                .studyPlan(studyPlan)
                .build();

        course1 = CourseEntity.builder()
                .id(101L)
                .code("TAP101")
                .name("Fundamentos de Programacion")
                .studyPlan(studyPlan)
                .build();

        course2 = CourseEntity.builder()
                .id(102L)
                .code("TAP102")
                .name("Matematica Aplicada")
                .studyPlan(studyPlan)
                .build();

        section1 = SectionEntity.builder()
                .id(10L)
                .sectionCode("TAP101-A-1")
                .course(course1)
                .academicPeriod(period)
                .enrolledCount(1)
                .build();
    }

    @Test
    @DisplayName("checkCloseReadiness - Returns canClose=true when all active enrollments have grades")
    void checkCloseReadiness_CanClose() {
        EnrollmentEntity e1 = EnrollmentEntity.builder()
                .id(1L)
                .section(section1)
                .academicPeriod(period)
                .finalGrade(5.5)
                .status(EnrollmentStatus.INSCRITA)
                .build();

        when(academicPeriodRepository.findById("2027-1")).thenReturn(Optional.of(period));
        when(enrollmentRepository.findByAcademicPeriod_IdAndStatus("2027-1", EnrollmentStatus.INSCRITA)).thenReturn(List.of(e1));

        PeriodCloseValidationDTO validation = periodCloseService.checkCloseReadiness("2027-1");

        assertNotNull(validation);
        assertTrue(validation.getCanClose());
        assertEquals(0, validation.getPendingEnrollmentsCount());
        assertTrue(validation.getIncompleteSections().isEmpty());
    }

    @Test
    @DisplayName("checkCloseReadiness - Returns canClose=false and incomplete sections when grades missing")
    void checkCloseReadiness_CannotClose_MissingGrades() {
        EnrollmentEntity e1 = EnrollmentEntity.builder()
                .id(1L)
                .section(section1)
                .academicPeriod(period)
                .finalGrade(null)
                .status(EnrollmentStatus.INSCRITA)
                .build();

        when(academicPeriodRepository.findById("2027-1")).thenReturn(Optional.of(period));
        when(enrollmentRepository.findByAcademicPeriod_IdAndStatus("2027-1", EnrollmentStatus.INSCRITA)).thenReturn(List.of(e1));

        PeriodCloseValidationDTO validation = periodCloseService.checkCloseReadiness("2027-1");

        assertNotNull(validation);
        assertFalse(validation.getCanClose());
        assertEquals(1, validation.getPendingEnrollmentsCount());
        assertEquals(1, validation.getIncompleteSections().size());
        assertEquals("TAP101-A-1", validation.getIncompleteSections().getFirst().getSectionCode());
    }

    @Test
    @DisplayName("executePeriodClose - Successfully closes period, sets APROBADA and REPROBADA")
    void executePeriodClose_Success() {
        EnrollmentEntity ePass = EnrollmentEntity.builder()
                .id(1L)
                .student(student)
                .course(course1)
                .section(section1)
                .academicPeriod(period)
                .finalGrade(5.0)
                .status(EnrollmentStatus.INSCRITA)
                .build();

        EnrollmentEntity eFail = EnrollmentEntity.builder()
                .id(2L)
                .student(student)
                .course(course2)
                .section(section1)
                .academicPeriod(period)
                .finalGrade(3.5)
                .status(EnrollmentStatus.INSCRITA)
                .build();

        when(academicPeriodRepository.findById("2027-1")).thenReturn(Optional.of(period));
        when(enrollmentRepository.findByAcademicPeriod_IdAndStatus("2027-1", EnrollmentStatus.INSCRITA))
                .thenReturn(List.of(ePass, eFail));
        when(courseRepository.findByStudyPlan_Id(1L)).thenReturn(List.of(course1, course2));
        when(enrollmentRepository.findByStudent_RunAndStatus(student.getRun(), EnrollmentStatus.REPROBADA))
                .thenReturn(List.of(eFail));
        when(enrollmentRepository.findByStudent_RunAndStatus(student.getRun(), EnrollmentStatus.APROBADA))
                .thenReturn(List.of(ePass));

        PeriodCloseResultDTO result = periodCloseService.executePeriodClose("2027-1");

        assertNotNull(result);
        assertEquals(2, result.getTotalProcessedEnrollments());
        assertEquals(1, result.getApprovedCount());
        assertEquals(1, result.getFailedCount());
        assertFalse(period.getIsOpen());
        verify(academicPeriodRepository).save(period);
    }

    @Test
    @DisplayName("executePeriodClose - Student becomes EGRESADO when passing all study plan subjects")
    void executePeriodClose_StudentGraduates() {
        EnrollmentEntity ePass = EnrollmentEntity.builder()
                .id(1L)
                .student(student)
                .course(course1)
                .section(section1)
                .academicPeriod(period)
                .finalGrade(6.0)
                .status(EnrollmentStatus.INSCRITA)
                .build();

        when(academicPeriodRepository.findById("2027-1")).thenReturn(Optional.of(period));
        when(enrollmentRepository.findByAcademicPeriod_IdAndStatus("2027-1", EnrollmentStatus.INSCRITA))
                .thenReturn(List.of(ePass));
        when(courseRepository.findByStudyPlan_Id(1L)).thenReturn(List.of(course1));
        when(enrollmentRepository.findByStudent_RunAndStatus(student.getRun(), EnrollmentStatus.REPROBADA))
                .thenReturn(List.of());
        when(enrollmentRepository.findByStudent_RunAndStatus(student.getRun(), EnrollmentStatus.APROBADA))
                .thenReturn(List.of(ePass));

        PeriodCloseResultDTO result = periodCloseService.executePeriodClose("2027-1");

        assertEquals(1, result.getGraduatedStudentsCount());
        assertEquals(student.getRun(), result.getGraduatedStudentRuns().getFirst());
        assertEquals(AcademicStatus.EGRESADO, student.getAcademicStatus());
        verify(studentRepository).save(student);
    }

    @Test
    @DisplayName("executePeriodClose - Student becomes ELIMINADO on failing the same subject twice")
    void executePeriodClose_StudentDismissed() {
        EnrollmentEntity eFail1 = EnrollmentEntity.builder()
                .id(1L)
                .student(student)
                .course(course1)
                .section(section1)
                .academicPeriod(period)
                .finalGrade(3.0)
                .status(EnrollmentStatus.REPROBADA)
                .build();

        EnrollmentEntity eFail2 = EnrollmentEntity.builder()
                .id(2L)
                .student(student)
                .course(course1)
                .section(section1)
                .academicPeriod(period)
                .finalGrade(3.2)
                .status(EnrollmentStatus.INSCRITA)
                .build();

        when(academicPeriodRepository.findById("2027-1")).thenReturn(Optional.of(period));
        when(enrollmentRepository.findByAcademicPeriod_IdAndStatus("2027-1", EnrollmentStatus.INSCRITA))
                .thenReturn(List.of(eFail2));
        when(enrollmentRepository.findByStudent_RunAndStatus(student.getRun(), EnrollmentStatus.REPROBADA))
                .thenReturn(List.of(eFail1, eFail2));

        PeriodCloseResultDTO result = periodCloseService.executePeriodClose("2027-1");

        assertEquals(1, result.getDismissedStudentsCount());
        assertEquals(student.getRun(), result.getDismissedStudentRuns().getFirst());
        assertEquals(AcademicStatus.ELIMINADO, student.getAcademicStatus());
        verify(studentRepository).save(student);
    }

    @Test
    @DisplayName("executePeriodClose - Fails when period is already CLOSED")
    void executePeriodClose_Fails_AlreadyClosed() {
        period.setIsOpen(false);
        when(academicPeriodRepository.findById("2027-1")).thenReturn(Optional.of(period));

        assertThrows(BusinessRuleException.class, () ->
                periodCloseService.executePeriodClose("2027-1")
        );
    }

    @Test
    @DisplayName("executePeriodClose - Fails when there are pending grades")
    void executePeriodClose_Fails_PendingGrades() {
        EnrollmentEntity ePending = EnrollmentEntity.builder()
                .id(1L)
                .student(student)
                .course(course1)
                .section(section1)
                .academicPeriod(period)
                .finalGrade(null)
                .status(EnrollmentStatus.INSCRITA)
                .build();

        when(academicPeriodRepository.findById("2027-1")).thenReturn(Optional.of(period));
        when(enrollmentRepository.findByAcademicPeriod_IdAndStatus("2027-1", EnrollmentStatus.INSCRITA))
                .thenReturn(List.of(ePending));

        assertThrows(BusinessRuleException.class, () ->
                periodCloseService.executePeriodClose("2027-1")
        );
    }
}
