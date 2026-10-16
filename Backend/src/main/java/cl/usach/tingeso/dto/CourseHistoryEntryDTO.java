package cl.usach.tingeso.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * CourseHistoryEntryDTO represents a single course enrollment in a student's academic history.
 * Corresponds to Epic 7: Final Grades Registration and Semester Close.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CourseHistoryEntryDTO {

    private Long enrollmentId;
    private Long courseId;
    private String courseCode;
    private String courseName;
    private Integer semester;
    private Integer sctCredits;
    private String sectionCode;
    private String teacherName;
    private Double finalGrade;
    private String status;
}
