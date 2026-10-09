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
 * SectionEntity represents a single course section offered during an academic period.
 * Each course can have at most ONE section per academic period.
 * Format for section code: "[COURSE_CODE]-A-1" (e.g. "TAP101-A-1").
 * Corresponds to Epic 5: Academic Offer and Sections Management.
 */
@Entity
@Table(
        name = "sections",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_section_course_period", columnNames = {"course_id", "academic_period_id"})
        }
)
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SectionEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Nomenclatura oficial de la sección (e.g., "TAP101-A-1", "TCS204-A-1").
     */
    @Column(name = "section_code", length = 50, nullable = false)
    private String sectionCode;

    /**
     * The subject/course being offered in this section.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "course_id", nullable = false)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private CourseEntity course;

    /**
     * Academic period for this section (e.g., "2027-1").
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "academic_period_id", nullable = false)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private AcademicPeriodEntity academicPeriod;

    /**
     * Faculty member in charge of this section. Must be in ACTIVE status.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "teacher_run", nullable = false)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private TeacherEntity teacher;

    /**
     * Maximum seating capacity (1 <= capacity <= 60).
     */
    @Column(name = "capacity", nullable = false)
    private Integer capacity;

    /**
     * Number of enrolled students (0 <= enrolledCount <= capacity).
     */
    @Column(name = "enrolled_count", nullable = false)
    @Builder.Default
    private Integer enrolledCount = 0;

    /**
     * Assigned schedule time blocks.
     * Count must match total TEL weekly hours / 2.
     */
    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "section_time_blocks",
            joinColumns = @JoinColumn(name = "section_id"),
            inverseJoinColumns = @JoinColumn(name = "time_block_code")
    )
    @Builder.Default
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private Set<TimeBlockEntity> timeBlocks = new HashSet<>();
}
