package cl.usach.tingeso.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * PeriodHistoryDTO groups courses taken by a student in a specific academic period.
 * Corresponds to Epic 7: Final Grades Registration and Semester Close.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PeriodHistoryDTO {

    private String periodId;
    private Integer year;
    private Integer semester;
    private Boolean isOpen;
    private Double periodGpa;
    private Integer enrolledCredits;
    private Integer approvedCredits;
    private List<CourseHistoryEntryDTO> courses;
}
