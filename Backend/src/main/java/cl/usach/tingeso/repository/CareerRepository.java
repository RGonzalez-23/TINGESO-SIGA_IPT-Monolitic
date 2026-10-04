package cl.usach.tingeso.repository;

import cl.usach.tingeso.entity.CareerEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * CareerRepository provides persistence operations for CareerEntity.
 * Layer: Repository.
 */
@Repository
public interface CareerRepository extends JpaRepository<CareerEntity, String> {

    /**
     * Finds a career by its unique code.
     *
     * @param code unique code of the career
     * @return Optional containing the found career, or empty if not found
     */
    Optional<CareerEntity> findByCode(String code);

    /**
     * Checks if a career exists with the given code.
     *
     * @param code unique code to verify
     * @return true if exists, false otherwise
     */
    boolean existsByCode(String code);

    /**
     * Retrieves all careers currently marked as active.
     *
     * @return list of active careers
     */
    List<CareerEntity> findByIsActiveTrue();
}
