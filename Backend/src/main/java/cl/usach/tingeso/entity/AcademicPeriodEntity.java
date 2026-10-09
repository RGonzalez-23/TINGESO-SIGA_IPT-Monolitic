package cl.usach.tingeso.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * AcademicPeriodEntity represents a regular academic semester (e.g., "2027-1", "2027-2").
 * Corresponds to Epic 5: Academic Offer and Sections Management.
 */
@Entity
@Table(
        name = "academic_periods",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_period_year_semester", columnNames = {"academic_year", "semester"})
        }
)
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AcademicPeriodEntity {

    /**
     * Canonical identifier in format "YYYY-S" (e.g., "2027-1").
     */
    @Id
    @Column(name = "id", length = 10, nullable = false)
    private String id;

    /**
     * Academic year (e.g., 2027).
     */
    @Column(name = "academic_year", nullable = false)
    private Integer year;

    /**
     * Semester number within the year (1 or 2).
     */
    @Column(name = "semester", nullable = false)
    private Integer semester;

    /**
     * Status flag: true for ABIERTO (open), false for CERRADO (closed).
     * Newly created periods are always initially ABIERTO (true).
     */
    @Column(name = "is_open", nullable = false)
    @Builder.Default
    private Boolean isOpen = true;
}
