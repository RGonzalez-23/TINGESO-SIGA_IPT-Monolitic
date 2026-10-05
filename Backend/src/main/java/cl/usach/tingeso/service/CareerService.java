package cl.usach.tingeso.service;

import cl.usach.tingeso.dto.CareerResponseDTO;
import cl.usach.tingeso.entity.CareerEntity;
import cl.usach.tingeso.repository.CareerRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * CareerService handles business logic related to careers.
 * Layer: Service.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class CareerService {

    private final CareerRepository careerRepository;

    /**
     * Retrieves all active careers.
     *
     * @return list of active career response DTOs
     */
    @Transactional(readOnly = true)
    public List<CareerResponseDTO> getActiveCareers() {
        return careerRepository.findByIsActiveTrue().stream()
                .map(this::mapToDTO)
                .toList();
    }

    private CareerResponseDTO mapToDTO(CareerEntity entity) {
        return CareerResponseDTO.builder()
                .code(entity.getCode())
                .name(entity.getName())
                .description(entity.getDescription())
                .durationSemesters(entity.getDurationSemesters())
                .isActive(entity.getIsActive())
                .build();
    }
}
