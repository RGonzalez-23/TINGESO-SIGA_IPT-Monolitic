package cl.usach.tingeso.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
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
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.time.LocalDateTime;

/**
 * EnrollmentEntity represents a student's course enrollment in a section for an academic period.
 * Corresponds to Epic 6: Academic Enrollment.
 */
@Entity
@Table(name = "enrollments")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EnrollmentEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Enrolled student.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "student_run", nullable = false)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private StudentEntity student;

    /**
     * Enrolled course / subject.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "course_id", nullable = false)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private CourseEntity course;

    /**
     * Assigned section for the course in this period.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "section_id", nullable = false)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private SectionEntity section;

    /**
     * Academic period in which this enrollment takes place (e.g., "2027-1").
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "academic_period_id", nullable = false)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private AcademicPeriodEntity academicPeriod;

    /**
     * Timestamp when the enrollment was formally submitted.
     */
    @Column(name = "enrollment_date", nullable = false)
    private LocalDateTime enrollmentDate;

    /**
     * Timestamp when the enrollment was cancelled/anulada (null if active).
     */
    @Column(name = "cancellation_date")
    private LocalDateTime cancellationDate;

    /**
     * Operational and academic status (INSCRITA, APROBADA, REPROBADA, ANULADA).
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 30, nullable = false)
    @Builder.Default
    private EnrollmentStatus status = EnrollmentStatus.INSCRITA;

    /**
     * Final evaluation grade (1.0 to 7.0) if completed; null while in progress.
     */
    @Column(name = "final_grade")
    private Double finalGrade;
}
