package cl.usach.tingeso.repository;

import cl.usach.tingeso.entity.StudyPlanEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * StudyPlanRepository provides persistence operations for StudyPlanEntity.
 * Layer: Repository.
 */
@Repository
public interface StudyPlanRepository extends JpaRepository<StudyPlanEntity, Long> {

    /**
     * Finds the currently active (vigente) study plan for a specific career code.
     *
     * @param careerCode the unique code of the career
     * @return Optional containing the active study plan, or empty if none is active
     */
    Optional<StudyPlanEntity> findByCareer_CodeAndIsActiveTrue(String careerCode);

    /**
     * Finds a study plan by career code and study plan version/code.
     *
     * @param careerCode the unique code of the career
     * @param code the code of the study plan
     * @return Optional containing the study plan if found
     */
    Optional<StudyPlanEntity> findByCareer_CodeAndCode(String careerCode, String code);

    /**
     * Retrieves all study plans associated with a given career code.
     *
     * @param careerCode the unique code of the career
     * @return list of study plans
     */
    List<StudyPlanEntity> findByCareer_Code(String careerCode);

    /**
     * Counts the study plans associated with a given career code.
     *
     * @param careerCode the unique code of the career
     * @return count of study plans
     */
    long countByCareer_Code(String careerCode);

    /**
     * Checks if a study plan already exists for a career with the same code.
     *
     * @param careerCode the unique code of the career
     * @param code the code/version of the study plan
     * @return true if exists, false otherwise
     */
    boolean existsByCareer_CodeAndCode(String careerCode, String code);
}
