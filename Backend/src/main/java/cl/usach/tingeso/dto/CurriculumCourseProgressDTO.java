package cl.usach.tingeso.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * CurriculumCourseProgressDTO represents a subject in a student's curriculum map with color-coded status.
 * Corresponds to Epic 7: Final Grades Registration and Semester Close.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CurriculumCourseProgressDTO {

    private Long courseId;
    private String code;
    private String name;
    private Integer semester;
    private Integer sctCredits;
    private Integer theoryHours;
    private Integer exerciseHours;
    private Integer laboratoryHours;

    /**
     * Visual status for curriculum map:
     * - "APPROVED": Verde
     * - "FAILED": Rojo
     * - "ENROLLED": Amarillo
     * - "PENDING": Blanco
     */
    private String status;

    /**
     * Number of times the student has failed this subject (0, 1, 2).
     */
    private Integer failCount;

    /**
     * Most recent final grade if available.
     */
    private Double latestGrade;

    /**
     * List of prerequisite course codes and names.
     */
    private List<CoursePrerequisiteDTO> prerequisites;
}
