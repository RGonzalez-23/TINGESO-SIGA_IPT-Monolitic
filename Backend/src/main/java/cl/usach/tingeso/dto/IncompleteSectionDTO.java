package cl.usach.tingeso.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * IncompleteSectionDTO reports a section that still has ungraded enrollments before period close.
 * Corresponds to Epic 7: Final Grades Registration and Semester Close.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class IncompleteSectionDTO {

    private Long sectionId;
    private String sectionCode;
    private String courseCode;
    private String courseName;
    private String teacherRun;
    private String teacherName;
    private Integer pendingGradesCount;
    private Integer totalEnrolledCount;
}
