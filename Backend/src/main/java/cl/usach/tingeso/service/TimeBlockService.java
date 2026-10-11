package cl.usach.tingeso.service;

import cl.usach.tingeso.dto.TimeBlockResponseDTO;
import cl.usach.tingeso.entity.TimeBlockEntity;
import cl.usach.tingeso.repository.TimeBlockRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * TimeBlockService handles retrieval of standardized academic schedule blocks.
 * Layer: Service.
 */
@Service
@RequiredArgsConstructor
public class TimeBlockService {

    private final TimeBlockRepository timeBlockRepository;
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm");

    /**
     * Retrieves all 30 standardized time blocks.
     */
    @Transactional(readOnly = true)
    public List<TimeBlockResponseDTO> getAllTimeBlocks() {
        return timeBlockRepository.findAllByOrderByDayOfWeekAscModuleNumberAsc().stream()
                .map(this::toResponseDTO)
                .toList();
    }

    public TimeBlockResponseDTO toResponseDTO(TimeBlockEntity entity) {
        String range = String.format("%s - %s",
                entity.getStartTime().format(TIME_FORMATTER),
                entity.getEndTime().format(TIME_FORMATTER));

        return TimeBlockResponseDTO.builder()
                .code(entity.getCode())
                .dayOfWeek(entity.getDayOfWeek())
                .dayName(TimeBlockResponseDTO.getSpanishDayName(entity.getDayOfWeek()))
                .moduleNumber(entity.getModuleNumber())
                .startTime(entity.getStartTime())
                .endTime(entity.getEndTime())
                .scheduleRange(range)
                .build();
    }
}
