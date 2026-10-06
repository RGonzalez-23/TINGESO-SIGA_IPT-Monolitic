package cl.usach.tingeso.service;

import cl.usach.tingeso.dto.StudyPlanRegistrationDTO;
import cl.usach.tingeso.dto.StudyPlanResponseDTO;
import cl.usach.tingeso.entity.CareerEntity;
import cl.usach.tingeso.entity.StudyPlanEntity;
import cl.usach.tingeso.exception.BusinessRuleException;
import cl.usach.tingeso.exception.ResourceNotFoundException;
import cl.usach.tingeso.repository.CareerRepository;
import cl.usach.tingeso.repository.StudentRepository;
import cl.usach.tingeso.repository.StudyPlanRepository;
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
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for StudyPlanService.
 * Layer: Service.
 */
@ExtendWith(MockitoExtension.class)
class StudyPlanServiceTest {

    @Mock
    private StudyPlanRepository studyPlanRepository;

    @Mock
    private CareerRepository careerRepository;

    @Mock
    private StudentRepository studentRepository;

    @InjectMocks
    private StudyPlanService studyPlanService;

    private CareerEntity sampleCareer;
    private StudyPlanEntity samplePlan1;
    private StudyPlanEntity samplePlan2;

    @BeforeEach
    void setUp() {
        sampleCareer = CareerEntity.builder()
                .code("10450")
                .name("Tecnico Analista Programador")
                .description("Software")
                .durationSemesters(4)
                .isActive(true)
                .build();

        samplePlan1 = StudyPlanEntity.builder()
                .id(1L)
                .code("2021.3")
                .career(sampleCareer)
                .isActive(true)
                .build();

        samplePlan2 = StudyPlanEntity.builder()
                .id(2L)
                .code("2024.1")
                .career(sampleCareer)
                .isActive(false)
                .build();
    }

    @Test
    @DisplayName("Should return study plans for career")
    void testGetPlansByCareer_Success() {
        when(careerRepository.existsByCode("10450")).thenReturn(true);
        when(studyPlanRepository.findByCareer_Code("10450")).thenReturn(List.of(samplePlan1, samplePlan2));
        when(studentRepository.countByStudyPlan_Id(1L)).thenReturn(10L);
        when(studentRepository.countByStudyPlan_Id(2L)).thenReturn(0L);

        List<StudyPlanResponseDTO> plans = studyPlanService.getPlansByCareer("10450");

        assertEquals(2, plans.size());
        assertEquals("2021.3", plans.get(0).getCode());
        assertEquals(10L, plans.get(0).getEnrolledStudentsCount());
        assertTrue(plans.get(0).getIsActive());
        assertFalse(plans.get(1).getIsActive());
    }

    @Test
    @DisplayName("Should throw ResourceNotFoundException when career does not exist")
    void testGetPlansByCareer_CareerNotFound() {
        when(careerRepository.existsByCode("UNKNOWN")).thenReturn(false);

        assertThrows(ResourceNotFoundException.class, () -> studyPlanService.getPlansByCareer("UNKNOWN"));
    }

    @Test
    @DisplayName("Should get study plan by ID when found")
    void testGetStudyPlanById_Success() {
        when(studyPlanRepository.findById(1L)).thenReturn(Optional.of(samplePlan1));
        when(studentRepository.countByStudyPlan_Id(1L)).thenReturn(5L);

        StudyPlanResponseDTO result = studyPlanService.getStudyPlanById(1L);

        assertNotNull(result);
        assertEquals("2021.3", result.getCode());
        assertEquals(5L, result.getEnrolledStudentsCount());
    }

    @Test
    @DisplayName("Should throw ResourceNotFoundException when study plan ID not found")
    void testGetStudyPlanById_NotFound() {
        when(studyPlanRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> studyPlanService.getStudyPlanById(99L));
    }

    @Test
    @DisplayName("Should create active study plan and deactivate existing active plan")
    void testCreateStudyPlan_Active() {
        StudyPlanRegistrationDTO dto = StudyPlanRegistrationDTO.builder()
                .code("2026.1")
                .careerCode("10450")
                .isActive(true)
                .build();

        when(careerRepository.findByCode("10450")).thenReturn(Optional.of(sampleCareer));
        when(studyPlanRepository.existsByCareer_CodeAndCode("10450", "2026.1")).thenReturn(false);
        when(studyPlanRepository.findByCareer_CodeAndIsActiveTrue("10450")).thenReturn(Optional.of(samplePlan1));
        when(studyPlanRepository.save(any(StudyPlanEntity.class))).thenAnswer(invocation -> {
            StudyPlanEntity entity = invocation.getArgument(0);
            if (entity.getId() == null) {
                entity.setId(3L);
            }
            return entity;
        });
        when(studentRepository.countByStudyPlan_Id(3L)).thenReturn(0L);

        StudyPlanResponseDTO result = studyPlanService.createStudyPlan(dto);

        assertNotNull(result);
        assertEquals("2026.1", result.getCode());
        assertTrue(result.getIsActive());
        assertFalse(samplePlan1.getIsActive()); // Verify previous active plan was deactivated
        verify(studyPlanRepository).save(samplePlan1);
    }

    @Test
    @DisplayName("Should throw BusinessRuleException when duplicate study plan code for career")
    void testCreateStudyPlan_DuplicateCode() {
        StudyPlanRegistrationDTO dto = StudyPlanRegistrationDTO.builder()
                .code("2021.3")
                .careerCode("10450")
                .isActive(true)
                .build();

        when(careerRepository.findByCode("10450")).thenReturn(Optional.of(sampleCareer));
        when(studyPlanRepository.existsByCareer_CodeAndCode("10450", "2021.3")).thenReturn(true);

        assertThrows(BusinessRuleException.class, () -> studyPlanService.createStudyPlan(dto));
        verify(studyPlanRepository, never()).save(any(StudyPlanEntity.class));
    }

    @Test
    @DisplayName("Should throw ResourceNotFoundException when creating study plan for non-existent career")
    void testCreateStudyPlan_CareerNotFound() {
        StudyPlanRegistrationDTO dto = StudyPlanRegistrationDTO.builder()
                .code("2026.1")
                .careerCode("UNKNOWN")
                .build();

        when(careerRepository.findByCode("UNKNOWN")).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> studyPlanService.createStudyPlan(dto));
    }

    @Test
    @DisplayName("Should activate study plan and deactivate previously active plan")
    void testActivateStudyPlan_Success() {
        when(studyPlanRepository.findById(2L)).thenReturn(Optional.of(samplePlan2));
        when(studyPlanRepository.findByCareer_CodeAndIsActiveTrue("10450")).thenReturn(Optional.of(samplePlan1));
        when(studyPlanRepository.save(any(StudyPlanEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(studentRepository.countByStudyPlan_Id(2L)).thenReturn(0L);

        StudyPlanResponseDTO result = studyPlanService.activateStudyPlan(2L);

        assertTrue(result.getIsActive());
        assertFalse(samplePlan1.getIsActive());
        verify(studyPlanRepository).save(samplePlan1);
        verify(studyPlanRepository).save(samplePlan2);
    }

    @Test
    @DisplayName("Should return unchanged if study plan is already active")
    void testActivateStudyPlan_AlreadyActive() {
        when(studyPlanRepository.findById(1L)).thenReturn(Optional.of(samplePlan1));
        when(studentRepository.countByStudyPlan_Id(1L)).thenReturn(10L);

        StudyPlanResponseDTO result = studyPlanService.activateStudyPlan(1L);

        assertTrue(result.getIsActive());
        verify(studyPlanRepository, never()).findByCareer_CodeAndIsActiveTrue(any());
    }

    @Test
    @DisplayName("Should delete study plan when no students enrolled")
    void testDeleteStudyPlan_Success() {
        when(studyPlanRepository.findById(2L)).thenReturn(Optional.of(samplePlan2));
        when(studentRepository.countByStudyPlan_Id(2L)).thenReturn(0L);

        studyPlanService.deleteStudyPlan(2L);

        verify(studyPlanRepository).delete(samplePlan2);
    }

    @Test
    @DisplayName("Should throw BusinessRuleException when deleting study plan with enrolled students")
    void testDeleteStudyPlan_BlockedByStudents() {
        when(studyPlanRepository.findById(1L)).thenReturn(Optional.of(samplePlan1));
        when(studentRepository.countByStudyPlan_Id(1L)).thenReturn(5L);

        assertThrows(BusinessRuleException.class, () -> studyPlanService.deleteStudyPlan(1L));
        verify(studyPlanRepository, never()).delete(any(StudyPlanEntity.class));
    }
}
