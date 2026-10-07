package cl.usach.tingeso.repository;

import cl.usach.tingeso.entity.CourseEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * CourseRepository provides persistence operations for CourseEntity.
 * Layer: Repository.
 */
@Repository
public interface CourseRepository extends JpaRepository<CourseEntity, Long> {

    /**
     * Retrieves all courses belonging to a specific study plan, ordered by semester and code.
     *
     * @param studyPlanId ID of the study plan
     * @return list of courses
     */
    List<CourseEntity> findByStudyPlan_IdOrderBySemesterAscCodeAsc(Long studyPlanId);

    /**
     * Retrieves courses for a specific study plan and semester.
     *
     * @param studyPlanId ID of the study plan
     * @param semester semester number (1 to 4)
     * @return list of courses
     */
    List<CourseEntity> findByStudyPlan_IdAndSemesterOrderByCodeAsc(Long studyPlanId, Integer semester);

    /**
     * Finds a course by study plan ID and unique course code.
     *
     * @param studyPlanId ID of the study plan
     * @param code course code (e.g., "TAP101")
     * @return Optional containing the course if found
     */
    Optional<CourseEntity> findByStudyPlan_IdAndCode(Long studyPlanId, String code);

    /**
     * Checks if a course with the given code already exists within a study plan.
     *
     * @param studyPlanId ID of the study plan
     * @param code course code to verify
     * @return true if exists, false otherwise
     */
    boolean existsByStudyPlan_IdAndCode(Long studyPlanId, String code);

    /**
     * Counts how many courses have the given course listed as a prerequisite.
     *
     * @param course prerequisite course
     * @return count of dependent courses
     */
    long countByPrerequisitesContaining(CourseEntity course);
}
