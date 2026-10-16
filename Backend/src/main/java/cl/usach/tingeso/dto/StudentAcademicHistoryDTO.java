package cl.usach.tingeso.dto;

import cl.usach.tingeso.entity.AcademicStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * StudentAcademicHistoryDTO provides a complete historical transcript of a student's academic progress.
 * Corresponds to Epic 7: Final Grades Registration and Semester Close.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StudentAcademicHistoryDTO {

    private String studentRun;
    private String studentFullName;
    private String careerCode;
    private String careerName;
    private String studyPlanCode;
    private AcademicStatus academicStatus;
    private Boolean isMatriculated;
    private Double cumulativeGpa;
    private Integer totalApprovedCredits;
    private Integer totalPlanCredits;
    private Integer totalApprovedCourses;
    private Integer totalPlanCourses;
    private List<PeriodHistoryDTO> periods;
}
