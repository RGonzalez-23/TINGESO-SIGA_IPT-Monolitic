package cl.usach.tingeso.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * PeriodCloseValidationDTO provides an audit of whether an academic period is ready to close.
 * Corresponds to Epic 7: Final Grades Registration and Semester Close.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PeriodCloseValidationDTO {

    private String periodId;
    private Boolean isOpen;
    private Boolean canClose;
    private Integer totalActiveEnrollments;
    private Integer gradedEnrollmentsCount;
    private Integer pendingEnrollmentsCount;
    private List<IncompleteSectionDTO> incompleteSections;
}
