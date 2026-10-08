package cl.usach.tingeso.repository;

import cl.usach.tingeso.entity.TeacherEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * TeacherRepository provides persistence operations for TeacherEntity.
 * Layer: Repository.
 */
@Repository
public interface TeacherRepository extends JpaRepository<TeacherEntity, String> {

    /**
     * Checks if a teacher exists with the specified RUN.
     *
     * @param run RUN with hyphen
     * @return true if exists, false otherwise
     */
    boolean existsByRun(String run);

    /**
     * Checks if a teacher exists with the specified email.
     *
     * @param email institutional email
     * @return true if exists, false otherwise
     */
    boolean existsByEmail(String email);

    /**
     * Finds a teacher by RUN.
     *
     * @param run RUN with hyphen
     * @return Optional of TeacherEntity
     */
    Optional<TeacherEntity> findByRun(String run);

    /**
     * Retrieves all teachers filtered by active status.
     *
     * @param isActive active status
     * @return list of teachers
     */
    List<TeacherEntity> findByIsActiveOrderByPaternalLastNameAscFirstNameAsc(Boolean isActive);

    /**
     * Retrieves all teachers ordered by paternal last name and first name.
     *
     * @return list of all teachers ordered
     */
    List<TeacherEntity> findAllByOrderByPaternalLastNameAscFirstNameAsc();
}
