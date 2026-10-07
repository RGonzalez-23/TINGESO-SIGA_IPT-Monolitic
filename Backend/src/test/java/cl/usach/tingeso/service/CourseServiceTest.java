package cl.usach.tingeso.service;

import cl.usach.tingeso.dto.CourseRegistrationDTO;
import cl.usach.tingeso.dto.CourseResponseDTO;
import cl.usach.tingeso.dto.CourseUpdateDTO;
import cl.usach.tingeso.entity.CareerEntity;
import cl.usach.tingeso.entity.CourseEntity;
import cl.usach.tingeso.entity.StudyPlanEntity;
import cl.usach.tingeso.exception.BusinessRuleException;
import cl.usach.tingeso.exception.ResourceNotFoundException;
import cl.usach.tingeso.repository.CourseRepository;
import cl.usach.tingeso.repository.StudyPlanRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for CourseService.
 * Layer: Service.
 * Ensures >= 90% code coverage.
 */
@ExtendWith(MockitoExtension.class)
class CourseServiceTest {

    @Mock
    private CourseRepository courseRepository;

    @Mock
    private StudyPlanRepository studyPlanRepository;

    @InjectMocks
    private CourseService courseService;

    private CareerEntity sampleCareer;
    private StudyPlanEntity samplePlan;
    private CourseEntity sampleCourse1;
    private CourseEntity sampleCourse2;

    @BeforeEach
    void setUp() {
        sampleCareer = CareerEntity.builder()
                .code("10450")
                .name("Técnico Analista Programador")
                .description("Software")
                .durationSemesters(4)
                .isActive(true)
                .build();

        samplePlan = StudyPlanEntity.builder()
                .id(1L)
                .code("2021.3")
                .career(sampleCareer)
                .isActive(true)
                .build();

        sampleCourse1 = CourseEntity.builder()
                .id(101L)
                .code("TAP101")
                .name("Fundamentos de Programación")
                .semester(1)
                .theoryHours(2)
                .exerciseHours(2)
                .laboratoryHours(2)
                .sctCredits(6)
                .studyPlan(samplePlan)
                .prerequisites(new HashSet<>())
                .build();

        sampleCourse2 = CourseEntity.builder()
                .id(201L)
                .code("TAP201")
                .name("Programación Orientada a Objetos")
                .semester(2)
                .theoryHours(2)
                .exerciseHours(2)
                .laboratoryHours(2)
                .sctCredits(6)
                .studyPlan(samplePlan)
                .prerequisites(new HashSet<>(Set.of(sampleCourse1)))
                .build();
    }

    @Test
    @DisplayName("Should return all courses for a study plan")
    void testGetCoursesByStudyPlan_Success() {
        when(studyPlanRepository.existsById(1L)).thenReturn(true);
        when(courseRepository.findByStudyPlan_IdOrderBySemesterAscCodeAsc(1L))
                .thenReturn(List.of(sampleCourse1, sampleCourse2));

        List<CourseResponseDTO> result = courseService.getCoursesByStudyPlan(1L);

        assertEquals(2, result.size());
        assertEquals("TAP101", result.get(0).getCode());
        assertEquals("2-2-2", result.get(0).getTel());
        assertEquals(6, result.get(0).getTotalHours());
    }

    @Test
    @DisplayName("Should throw ResourceNotFoundException when study plan does not exist in getCoursesByStudyPlan")
    void testGetCoursesByStudyPlan_NotFound() {
        when(studyPlanRepository.existsById(99L)).thenReturn(false);

        assertThrows(ResourceNotFoundException.class, () -> courseService.getCoursesByStudyPlan(99L));
    }

    @Test
    @DisplayName("Should return courses filtered by semester")
    void testGetCoursesByStudyPlanAndSemester_Success() {
        when(studyPlanRepository.existsById(1L)).thenReturn(true);
        when(courseRepository.findByStudyPlan_IdAndSemesterOrderByCodeAsc(1L, 1))
                .thenReturn(List.of(sampleCourse1));

        List<CourseResponseDTO> result = courseService.getCoursesByStudyPlanAndSemester(1L, 1);

        assertEquals(1, result.size());
        assertEquals("TAP101", result.get(0).getCode());
    }

    @Test
    @DisplayName("Should throw BusinessRuleException for invalid semester in getCoursesByStudyPlanAndSemester")
    void testGetCoursesByStudyPlanAndSemester_InvalidSemester() {
        when(studyPlanRepository.existsById(1L)).thenReturn(true);

        assertThrows(BusinessRuleException.class, () -> courseService.getCoursesByStudyPlanAndSemester(1L, 5));
    }

    @Test
    @DisplayName("Should get course by ID when found")
    void testGetCourseById_Success() {
        when(courseRepository.findById(101L)).thenReturn(Optional.of(sampleCourse1));

        CourseResponseDTO result = courseService.getCourseById(101L);

        assertNotNull(result);
        assertEquals("TAP101", result.getCode());
        assertEquals(6, result.getSctCredits());
    }

    @Test
    @DisplayName("Should throw ResourceNotFoundException when course ID not found")
    void testGetCourseById_NotFound() {
        when(courseRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> courseService.getCourseById(999L));
    }

    @Test
    @DisplayName("Should create course with prerequisites successfully")
    void testCreateCourse_SuccessWithPrerequisites() {
        CourseRegistrationDTO dto = CourseRegistrationDTO.builder()
                .studyPlanId(1L)
                .code("TAP201")
                .name("Programación Orientada a Objetos")
                .semester(2)
                .theoryHours(2)
                .exerciseHours(2)
                .laboratoryHours(2)
                .sctCredits(6)
                .prerequisiteCodes(List.of("TAP101"))
                .build();

        when(studyPlanRepository.findById(1L)).thenReturn(Optional.of(samplePlan));
        when(courseRepository.existsByStudyPlan_IdAndCode(1L, "TAP201")).thenReturn(false);
        when(courseRepository.findByStudyPlan_IdAndCode(1L, "TAP101")).thenReturn(Optional.of(sampleCourse1));
        when(courseRepository.save(any(CourseEntity.class))).thenAnswer(inv -> {
            CourseEntity entity = inv.getArgument(0);
            entity.setId(201L);
            return entity;
        });

        CourseResponseDTO result = courseService.createCourse(dto);

        assertNotNull(result);
        assertEquals("TAP201", result.getCode());
        assertEquals(1, result.getPrerequisites().size());
        assertEquals("TAP101", result.getPrerequisites().get(0).getCode());
    }

    @Test
    @DisplayName("Should throw BusinessRuleException when duplicate course code in study plan")
    void testCreateCourse_DuplicateCode() {
        CourseRegistrationDTO dto = CourseRegistrationDTO.builder()
                .studyPlanId(1L)
                .code("TAP101")
                .name("Duplicate")
                .semester(1)
                .theoryHours(2)
                .exerciseHours(2)
                .laboratoryHours(2)
                .sctCredits(6)
                .build();

        when(studyPlanRepository.findById(1L)).thenReturn(Optional.of(samplePlan));
        when(courseRepository.existsByStudyPlan_IdAndCode(1L, "TAP101")).thenReturn(true);

        assertThrows(BusinessRuleException.class, () -> courseService.createCourse(dto));
        verify(courseRepository, never()).save(any(CourseEntity.class));
    }

    @Test
    @DisplayName("Should throw BusinessRuleException when TEL hours sum >= 8")
    void testCreateCourse_TelHoursExceeded() {
        CourseRegistrationDTO dto = CourseRegistrationDTO.builder()
                .studyPlanId(1L)
                .code("TAP999")
                .name("Too Many Hours")
                .semester(1)
                .theoryHours(4)
                .exerciseHours(2)
                .laboratoryHours(2) // Sum = 8, must be < 8
                .sctCredits(6)
                .build();

        when(studyPlanRepository.findById(1L)).thenReturn(Optional.of(samplePlan));
        when(courseRepository.existsByStudyPlan_IdAndCode(1L, "TAP999")).thenReturn(false);

        assertThrows(BusinessRuleException.class, () -> courseService.createCourse(dto));
    }

    @Test
    @DisplayName("Should throw BusinessRuleException when TEL hours sum <= 0 or negative")
    void testCreateCourse_TelHoursZeroOrNegative() {
        CourseRegistrationDTO dtoZero = CourseRegistrationDTO.builder()
                .studyPlanId(1L)
                .code("TAP999")
                .name("Zero Hours")
                .semester(1)
                .theoryHours(0)
                .exerciseHours(0)
                .laboratoryHours(0)
                .sctCredits(4)
                .build();

        when(studyPlanRepository.findById(1L)).thenReturn(Optional.of(samplePlan));
        when(courseRepository.existsByStudyPlan_IdAndCode(1L, "TAP999")).thenReturn(false);

        assertThrows(BusinessRuleException.class, () -> courseService.createCourse(dtoZero));
    }

    @Test
    @DisplayName("Should throw BusinessRuleException when SCT credits out of range")
    void testCreateCourse_InvalidSct() {
        CourseRegistrationDTO dtoInvalidSct = CourseRegistrationDTO.builder()
                .studyPlanId(1L)
                .code("TAP999")
                .name("Invalid SCT")
                .semester(1)
                .theoryHours(2)
                .exerciseHours(2)
                .laboratoryHours(0)
                .sctCredits(8) // Allowed: 1 to 7
                .build();

        when(studyPlanRepository.findById(1L)).thenReturn(Optional.of(samplePlan));
        when(courseRepository.existsByStudyPlan_IdAndCode(1L, "TAP999")).thenReturn(false);

        assertThrows(BusinessRuleException.class, () -> courseService.createCourse(dtoInvalidSct));
    }

    @Test
    @DisplayName("Should throw BusinessRuleException when course has more than 3 prerequisites")
    void testCreateCourse_TooManyPrerequisites() {
        CourseRegistrationDTO dto = CourseRegistrationDTO.builder()
                .studyPlanId(1L)
                .code("TAP401")
                .name("Advanced Software")
                .semester(4)
                .theoryHours(2)
                .exerciseHours(2)
                .laboratoryHours(2)
                .sctCredits(6)
                .prerequisiteCodes(List.of("C1", "C2", "C3", "C4"))
                .build();

        when(studyPlanRepository.findById(1L)).thenReturn(Optional.of(samplePlan));
        when(courseRepository.existsByStudyPlan_IdAndCode(1L, "TAP401")).thenReturn(false);

        assertThrows(BusinessRuleException.class, () -> courseService.createCourse(dto));
    }

    @Test
    @DisplayName("Should throw BusinessRuleException when prerequisite is in same or higher semester")
    void testCreateCourse_InvalidPrerequisiteSemester() {
        CourseRegistrationDTO dto = CourseRegistrationDTO.builder()
                .studyPlanId(1L)
                .code("TAP102")
                .name("Course Sem 1")
                .semester(1)
                .theoryHours(2)
                .exerciseHours(2)
                .laboratoryHours(0)
                .sctCredits(5)
                .prerequisiteCodes(List.of("TAP201")) // TAP201 is in Semester 2!
                .build();

        when(studyPlanRepository.findById(1L)).thenReturn(Optional.of(samplePlan));
        when(courseRepository.existsByStudyPlan_IdAndCode(1L, "TAP102")).thenReturn(false);
        when(courseRepository.findByStudyPlan_IdAndCode(1L, "TAP201")).thenReturn(Optional.of(sampleCourse2));

        assertThrows(BusinessRuleException.class, () -> courseService.createCourse(dto));
    }

    @Test
    @DisplayName("Should throw BusinessRuleException when course sets itself as prerequisite")
    void testCreateCourse_SelfPrerequisite() {
        CourseRegistrationDTO dto = CourseRegistrationDTO.builder()
                .studyPlanId(1L)
                .code("TAP201")
                .name("Course Sem 2")
                .semester(2)
                .theoryHours(2)
                .exerciseHours(2)
                .laboratoryHours(0)
                .sctCredits(5)
                .prerequisiteCodes(List.of("TAP201"))
                .build();

        when(studyPlanRepository.findById(1L)).thenReturn(Optional.of(samplePlan));
        when(courseRepository.existsByStudyPlan_IdAndCode(1L, "TAP201")).thenReturn(false);

        assertThrows(BusinessRuleException.class, () -> courseService.createCourse(dto));
    }

    @Test
    @DisplayName("Should throw BusinessRuleException when prerequisite does not exist in study plan")
    void testCreateCourse_PrerequisiteNotFound() {
        CourseRegistrationDTO dto = CourseRegistrationDTO.builder()
                .studyPlanId(1L)
                .code("TAP201")
                .name("Course Sem 2")
                .semester(2)
                .theoryHours(2)
                .exerciseHours(2)
                .laboratoryHours(0)
                .sctCredits(5)
                .prerequisiteCodes(List.of("UNKNOWN"))
                .build();

        when(studyPlanRepository.findById(1L)).thenReturn(Optional.of(samplePlan));
        when(courseRepository.existsByStudyPlan_IdAndCode(1L, "TAP201")).thenReturn(false);
        when(courseRepository.findByStudyPlan_IdAndCode(1L, "UNKNOWN")).thenReturn(Optional.empty());

        assertThrows(BusinessRuleException.class, () -> courseService.createCourse(dto));
    }

    @Test
    @DisplayName("Should update course successfully")
    void testUpdateCourse_Success() {
        CourseUpdateDTO dto = CourseUpdateDTO.builder()
                .name("Fundamentos de Programación Avanzada")
                .semester(1)
                .theoryHours(2)
                .exerciseHours(2)
                .laboratoryHours(1)
                .sctCredits(5)
                .prerequisiteCodes(new ArrayList<>())
                .build();

        when(courseRepository.findById(101L)).thenReturn(Optional.of(sampleCourse1));
        when(courseRepository.save(any(CourseEntity.class))).thenAnswer(inv -> inv.getArgument(0));

        CourseResponseDTO result = courseService.updateCourse(101L, dto);

        assertNotNull(result);
        assertEquals("Fundamentos de Programación Avanzada", result.getName());
        assertEquals("2-2-1", result.getTel());
        assertEquals(5, result.getSctCredits());
    }

    @Test
    @DisplayName("Should throw ResourceNotFoundException when updating non-existent course")
    void testUpdateCourse_NotFound() {
        CourseUpdateDTO dto = CourseUpdateDTO.builder()
                .name("Name")
                .semester(1)
                .theoryHours(2)
                .exerciseHours(2)
                .laboratoryHours(0)
                .sctCredits(4)
                .build();

        when(courseRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> courseService.updateCourse(999L, dto));
    }

    @Test
    @DisplayName("Should delete course successfully when no dependent courses exist")
    void testDeleteCourse_Success() {
        when(courseRepository.findById(201L)).thenReturn(Optional.of(sampleCourse2));
        when(courseRepository.countByPrerequisitesContaining(sampleCourse2)).thenReturn(0L);

        courseService.deleteCourse(201L);

        verify(courseRepository).delete(sampleCourse2);
    }

    @Test
    @DisplayName("Should throw BusinessRuleException when deleting course that is prerequisite for other courses")
    void testDeleteCourse_BlockedByPrerequisiteDependents() {
        when(courseRepository.findById(101L)).thenReturn(Optional.of(sampleCourse1));
        when(courseRepository.countByPrerequisitesContaining(sampleCourse1)).thenReturn(2L);

        assertThrows(BusinessRuleException.class, () -> courseService.deleteCourse(101L));
        verify(courseRepository, never()).delete(any(CourseEntity.class));
    }
}
