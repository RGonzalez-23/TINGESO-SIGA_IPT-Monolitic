package cl.usach.tingeso.repository;

import cl.usach.tingeso.entity.AcademicStatus;
import cl.usach.tingeso.entity.StudentEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * StudentRepository provides persistence operations for StudentEntity.
 * Layer: Repository.
 */
@Repository
public interface StudentRepository extends JpaRepository<StudentEntity, String> {

    /**
     * Finds a student by their unique RUN.
     *
     * @param run National identification number with hyphen (e.g., "12345678-9")
     * @return Optional containing the student if found
     */
    Optional<StudentEntity> findByRun(String run);

    /**
     * Checks if a student already exists with the given RUN.
     *
     * @param run RUN to verify
     * @return true if exists, false otherwise
     */
    boolean existsByRun(String run);

    /**
     * Checks if a student already exists with the given email.
     *
     * @param email Institutional email to verify
     * @return true if exists, false otherwise
     */
    boolean existsByEmail(String email);

    /**
     * Retrieves all students with a specific academic status.
     *
     * @param academicStatus the status to filter by (e.g., REGULAR, POSTERGACION, etc.)
     * @return list of matching students
     */
    List<StudentEntity> findByAcademicStatus(AcademicStatus academicStatus);

    /**
     * Retrieves all students belonging to a specific career code.
     *
     * @param careerCode the unique code of the career
     * @return list of matching students
     */
    List<StudentEntity> findByCareer_Code(String careerCode);
}
