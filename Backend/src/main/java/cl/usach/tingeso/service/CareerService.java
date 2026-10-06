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
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * CareerService handles business logic related to careers and their lifecycle.
 * Layer: Service.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class CareerService {

    private final CareerRepository careerRepository;
    private final StudyPlanRepository studyPlanRepository;
    private final StudentRepository studentRepository;

    /**
     * Retrieves all careers (both active and inactive) with their active study plan and statistics.
     *
     * @return list of career response DTOs
     */
    @Transactional(readOnly = true)
    public List<CareerResponseDTO> getAllCareers() {
        return careerRepository.findAll().stream()
                .map(this::mapToDetailedDTO)
                .toList();
    }

    /**
     * Retrieves all active careers.
     *
     * @return list of active career response DTOs
     */
    @Transactional(readOnly = true)
    public List<CareerResponseDTO> getActiveCareers() {
        return careerRepository.findByIsActiveTrue().stream()
                .map(this::mapToDetailedDTO)
                .toList();
    }

    /**
     * Retrieves a single career by its unique code.
     *
     * @param code unique career code
     * @return career response DTO
     * @throws ResourceNotFoundException if career does not exist
     */
    @Transactional(readOnly = true)
    public CareerResponseDTO getCareerByCode(String code) {
        CareerEntity entity = careerRepository.findByCode(code)
                .orElseThrow(() -> new ResourceNotFoundException("Career not found with code: " + code));
        return mapToDetailedDTO(entity);
    }

    /**
     * Registers a new career and creates its initial active study plan.
     *
     * @param dto registration payload
     * @return created career response DTO
     * @throws BusinessRuleException if career code already exists
     */
    @Transactional
    public CareerResponseDTO registerCareer(CareerRegistrationDTO dto) {
        String cleanCode = dto.getCode().trim();
        if (careerRepository.existsByCode(cleanCode)) {
            throw new BusinessRuleException("A career already exists with code: " + cleanCode);
        }

        CareerEntity careerEntity = CareerEntity.builder()
                .code(cleanCode)
                .name(dto.getName().trim())
                .description(dto.getDescription().trim())
                .durationSemesters(dto.getDurationSemesters() != null ? dto.getDurationSemesters() : 4)
                .isActive(true)
                .build();

        CareerEntity savedCareer = careerRepository.save(careerEntity);
        log.info("Career registered successfully with code: {}", savedCareer.getCode());

        String cleanPlanCode = dto.getInitialPlanCode().trim();
        StudyPlanEntity initialPlan = StudyPlanEntity.builder()
                .code(cleanPlanCode)
                .career(savedCareer)
                .isActive(true)
                .build();

        studyPlanRepository.save(initialPlan);
        log.info("Initial active study plan {} registered for career {}", cleanPlanCode, savedCareer.getCode());

        return CareerResponseDTO.builder()
                .code(savedCareer.getCode())
                .name(savedCareer.getName())
                .description(savedCareer.getDescription())
                .durationSemesters(savedCareer.getDurationSemesters())
                .isActive(savedCareer.getIsActive())
                .activeStudyPlanCode(cleanPlanCode)
                .totalStudyPlans(1L)
                .totalEnrolledStudents(0L)
                .build();
    }

    /**
     * Updates an existing career's name, description, and active status.
     *
     * @param code unique career code
     * @param dto update payload
     * @return updated career response DTO
     * @throws ResourceNotFoundException if career does not exist
     */
    @Transactional
    public CareerResponseDTO updateCareer(String code, CareerUpdateDTO dto) {
        CareerEntity career = careerRepository.findByCode(code)
                .orElseThrow(() -> new ResourceNotFoundException("Career not found with code: " + code));

        career.setName(dto.getName().trim());
        career.setDescription(dto.getDescription().trim());
        career.setIsActive(dto.getIsActive());

        CareerEntity updatedCareer = careerRepository.save(career);
        log.info("Career {} updated successfully. Active status: {}", code, updatedCareer.getIsActive());

        return mapToDetailedDTO(updatedCareer);
    }

    /**
     * Deletes a career if it has no associated study plans or enrolled students.
     *
     * @param code unique career code
     * @throws ResourceNotFoundException if career does not exist
     * @throws BusinessRuleException if career cannot be deleted due to existing plans or students
     */
    @Transactional
    public void deleteCareer(String code) {
        CareerEntity career = careerRepository.findByCode(code)
                .orElseThrow(() -> new ResourceNotFoundException("Career not found with code: " + code));

        long enrolledStudents = studentRepository.countByCareer_Code(code);
        if (enrolledStudents > 0) {
            throw new BusinessRuleException("Cannot delete career because it has " + enrolledStudents
                    + " enrolled student(s). Deactivate the career instead.");
        }

        long studyPlansCount = studyPlanRepository.countByCareer_Code(code);
        if (studyPlansCount > 0) {
            throw new BusinessRuleException("Cannot delete career because it has " + studyPlansCount
                    + " associated study plan(s). Deactivate the career instead.");
        }

        careerRepository.delete(career);
        log.info("Career {} deleted successfully", code);
    }

    private CareerResponseDTO mapToDetailedDTO(CareerEntity entity) {
        String activePlanCode = studyPlanRepository.findByCareer_CodeAndIsActiveTrue(entity.getCode())
                .map(StudyPlanEntity::getCode)
                .orElse(null);

        long totalPlans = studyPlanRepository.countByCareer_Code(entity.getCode());
        long totalStudents = studentRepository.countByCareer_Code(entity.getCode());

        return CareerResponseDTO.builder()
                .code(entity.getCode())
                .name(entity.getName())
                .description(entity.getDescription())
                .durationSemesters(entity.getDurationSemesters())
                .isActive(entity.getIsActive())
                .activeStudyPlanCode(activePlanCode)
                .totalStudyPlans(totalPlans)
                .totalEnrolledStudents(totalStudents)
                .build();
    }
}
