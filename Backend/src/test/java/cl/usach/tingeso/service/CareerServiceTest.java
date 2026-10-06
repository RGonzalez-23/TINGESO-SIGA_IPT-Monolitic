package cl.usach.tingeso.service;

import cl.usach.tingeso.dto.CareerRegistrationDTO;
import cl.usach.tingeso.dto.CareerResponseDTO;
import cl.usach.tingeso.dto.CareerUpdateDTO;
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
 * Unit tests for CareerService.
 * Layer: Service.
 */
@ExtendWith(MockitoExtension.class)
class CareerServiceTest {

    @Mock
    private CareerRepository careerRepository;

    @Mock
    private StudyPlanRepository studyPlanRepository;

    @Mock
    private StudentRepository studentRepository;

    @InjectMocks
    private CareerService careerService;

    private CareerEntity sampleCareer;

    @BeforeEach
    void setUp() {
        sampleCareer = CareerEntity.builder()
                .code("10450")
                .name("Tecnico Analista Programador")
                .description("Software Development Program")
                .durationSemesters(4)
                .isActive(true)
                .build();
    }

    @Test
    @DisplayName("Should return all active careers")
    void testGetActiveCareers() {
        when(careerRepository.findByIsActiveTrue()).thenReturn(List.of(sampleCareer));
        when(studyPlanRepository.findByCareer_CodeAndIsActiveTrue("10450"))
                .thenReturn(Optional.of(StudyPlanEntity.builder().code("2021.3").build()));
        when(studyPlanRepository.countByCareer_Code("10450")).thenReturn(1L);
        when(studentRepository.countByCareer_Code("10450")).thenReturn(5L);

        List<CareerResponseDTO> result = careerService.getActiveCareers();

        assertEquals(1, result.size());
        assertEquals("10450", result.get(0).getCode());
        assertEquals("2021.3", result.get(0).getActiveStudyPlanCode());
        assertEquals(5L, result.get(0).getTotalEnrolledStudents());
    }

    @Test
    @DisplayName("Should return all careers")
    void testGetAllCareers() {
        when(careerRepository.findAll()).thenReturn(List.of(sampleCareer));
        when(studyPlanRepository.findByCareer_CodeAndIsActiveTrue("10450")).thenReturn(Optional.empty());
        when(studyPlanRepository.countByCareer_Code("10450")).thenReturn(0L);
        when(studentRepository.countByCareer_Code("10450")).thenReturn(0L);

        List<CareerResponseDTO> result = careerService.getAllCareers();

        assertEquals(1, result.size());
        assertEquals("10450", result.get(0).getCode());
    }

    @Test
    @DisplayName("Should get career by code when found")
    void testGetCareerByCode_Success() {
        when(careerRepository.findByCode("10450")).thenReturn(Optional.of(sampleCareer));
        when(studyPlanRepository.findByCareer_CodeAndIsActiveTrue("10450"))
                .thenReturn(Optional.of(StudyPlanEntity.builder().code("2021.3").build()));
        when(studyPlanRepository.countByCareer_Code("10450")).thenReturn(1L);
        when(studentRepository.countByCareer_Code("10450")).thenReturn(2L);

        CareerResponseDTO dto = careerService.getCareerByCode("10450");

        assertNotNull(dto);
        assertEquals("10450", dto.getCode());
        assertEquals("2021.3", dto.getActiveStudyPlanCode());
    }

    @Test
    @DisplayName("Should throw ResourceNotFoundException when career code does not exist")
    void testGetCareerByCode_NotFound() {
        when(careerRepository.findByCode("UNKNOWN")).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> careerService.getCareerByCode("UNKNOWN"));
    }

    @Test
    @DisplayName("Should register new career and initial study plan successfully")
    void testRegisterCareer_Success() {
        CareerRegistrationDTO dto = CareerRegistrationDTO.builder()
                .code("10453")
                .name("Tecnico en Ciberseguridad")
                .description("Cybersecurity technical program")
                .durationSemesters(4)
                .initialPlanCode("2026.1")
                .build();

        when(careerRepository.existsByCode("10453")).thenReturn(false);
        when(careerRepository.save(any(CareerEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        CareerResponseDTO result = careerService.registerCareer(dto);

        assertNotNull(result);
        assertEquals("10453", result.getCode());
        assertEquals("2026.1", result.getActiveStudyPlanCode());
        assertTrue(result.getIsActive());
        verify(studyPlanRepository).save(any(StudyPlanEntity.class));
    }

    @Test
    @DisplayName("Should throw BusinessRuleException when career code already exists on registration")
    void testRegisterCareer_DuplicateCode() {
        CareerRegistrationDTO dto = CareerRegistrationDTO.builder()
                .code("10450")
                .name("Duplicate Career")
                .description("Desc")
                .initialPlanCode("2026.1")
                .build();

        when(careerRepository.existsByCode("10450")).thenReturn(true);

        assertThrows(BusinessRuleException.class, () -> careerService.registerCareer(dto));
        verify(careerRepository, never()).save(any(CareerEntity.class));
    }

    @Test
    @DisplayName("Should update existing career")
    void testUpdateCareer_Success() {
        CareerUpdateDTO dto = CareerUpdateDTO.builder()
                .name("Updated Program Name")
                .description("Updated Desc")
                .isActive(false)
                .build();

        when(careerRepository.findByCode("10450")).thenReturn(Optional.of(sampleCareer));
        when(careerRepository.save(any(CareerEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        CareerResponseDTO updated = careerService.updateCareer("10450", dto);

        assertEquals("Updated Program Name", updated.getName());
        assertEquals("Updated Desc", updated.getDescription());
        assertFalse(updated.getIsActive());
    }

    @Test
    @DisplayName("Should throw ResourceNotFoundException when updating non-existent career")
    void testUpdateCareer_NotFound() {
        CareerUpdateDTO dto = CareerUpdateDTO.builder()
                .name("Name")
                .description("Desc")
                .isActive(true)
                .build();

        when(careerRepository.findByCode("UNKNOWN")).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> careerService.updateCareer("UNKNOWN", dto));
    }

    @Test
    @DisplayName("Should delete career when it has no students and no study plans")
    void testDeleteCareer_Success() {
        when(careerRepository.findByCode("10450")).thenReturn(Optional.of(sampleCareer));
        when(studentRepository.countByCareer_Code("10450")).thenReturn(0L);
        when(studyPlanRepository.countByCareer_Code("10450")).thenReturn(0L);

        careerService.deleteCareer("10450");

        verify(careerRepository).delete(sampleCareer);
    }

    @Test
    @DisplayName("Should throw BusinessRuleException when deleting career with enrolled students")
    void testDeleteCareer_BlockedByStudents() {
        when(careerRepository.findByCode("10450")).thenReturn(Optional.of(sampleCareer));
        when(studentRepository.countByCareer_Code("10450")).thenReturn(3L);

        assertThrows(BusinessRuleException.class, () -> careerService.deleteCareer("10450"));
        verify(careerRepository, never()).delete(any(CareerEntity.class));
    }

    @Test
    @DisplayName("Should throw BusinessRuleException when deleting career with associated study plans")
    void testDeleteCareer_BlockedByStudyPlans() {
        when(careerRepository.findByCode("10450")).thenReturn(Optional.of(sampleCareer));
        when(studentRepository.countByCareer_Code("10450")).thenReturn(0L);
        when(studyPlanRepository.countByCareer_Code("10450")).thenReturn(2L);

        assertThrows(BusinessRuleException.class, () -> careerService.deleteCareer("10450"));
        verify(careerRepository, never()).delete(any(CareerEntity.class));
    }
}
