package cl.usach.tingeso.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * StudentEntity represents a student enrolled at IPT.
 * Corresponds to Epic 1: Student Management.
 */
@Entity
@Table(name = "students")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StudentEntity {

    /**
     * Unique National Identification Number (RUN/RUT) with hyphen (e.g., "12345678-9").
     * Serves as the primary key and identity link with Keycloak.
     */
    @Id
    @Column(name = "run", length = 12, nullable = false, unique = true)
    private String run;

    /**
     * First and middle names of the student.
     */
    @Column(name = "first_name", length = 100, nullable = false)
    private String firstName;

    /**
     * Paternal last name of the student.
     */
    @Column(name = "paternal_last_name", length = 100, nullable = false)
    private String paternalLastName;

    /**
     * Maternal last name of the student.
     */
    @Column(name = "maternal_last_name", length = 100, nullable = false)
    private String maternalLastName;

    /**
     * Institutional email address (e.g., "first.paternal@sigaipt.cl").
     * Must be unique within the system.
     */
    @Column(name = "email", length = 150, nullable = false, unique = true)
    private String email;

    /**
     * Academic status representing the student's current situation in the institution.
     * Default value is REGULAR upon registration.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "academic_status", length = 30, nullable = false)
    @Builder.Default
    private AcademicStatus academicStatus = AcademicStatus.REGULAR;

    /**
     * The career in which the student is enrolled.
     * Cannot be changed if the student has enrollments or academic history.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "career_code", nullable = false)
    private CareerEntity career;

    /**
     * The specific study plan assigned to the student (the active plan at registration time).
     * Cannot be changed if the student has enrollments or academic history.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "study_plan_id", nullable = false)
    private StudyPlanEntity studyPlan;
}
