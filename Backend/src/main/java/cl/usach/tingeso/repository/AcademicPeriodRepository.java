package cl.usach.tingeso.repository;

import cl.usach.tingeso.entity.AcademicPeriodEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * AcademicPeriodRepository handles database operations for AcademicPeriodEntity.
 * Layer: Repository.
 */
@Repository
public interface AcademicPeriodRepository extends JpaRepository<AcademicPeriodEntity, String> {

    /**
     * Checks if a period exists for the given year and semester.
     */
    boolean existsByYearAndSemester(Integer year, Integer semester);

    /**
     * Finds all periods ordered chronologically descending (newest first).
     */
    List<AcademicPeriodEntity> findAllByOrderByYearDescSemesterDesc();

    /**
     * Finds all currently open academic periods.
     */
    List<AcademicPeriodEntity> findAllByIsOpenTrue();

    /**
     * Finds the currently open academic period.
     */
    java.util.Optional<AcademicPeriodEntity> findFirstByIsOpenTrue();

    /**
     * Finds the single currently open academic period.
     */
    default java.util.Optional<AcademicPeriodEntity> findByIsOpenTrue() {
        return findFirstByIsOpenTrue();
    }
}
