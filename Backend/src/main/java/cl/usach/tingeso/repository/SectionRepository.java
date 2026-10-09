package cl.usach.tingeso.repository;

import cl.usach.tingeso.entity.SectionEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * SectionRepository handles database operations for SectionEntity.
 * Layer: Repository.
 */
@Repository
public interface SectionRepository extends JpaRepository<SectionEntity, Long> {

    /**
     * Checks if a section already exists for a specific course within an academic period.
     */
    boolean existsByCourseIdAndAcademicPeriodId(Long courseId, String academicPeriodId);

    /**
     * Finds the single section for a course in a given period.
     */
    Optional<SectionEntity> findByCourseIdAndAcademicPeriodId(Long courseId, String academicPeriodId);

    /**
     * Retrieves all sections for a given academic period.
     */
    List<SectionEntity> findByAcademicPeriodId(String academicPeriodId);

    /**
     * Retrieves all sections for a given academic period and study plan.
     */
    List<SectionEntity> findByAcademicPeriodIdAndCourseStudyPlanId(String academicPeriodId, Long studyPlanId);

    /**
     * Retrieves all sections assigned to a teacher within an academic period.
     * Essential for teacher timetable conflict detection.
     */
    List<SectionEntity> findByTeacherRunAndAcademicPeriodId(String teacherRun, String academicPeriodId);

    /**
     * Checks if a teacher is assigned to any sections across the system.
     */
    boolean existsByTeacherRun(String teacherRun);

    /**
     * Checks if a teacher has active sections in currently open academic periods.
     */
    boolean existsByTeacherRunAndAcademicPeriodIsOpenTrue(String teacherRun);

    /**
     * Checks if any section exists for a given course.
     */
    boolean existsByCourseId(Long courseId);

    /**
     * Counts the total number of sections in an academic period.
     */
    long countByAcademicPeriodId(String academicPeriodId);

    /**
     * Eagerly loads a section with its time blocks and course details.
     */
    @Query("SELECT s FROM SectionEntity s LEFT JOIN FETCH s.timeBlocks WHERE s.id = :id")
    Optional<SectionEntity> findByIdWithTimeBlocks(@Param("id") Long id);
}
