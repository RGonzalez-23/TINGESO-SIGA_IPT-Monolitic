package cl.usach.tingeso.repository;

import cl.usach.tingeso.entity.EnrollmentEntity;
import cl.usach.tingeso.entity.EnrollmentStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * EnrollmentRepository manages data persistence and queries for EnrollmentEntity.
 * Corresponds to Epic 6: Academic Enrollment.
 */
@Repository
public interface EnrollmentRepository extends JpaRepository<EnrollmentEntity, Long> {

    List<EnrollmentEntity> findByStudent_Run(String run);

    List<EnrollmentEntity> findByStudent_RunAndStatus(String run, EnrollmentStatus status);

    List<EnrollmentEntity> findByStudent_RunAndAcademicPeriod_IdAndStatus(String run, String periodId, EnrollmentStatus status);

    List<EnrollmentEntity> findBySection_IdAndStatus(Long sectionId, EnrollmentStatus status);

    List<EnrollmentEntity> findBySection_Id(Long sectionId);

    boolean existsByStudent_RunAndCourse_CodeAndStatus(String run, String courseCode, EnrollmentStatus status);

    boolean existsByStudent_RunAndCourse_CodeAndAcademicPeriod_IdAndStatus(String run, String courseCode, String periodId, EnrollmentStatus status);

    boolean existsByStudent_Run(String run);

    long countByStudent_RunAndAcademicPeriod_IdAndStatus(String run, String periodId, EnrollmentStatus status);

    List<EnrollmentEntity> findByAcademicPeriod_IdAndStatus(String periodId, EnrollmentStatus status);

    List<EnrollmentEntity> findByAcademicPeriod_Id(String periodId);

    long countByAcademicPeriod_IdAndStatusAndFinalGradeIsNull(String periodId, EnrollmentStatus status);

    long countByStudent_RunAndCourse_IdAndStatus(String run, Long courseId, EnrollmentStatus status);

    List<EnrollmentEntity> findByStudent_RunOrderByAcademicPeriod_IdAscEnrollmentDateAsc(String run);

    List<EnrollmentEntity> findByStudent_RunAndCourse_Id(String run, Long courseId);
}
