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
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * StudyPlanService handles business logic related to career study plans.
 * Layer: Service.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class StudyPlanService {

    private final StudyPlanRepository studyPlanRepository;
    private final CareerRepository careerRepository;
    private final StudentRepository studentRepository;

    /**
     * Retrieves all study plans associated with a specific career.
     *
     * @param careerCode the unique code of the career
     * @return list of study plan response DTOs
     * @throws ResourceNotFoundException if career does not exist
     */
    @Transactional(readOnly = true)
    public List<StudyPlanResponseDTO> getPlansByCareer(String careerCode) {
        if (!careerRepository.existsByCode(careerCode)) {
            throw new ResourceNotFoundException("Career not found with code: " + careerCode);
        }

        return studyPlanRepository.findByCareer_Code(careerCode).stream()
                .map(this::mapToDTO)
                .toList();
    }

    /**
     * Retrieves a single study plan by its ID.
     *
     * @param id study plan ID
     * @return study plan response DTO
     * @throws ResourceNotFoundException if study plan does not exist
     */
    @Transactional(readOnly = true)
    public StudyPlanResponseDTO getStudyPlanById(Long id) {
        StudyPlanEntity plan = studyPlanRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Study plan not found with ID: " + id));
        return mapToDTO(plan);
    }

    /**
     * Creates a new study plan for a career.
     * If marked as active, automatically deactivates any currently active plan
     * of that career without altering existing enrolled students.
     *
     * @param dto registration payload
     * @return created study plan response DTO
     * @throws ResourceNotFoundException if career does not exist
     * @throws BusinessRuleException if plan code already exists for this career
     */
    @Transactional
    public StudyPlanResponseDTO createStudyPlan(StudyPlanRegistrationDTO dto) {
        String cleanCareerCode = dto.getCareerCode().trim();
        String cleanPlanCode = dto.getCode().trim();

        CareerEntity career = careerRepository.findByCode(cleanCareerCode)
                .orElseThrow(() -> new ResourceNotFoundException("Career not found with code: " + cleanCareerCode));

        if (studyPlanRepository.existsByCareer_CodeAndCode(cleanCareerCode, cleanPlanCode)) {
            throw new BusinessRuleException("A study plan with code '" + cleanPlanCode
                    + "' already exists for career " + cleanCareerCode);
        }

        boolean makeActive = dto.getIsActive() != null ? dto.getIsActive() : true;

        // If this is the only plan being created or explicitly marked active, deactivate current active plan
        if (makeActive) {
            studyPlanRepository.findByCareer_CodeAndIsActiveTrue(cleanCareerCode)
                    .ifPresent(currentActive -> {
                        currentActive.setIsActive(false);
                        studyPlanRepository.save(currentActive);
                        log.info("Previous active study plan {} deactivated for career {}",
                                currentActive.getCode(), cleanCareerCode);
                    });
        } else {
            // If career has no active plans yet, force this first plan to be active
            if (studyPlanRepository.findByCareer_CodeAndIsActiveTrue(cleanCareerCode).isEmpty()) {
                makeActive = true;
            }
        }

        StudyPlanEntity newPlan = StudyPlanEntity.builder()
                .code(cleanPlanCode)
                .career(career)
                .isActive(makeActive)
                .build();

        StudyPlanEntity savedPlan = studyPlanRepository.save(newPlan);
        log.info("Study plan {} created successfully for career {}. Active status: {}",
                savedPlan.getCode(), cleanCareerCode, savedPlan.getIsActive());

        return mapToDTO(savedPlan);
    }

    /**
     * Activates a study plan, making it the active (vigente) curriculum for its career.
     * The previously active plan becomes inactive.
     *
     * @param id study plan ID to activate
     * @return updated study plan response DTO
     * @throws ResourceNotFoundException if study plan does not exist
     */
    @Transactional
    public StudyPlanResponseDTO activateStudyPlan(Long id) {
        StudyPlanEntity targetPlan = studyPlanRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Study plan not found with ID: " + id));

        if (Boolean.TRUE.equals(targetPlan.getIsActive())) {
            log.info("Study plan {} is already active for career {}", targetPlan.getCode(), targetPlan.getCareer().getCode());
            return mapToDTO(targetPlan);
        }

        String careerCode = targetPlan.getCareer().getCode();

        // Deactivate currently active plan
        studyPlanRepository.findByCareer_CodeAndIsActiveTrue(careerCode)
                .ifPresent(currentActive -> {
                    if (!currentActive.getId().equals(id)) {
                        currentActive.setIsActive(false);
                        studyPlanRepository.save(currentActive);
                        log.info("Deactivated previous active study plan {} for career {}",
                                currentActive.getCode(), careerCode);
                    }
                });

        targetPlan.setIsActive(true);
        StudyPlanEntity updated = studyPlanRepository.save(targetPlan);
        log.info("Activated study plan {} for career {}", updated.getCode(), careerCode);

        return mapToDTO(updated);
    }

    /**
     * Deletes a study plan if it has no enrolled students.
     *
     * @param id study plan ID
     * @throws ResourceNotFoundException if study plan does not exist
     * @throws BusinessRuleException if study plan has enrolled students
     */
    @Transactional
    public void deleteStudyPlan(Long id) {
        StudyPlanEntity plan = studyPlanRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Study plan not found with ID: " + id));

        long enrolledCount = studentRepository.countByStudyPlan_Id(id);
        if (enrolledCount > 0) {
            throw new BusinessRuleException("Cannot delete study plan because it has " + enrolledCount
                    + " enrolled student(s).");
        }

        studyPlanRepository.delete(plan);
        log.info("Study plan {} deleted successfully", id);
    }

    private StudyPlanResponseDTO mapToDTO(StudyPlanEntity entity) {
        long enrolledCount = studentRepository.countByStudyPlan_Id(entity.getId());
        return StudyPlanResponseDTO.builder()
                .id(entity.getId())
                .code(entity.getCode())
                .isActive(entity.getIsActive())
                .careerCode(entity.getCareer().getCode())
                .careerName(entity.getCareer().getName())
                .enrolledStudentsCount(enrolledCount)
                .build();
    }
}
