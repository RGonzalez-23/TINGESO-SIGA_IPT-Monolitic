package cl.usach.tingeso.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * CareerEntity represents an academic program offered by IPT.
 * Corresponds to Epic 2: Management of careers and study plans.
 */
@Entity
@Table(name = "careers")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CareerEntity {

    /**
     * Unique code of the career (e.g., "10450", "10451", "10452").
     */
    @Id
    @Column(name = "code", length = 20, nullable = false, unique = true)
    private String code;

    /**
     * Official name of the career (e.g., "Tecnico Analista Programador").
     */
    @Column(name = "name", length = 150, nullable = false)
    private String name;

    /**
     * Detailed description of the career profile and objectives.
     */
    @Column(name = "description", length = 500, nullable = false)
    private String description;

    /**
     * Duration in semesters (always 4 semesters for 2-year technical programs).
     */
    @Builder.Default
    @Column(name = "duration_semesters", nullable = false)
    private Integer durationSemesters = 4;

    /**
     * Status of the career: true indicates ACTIVE, false indicates INACTIVE.
     * An inactive career cannot admit new students.
     */
    @Builder.Default
    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;
}
