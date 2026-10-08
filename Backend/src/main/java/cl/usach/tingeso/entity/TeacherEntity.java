package cl.usach.tingeso.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * TeacherEntity represents a faculty member/instructor at IPT.
 * Corresponds to Epic 4: Teacher Management.
 * Layer: Entity.
 */
@Entity
@Table(name = "teachers")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TeacherEntity {

    /**
     * Unique National Identification Number (RUN/RUT) with hyphen (e.g., "11111111-1").
     * Serves as primary key and identity link with Keycloak preferred_username.
     */
    @Id
    @Column(name = "run", length = 12, nullable = false, unique = true)
    private String run;

    /**
     * First and middle names of the teacher.
     */
    @Column(name = "first_name", length = 100, nullable = false)
    private String firstName;

    /**
     * Paternal last name of the teacher.
     */
    @Column(name = "paternal_last_name", length = 100, nullable = false)
    private String paternalLastName;

    /**
     * Maternal last name of the teacher.
     */
    @Column(name = "maternal_last_name", length = 100, nullable = false)
    private String maternalLastName;

    /**
     * Institutional email address (e.g., "first.paternal@sigaipt.cl").
     * Must be unique within the system (across both students and teachers).
     */
    @Column(name = "email", length = 150, nullable = false, unique = true)
    private String email;

    /**
     * Professional degree / title (e.g., "Ingeniero Civil en Informática").
     */
    @Column(name = "professional_title", length = 150, nullable = false)
    private String professionalTitle;

    /**
     * Highest academic degree (Licenciatura, Magíster, Doctorado).
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "academic_degree", length = 50, nullable = false)
    private AcademicDegree academicDegree;

    /**
     * Teacher active status (ACTIVO / INACTIVO).
     * Newly registered teachers are always initialized as true (ACTIVO).
     */
    @Builder.Default
    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;

    /**
     * Returns full formatted name.
     *
     * @return full name as "firstName paternalLastName maternalLastName"
     */
    public String getFullName() {
        return String.format("%s %s %s",
                firstName != null ? firstName.trim() : "",
                paternalLastName != null ? paternalLastName.trim() : "",
                maternalLastName != null ? maternalLastName.trim() : "").trim();
    }
}
