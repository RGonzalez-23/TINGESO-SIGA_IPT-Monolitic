package cl.usach.tingeso.service;

import cl.usach.tingeso.dto.TimeBlockResponseDTO;
import cl.usach.tingeso.entity.TimeBlockEntity;
import cl.usach.tingeso.repository.TimeBlockRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for TimeBlockService.
 * Layer: Service.
 */
@ExtendWith(MockitoExtension.class)
class TimeBlockServiceTest {

    @Mock
    private TimeBlockRepository timeBlockRepository;

    @InjectMocks
    private TimeBlockService timeBlockService;

    @Test
    @DisplayName("Should return all standardized time blocks ordered")
    void testGetAllTimeBlocks_Success() {
        TimeBlockEntity l1 = TimeBlockEntity.builder()
                .code("L1")
                .dayOfWeek("L")
                .moduleNumber(1)
                .startTime(LocalTime.of(8, 15))
                .endTime(LocalTime.of(9, 35))
                .build();

        TimeBlockEntity m2 = TimeBlockEntity.builder()
                .code("M2")
                .dayOfWeek("M")
                .moduleNumber(2)
                .startTime(LocalTime.of(9, 50))
                .endTime(LocalTime.of(11, 10))
                .build();

        when(timeBlockRepository.findAllByOrderByDayOfWeekAscModuleNumberAsc()).thenReturn(List.of(l1, m2));

        List<TimeBlockResponseDTO> results = timeBlockService.getAllTimeBlocks();

        assertNotNull(results);
        assertEquals(2, results.size());
        assertEquals("L1", results.get(0).getCode());
        assertEquals("Lunes", results.get(0).getDayName());
        assertEquals("08:15 - 09:35", results.get(0).getScheduleRange());

        assertEquals("M2", results.get(1).getCode());
        assertEquals("Martes", results.get(1).getDayName());
        assertEquals("09:50 - 11:10", results.get(1).getScheduleRange());

        verify(timeBlockRepository).findAllByOrderByDayOfWeekAscModuleNumberAsc();
    }
}
