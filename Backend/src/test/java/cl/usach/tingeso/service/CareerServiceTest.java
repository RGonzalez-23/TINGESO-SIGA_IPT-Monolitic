package cl.usach.tingeso.service;

import cl.usach.tingeso.dto.CareerResponseDTO;
import cl.usach.tingeso.entity.CareerEntity;
import cl.usach.tingeso.repository.CareerRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

/**
 * Unit tests for CareerService.
 * Layer: Service.
 */
@ExtendWith(MockitoExtension.class)
class CareerServiceTest {

    @Mock
    private CareerRepository careerRepository;

    @InjectMocks
    private CareerService careerService;

    @Test
    @DisplayName("Should return all active careers")
    void testGetActiveCareers() {
        CareerEntity career = CareerEntity.builder()
                .code("10450")
                .name("Técnico Analista Programador")
                .description("Software")
                .durationSemesters(4)
                .isActive(true)
                .build();

        when(careerRepository.findByIsActiveTrue()).thenReturn(List.of(career));

        List<CareerResponseDTO> result = careerService.getActiveCareers();

        assertEquals(1, result.size());
        assertEquals("10450", result.get(0).getCode());
        assertEquals("Técnico Analista Programador", result.get(0).getName());
    }
}
