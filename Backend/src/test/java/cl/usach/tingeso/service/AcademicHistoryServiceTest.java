package cl.usach.tingeso.service;

import cl.usach.tingeso.dto.StudentAcademicHistoryDTO;
import cl.usach.tingeso.dto.StudentCurriculumProgressDTO;
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
import cl.usach.tingeso.exception.ResourceNotFoundException;
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

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AcademicHistoryServiceTest {

    @Mock
    private StudentRepository studentRepository;

    @Mock
    private EnrollmentRepository enrollmentRepository;

    @Mock
    private CourseRepository courseRepository;

    @InjectMocks
    private AcademicHistoryService academicHistoryService;

    private StudentEntity student;
    private StudyPlanEntity studyPlan;
    private CareerEntity career;
    private CourseEntity course1;
    private CourseEntity course2;
    private CourseEntity course3;
    private CourseEntity course4;
    private AcademicPeriodEntity period1;
    private AcademicPeriodEntity period2;

    @BeforeEach
    void setUp() {
        career = CareerEntity.builder()
                .code("10450")
                .name("Tecnico Analista Programador")
                .build();

        studyPlan = StudyPlanEntity.builder()
                .id(1L)
                .code("2021.3")
                .career(career)
                .build();

        student = StudentEntity.builder()
                .run("20123456-5")
                .firstName("Sofia")
                .paternalLastName("Morales")
                .maternalLastName("Herrera")
                .academicStatus(AcademicStatus.REGULAR)
                .isMatriculated(true)
                .career(career)
                .studyPlan(studyPlan)
                .build();

        course1 = CourseEntity.builder().id(1L).code("TAP101").name("Fundamentos Prog").semester(1).sctCredits(5).studyPlan(studyPlan).build();
        course2 = CourseEntity.builder().id(2L).code("TAP102").name("Matematica").semester(1).sctCredits(5).studyPlan(studyPlan).build();
        course3 = CourseEntity.builder().id(3L).code("TAP103").name("Bases de Datos").semester(2).sctCredits(6).studyPlan(studyPlan).build();
        course4 = CourseEntity.builder().id(4L).code("TAP104").name("Redes").semester(2).sctCredits(4).studyPlan(studyPlan).build();

        period1 = AcademicPeriodEntity.builder().id("2026-1").year(2026).semester(1).isOpen(false).build();
        period2 = AcademicPeriodEntity.builder().id("2026-2").year(2026).semester(2).isOpen(true).build();
    }

    @Test
    @DisplayName("getStudentAcademicHistory - Generates correct transcript and GPA")
    void getStudentAcademicHistory_Success() {
        TeacherEntity teacher = TeacherEntity.builder().firstName("Maria").paternalLastName("Gonzalez").build();
        SectionEntity sec1 = SectionEntity.builder().sectionCode("TAP101-A-1").teacher(teacher).build();

        EnrollmentEntity e1 = EnrollmentEntity.builder()
                .id(1L)
                .student(student)
                .course(course1)
                .section(sec1)
                .academicPeriod(period1)
                .enrollmentDate(LocalDateTime.now().minusMonths(6))
                .finalGrade(6.0)
                .status(EnrollmentStatus.APROBADA)
                .build();

        EnrollmentEntity e2 = EnrollmentEntity.builder()
                .id(2L)
                .student(student)
                .course(course2)
                .section(sec1)
                .academicPeriod(period1)
                .enrollmentDate(LocalDateTime.now().minusMonths(6))
                .finalGrade(4.0)
                .status(EnrollmentStatus.APROBADA)
                .build();

        when(studentRepository.findById("20123456-5")).thenReturn(Optional.of(student));
        when(enrollmentRepository.findByStudent_RunOrderByAcademicPeriod_IdAscEnrollmentDateAsc("20123456-5"))
                .thenReturn(List.of(e1, e2));
        when(courseRepository.findByStudyPlan_Id(1L)).thenReturn(List.of(course1, course2, course3, course4));

        StudentAcademicHistoryDTO history = academicHistoryService.getStudentAcademicHistory("20123456-5");

        assertNotNull(history);
        assertEquals("20123456-5", history.getStudentRun());
        assertEquals(5.0, history.getCumulativeGpa());
        assertEquals(10, history.getTotalApprovedCredits());
        assertEquals(2, history.getTotalApprovedCourses());
        assertEquals(4, history.getTotalPlanCourses());
        assertEquals(1, history.getPeriods().size());
        assertEquals(5.0, history.getPeriods().getFirst().getPeriodGpa());
    }

    @Test
    @DisplayName("getStudentAcademicHistory - Throws exception if student not found")
    void getStudentAcademicHistory_NotFound() {
        when(studentRepository.findById("99999999-9")).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () ->
                academicHistoryService.getStudentAcademicHistory("99999999-9")
        );
    }

    @Test
    @DisplayName("getStudentCurriculumProgress - Maps color codes: APPROVED (green), FAILED (red), ENROLLED (yellow), PENDING (white)")
    void getStudentCurriculumProgress_VisualStatuses() {
        EnrollmentEntity eApproved = EnrollmentEntity.builder()
                .id(1L)
                .student(student)
                .course(course1)
                .finalGrade(5.5)
                .status(EnrollmentStatus.APROBADA)
                .build();

        EnrollmentEntity eFailed = EnrollmentEntity.builder()
                .id(2L)
                .student(student)
                .course(course2)
                .finalGrade(3.0)
                .status(EnrollmentStatus.REPROBADA)
                .build();

        EnrollmentEntity eEnrolled = EnrollmentEntity.builder()
                .id(3L)
                .student(student)
                .course(course3)
                .finalGrade(null)
                .status(EnrollmentStatus.INSCRITA)
                .build();

        when(studentRepository.findById("20123456-5")).thenReturn(Optional.of(student));
        when(courseRepository.findByStudyPlan_IdOrderBySemesterAscCodeAsc(1L))
                .thenReturn(List.of(course1, course2, course3, course4));
        when(enrollmentRepository.findByStudent_Run("20123456-5"))
                .thenReturn(List.of(eApproved, eFailed, eEnrolled));

        StudentCurriculumProgressDTO progress = academicHistoryService.getStudentCurriculumProgress("20123456-5");

        assertNotNull(progress);
        assertEquals(4, progress.getCourses().size());

        // course1 -> APPROVED (Verde)
        assertEquals("APPROVED", progress.getCourses().get(0).getStatus());
        assertEquals(0, progress.getCourses().get(0).getFailCount());

        // course2 -> FAILED (Rojo)
        assertEquals("FAILED", progress.getCourses().get(1).getStatus());
        assertEquals(1, progress.getCourses().get(1).getFailCount());

        // course3 -> ENROLLED (Amarillo)
        assertEquals("ENROLLED", progress.getCourses().get(2).getStatus());

        // course4 -> PENDING (Blanco)
        assertEquals("PENDING", progress.getCourses().get(3).getStatus());
    }
}
