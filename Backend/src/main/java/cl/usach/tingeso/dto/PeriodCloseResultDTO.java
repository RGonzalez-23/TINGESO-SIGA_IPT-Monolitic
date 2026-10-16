package cl.usach.tingeso.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * PeriodCloseResultDTO summarizes the outcome of closing an academic period.
 * Corresponds to Epic 7: Final Grades Registration and Semester Close.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PeriodCloseResultDTO {

    private String periodId;
    private String message;
    private Integer totalProcessedEnrollments;
    private Integer approvedCount;
    private Integer failedCount;
    private Integer graduatedStudentsCount;
    private List<String> graduatedStudentRuns;
    private Integer dismissedStudentsCount;
    private List<String> dismissedStudentRuns;
}
