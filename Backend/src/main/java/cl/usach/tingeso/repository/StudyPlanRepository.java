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
     * Retrieves all study plans associated with a given career code.
     *
     * @param careerCode the unique code of the career
     * @return list of study plans
     */
    List<StudyPlanEntity> findByCareer_Code(String careerCode);
}
