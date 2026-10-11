package cl.usach.tingeso.service;

import cl.usach.tingeso.dto.AcademicPeriodCreateDTO;
import cl.usach.tingeso.dto.AcademicPeriodResponseDTO;
import cl.usach.tingeso.entity.AcademicPeriodEntity;
import cl.usach.tingeso.exception.BusinessRuleException;
import cl.usach.tingeso.exception.ResourceNotFoundException;
import cl.usach.tingeso.repository.AcademicPeriodRepository;
import cl.usach.tingeso.repository.SectionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * AcademicPeriodService handles management of academic periods (e.g., "2027-1").
 * Layer: Service.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AcademicPeriodService {

    private final AcademicPeriodRepository academicPeriodRepository;
    private final SectionRepository sectionRepository;

    /**
     * Retrieves all academic periods ordered descending.
     */
    @Transactional(readOnly = true)
    public List<AcademicPeriodResponseDTO> getAllPeriods() {
        return academicPeriodRepository.findAllByOrderByYearDescSemesterDesc().stream()
                .map(this::toResponseDTO)
                .toList();
    }

    /**
     * Retrieves an academic period by ID (e.g. "2027-1").
     */
    @Transactional(readOnly = true)
    public AcademicPeriodResponseDTO getPeriodById(String id) {
        AcademicPeriodEntity entity = academicPeriodRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Período académico no encontrado con ID: " + id));
        return toResponseDTO(entity);
    }

    /**
     * Creates a new academic period in initial ABIERTO (open) state.
     * Enforces the business rule that only one academic period can be open at a time
     * by closing any previously open period.
     */
    @Transactional
    public AcademicPeriodResponseDTO createPeriod(AcademicPeriodCreateDTO dto) {
        String periodId = String.format("%d-%d", dto.getYear(), dto.getSemester());

        if (academicPeriodRepository.existsById(periodId)) {
            throw new BusinessRuleException("El período académico " + periodId + " ya existe en el sistema.");
        }

        // Close any currently open period so strictly only one period is open at a time
        academicPeriodRepository.findAllByIsOpenTrue().forEach(openPeriod -> {
            openPeriod.setIsOpen(false);
            academicPeriodRepository.save(openPeriod);
            log.info("Período {} cerrado automáticamente al crear el nuevo período {}", openPeriod.getId(), periodId);
        });

        AcademicPeriodEntity entity = AcademicPeriodEntity.builder()
                .id(periodId)
                .year(dto.getYear())
                .semester(dto.getSemester())
                .isOpen(true) // Initial state is always ABIERTO
                .build();

        AcademicPeriodEntity saved = academicPeriodRepository.save(entity);
        log.info("Academic period created successfully: {}", saved.getId());
        return toResponseDTO(saved);
    }

    /**
     * Toggles open/closed state of an academic period.
     * If opening a closed period, automatically closes any other currently open period
     * so that strictly only one academic period can be open at a time.
     */
    @Transactional
    public AcademicPeriodResponseDTO togglePeriodStatus(String id) {
        AcademicPeriodEntity entity = academicPeriodRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Período académico no encontrado con ID: " + id));

        if (!entity.getIsOpen()) {
            // Opening this period: close all other currently open periods first
            academicPeriodRepository.findAllByIsOpenTrue().forEach(openPeriod -> {
                if (!openPeriod.getId().equals(id)) {
                    openPeriod.setIsOpen(false);
                    academicPeriodRepository.save(openPeriod);
                    log.info("Período {} cerrado automáticamente al abrir {}", openPeriod.getId(), id);
                }
            });
            entity.setIsOpen(true);
        } else {
            entity.setIsOpen(false);
        }

        AcademicPeriodEntity updated = academicPeriodRepository.save(entity);
        log.info("Academic period {} status changed to: {}", id, updated.getIsOpen() ? "ABIERTO" : "CERRADO");
        return toResponseDTO(updated);
    }

    private AcademicPeriodResponseDTO toResponseDTO(AcademicPeriodEntity entity) {
        long sections = sectionRepository.countByAcademicPeriodId(entity.getId());
        return AcademicPeriodResponseDTO.builder()
                .id(entity.getId())
                .year(entity.getYear())
                .semester(entity.getSemester())
                .isOpen(entity.getIsOpen())
                .sectionCount(sections)
                .build();
    }
}
