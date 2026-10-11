package cl.usach.tingeso.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * SectionResponseDTO representing complete information of a course section.
 * Includes format "[COURSE_CODE]-A-1" in sectionCode.
 * Layer: DTO.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SectionResponseDTO {

    private Long id;
    private String sectionCode;

    // Course info
    private Long courseId;
    private String courseCode;
    private String courseName;
    private Integer semester;
    private Integer sctCredits;
    private Integer theoryHours;
    private Integer exerciseHours;
    private Integer laboratoryHours;
    private Integer totalTELHours;
    private Integer requiredBlocksCount;

    // Plan & Career info
    private Long studyPlanId;
    private String studyPlanCode;
    private String careerCode;
    private String careerName;

    // Academic Period info
    private String academicPeriodId;
    private Boolean academicPeriodOpen;

    // Teacher info
    private String teacherRun;
    private String teacherName;

    // Quotas
    private Integer capacity;
    private Integer enrolledCount;
    private Integer availableQuota;

    // Schedule
    private List<TimeBlockResponseDTO> timeBlocks;
    private String scheduleSummary;
}
