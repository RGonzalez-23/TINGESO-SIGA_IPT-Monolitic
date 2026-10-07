package cl.usach.tingeso.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.util.HashSet;
import java.util.Set;

/**
 * CourseEntity represents an academic subject (asignatura) belonging to a specific study plan.
 * Corresponds to Epic 3: Management of subjects and prerequisites.
 */
@Entity
@Table(
        name = "courses",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_course_plan_code", columnNames = {"study_plan_id", "code"})
        }
)
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CourseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Unique code of the course within its study plan (e.g., "TAP101", "MAT102").
     */
    @Column(name = "code", length = 30, nullable = false)
    private String code;

    /**
     * Official name of the subject.
     */
    @Column(name = "name", length = 150, nullable = false)
    private String name;

    /**
     * Academic semester in the curriculum (1, 2, 3, or 4).
     */
    @Column(name = "semester", nullable = false)
    private Integer semester;

    /**
     * Weekly Theory hours (T). Must be >= 0.
     */
    @Column(name = "theory_hours", nullable = false)
    private Integer theoryHours;

    /**
     * Weekly Exercise hours (E). Must be >= 0.
     */
    @Column(name = "exercise_hours", nullable = false)
    private Integer exerciseHours;

    /**
     * Weekly Laboratory hours (L). Must be >= 0.
     */
    @Column(name = "laboratory_hours", nullable = false)
    private Integer laboratoryHours;

    /**
     * Academic credits assigned (SCT). Must be between 1 and 7.
     */
    @Column(name = "sct_credits", nullable = false)
    private Integer sctCredits;

    /**
     * The study plan to which this course belongs.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "study_plan_id", nullable = false)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private StudyPlanEntity studyPlan;

    /**
     * Prerequisites required to take this course (0 to 3 courses from previous semesters in the same plan).
     */
    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "course_prerequisites",
            joinColumns = @JoinColumn(name = "course_id"),
            inverseJoinColumns = @JoinColumn(name = "prerequisite_id")
    )
    @Builder.Default
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private Set<CourseEntity> prerequisites = new HashSet<>();
}
