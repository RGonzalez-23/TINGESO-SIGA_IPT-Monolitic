package cl.usach.tingeso.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * StudyPlanEntity represents the curricular structure of a career over time.
 * Corresponds to Epic 2: Management of careers and study plans.
 * Only one study plan can be active (vigente) for a given career at any time.
 */
@Entity
@Table(name = "study_plans")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StudyPlanEntity {

    /**
     * Unique identifier for the study plan.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Code or version of the study plan (e.g., "2021.3", "2022.3").
     */
    @Column(name = "code", length = 50, nullable = false)
    private String code;

    /**
     * Indicates whether this plan is the currently active (vigente) plan for its career.
     * When a new plan is activated, the previous one becomes inactive.
     */
    @Column(name = "is_active", nullable = false)
    private Boolean isActive;

    /**
     * The career to which this study plan belongs.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "career_code", nullable = false)
    private CareerEntity career;
}
