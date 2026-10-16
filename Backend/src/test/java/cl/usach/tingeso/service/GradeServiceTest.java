package cl.usach.tingeso.service;

import cl.usach.tingeso.dto.SectionGradesSubmissionDTO;
import cl.usach.tingeso.dto.StudentGradeEntryDTO;
import cl.usach.tingeso.entity.AcademicPeriodEntity;
import cl.usach.tingeso.entity.CourseEntity;
import cl.usach.tingeso.entity.EnrollmentEntity;
import cl.usach.tingeso.entity.EnrollmentStatus;
import cl.usach.tingeso.entity.SectionEntity;
import cl.usach.tingeso.entity.StudentEntity;
import cl.usach.tingeso.entity.TeacherEntity;
import cl.usach.tingeso.exception.BusinessRuleException;
import cl.usach.tingeso.exception.ResourceNotFoundException;
import cl.usach.tingeso.repository.EnrollmentRepository;
import cl.usach.tingeso.repository.SectionRepository;
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
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GradeServiceTest {

    @Mock
    private SectionRepository sectionRepository;

    @Mock
    private EnrollmentRepository enrollmentRepository;

    @InjectMocks
    private GradeService gradeService;

    private SectionEntity section;
    private TeacherEntity teacher;
    private AcademicPeriodEntity period;
    private StudentEntity student;
    private EnrollmentEntity enrollment;

    @BeforeEach
    void setUp() {
        teacher = TeacherEntity.builder()
                .run("16432890-2")
                .firstName("Maria")
                .paternalLastName("Gonzalez")
                .build();

        period = AcademicPeriodEntity.builder()
                .id("2027-1")
                .year(2027)
                .semester(1)
                .isOpen(true)
                .build();

        CourseEntity course = CourseEntity.builder()
                .id(1L)
                .code("TAP101")
                .name("Fundamentos de Programacion")
                .build();

        section = SectionEntity.builder()
                .id(10L)
                .sectionCode("TAP101-A-1")
                .teacher(teacher)
                .academicPeriod(period)
                .course(course)
                .capacity(25)
                .enrolledCount(1)
                .build();

        student = StudentEntity.builder()
                .run("20123456-5")
                .firstName("Sofia")
                .paternalLastName("Morales")
                .build();

        enrollment = EnrollmentEntity.builder()
                .id(100L)
                .student(student)
                .section(section)
                .academicPeriod(period)
                .course(course)
                .status(EnrollmentStatus.INSCRITA)
                .build();
    }

    @Test
    @DisplayName("getSectionGrades - Success by assigned teacher")
    void getSectionGrades_Success_AssignedTeacher() {
        when(sectionRepository.findById(10L)).thenReturn(Optional.of(section));
        when(enrollmentRepository.findBySection_IdAndStatus(10L, EnrollmentStatus.INSCRITA)).thenReturn(List.of(enrollment));

        List<StudentGradeEntryDTO> grades = gradeService.getSectionGrades(10L, "16432890-2", false);

        assertNotNull(grades);
        assertEquals(1, grades.size());
        assertEquals("20123456-5", grades.getFirst().getStudentRun());
    }

    @Test
    @DisplayName("getSectionGrades - Success by ADMIN")
    void getSectionGrades_Success_Admin() {
        when(sectionRepository.findById(10L)).thenReturn(Optional.of(section));
        when(enrollmentRepository.findBySection_IdAndStatus(10L, EnrollmentStatus.INSCRITA)).thenReturn(List.of(enrollment));

        List<StudentGradeEntryDTO> grades = gradeService.getSectionGrades(10L, "admin", true);

        assertNotNull(grades);
        assertEquals(1, grades.size());
    }

    @Test
    @DisplayName("getSectionGrades - Fails if not assigned teacher and not admin")
    void getSectionGrades_Fails_UnassignedTeacher() {
        when(sectionRepository.findById(10L)).thenReturn(Optional.of(section));

        assertThrows(BusinessRuleException.class, () ->
                gradeService.getSectionGrades(10L, "99999999-9", false)
        );
    }

    @Test
    @DisplayName("getSectionGrades - Fails if section not found")
    void getSectionGrades_Fails_NotFound() {
        when(sectionRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () ->
                gradeService.getSectionGrades(999L, "16432890-2", false)
        );
    }

    @Test
    @DisplayName("submitSectionGrades - Success registering grade in open period")
    void submitSectionGrades_Success() {
        when(sectionRepository.findById(10L)).thenReturn(Optional.of(section));
        when(enrollmentRepository.findById(100L)).thenReturn(Optional.of(enrollment));
        when(enrollmentRepository.save(any(EnrollmentEntity.class))).thenAnswer(i -> i.getArgument(0));

        SectionGradesSubmissionDTO submission = SectionGradesSubmissionDTO.builder()
                .grades(List.of(
                        StudentGradeEntryDTO.builder()
                                .enrollmentId(100L)
                                .finalGrade(5.8)
                                .build()
                ))
                .build();

        List<StudentGradeEntryDTO> result = gradeService.submitSectionGrades(10L, submission, "16432890-2", false);

        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(5.8, result.getFirst().getFinalGrade());
        verify(enrollmentRepository).save(any(EnrollmentEntity.class));
    }

    @Test
    @DisplayName("submitSectionGrades - Fails if academic period is CLOSED")
    void submitSectionGrades_Fails_ClosedPeriod() {
        period.setIsOpen(false);
        when(sectionRepository.findById(10L)).thenReturn(Optional.of(section));

        SectionGradesSubmissionDTO submission = SectionGradesSubmissionDTO.builder()
                .grades(List.of(StudentGradeEntryDTO.builder().enrollmentId(100L).finalGrade(5.0).build()))
                .build();

        assertThrows(BusinessRuleException.class, () ->
                gradeService.submitSectionGrades(10L, submission, "16432890-2", false)
        );
    }

    @Test
    @DisplayName("submitSectionGrades - Fails if grade out of bounds (< 1.0 or > 7.0)")
    void submitSectionGrades_Fails_OutOfBounds() {
        when(sectionRepository.findById(10L)).thenReturn(Optional.of(section));
        when(enrollmentRepository.findById(100L)).thenReturn(Optional.of(enrollment));

        SectionGradesSubmissionDTO subLow = SectionGradesSubmissionDTO.builder()
                .grades(List.of(StudentGradeEntryDTO.builder().enrollmentId(100L).finalGrade(0.9).build()))
                .build();

        assertThrows(BusinessRuleException.class, () ->
                gradeService.submitSectionGrades(10L, subLow, "16432890-2", false)
        );

        SectionGradesSubmissionDTO subHigh = SectionGradesSubmissionDTO.builder()
                .grades(List.of(StudentGradeEntryDTO.builder().enrollmentId(100L).finalGrade(7.5).build()))
                .build();

        assertThrows(BusinessRuleException.class, () ->
                gradeService.submitSectionGrades(10L, subHigh, "16432890-2", false)
        );
    }

    @Test
    @DisplayName("submitSectionGrades - Fails if enrollment is cancelled (ANULADA)")
    void submitSectionGrades_Fails_CancelledEnrollment() {
        enrollment.setStatus(EnrollmentStatus.ANULADA);
        when(sectionRepository.findById(10L)).thenReturn(Optional.of(section));
        when(enrollmentRepository.findById(100L)).thenReturn(Optional.of(enrollment));

        SectionGradesSubmissionDTO submission = SectionGradesSubmissionDTO.builder()
                .grades(List.of(StudentGradeEntryDTO.builder().enrollmentId(100L).finalGrade(5.0).build()))
                .build();

        assertThrows(BusinessRuleException.class, () ->
                gradeService.submitSectionGrades(10L, submission, "16432890-2", false)
        );
    }
}
