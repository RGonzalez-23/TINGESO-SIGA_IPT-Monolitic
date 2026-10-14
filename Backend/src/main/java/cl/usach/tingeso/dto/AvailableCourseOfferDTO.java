package cl.usach.tingeso.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * AvailableCourseOfferDTO represents the detailed offer of a course for student enrollment.
 * Includes course metadata, section information, schedule blocks, quota, and eligibility flags.
 * Corresponds to Epic 6: Academic Enrollment.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AvailableCourseOfferDTO {

    private Long courseId;
    private String courseCode;
    private String courseName;
    private Integer semester;
    private Integer sctCredits;
    private Integer theoryHours;
    private Integer exerciseHours;
    private Integer laboratoryHours;
    private Integer totalTelHours;
    private List<String> prerequisiteCodes;
    private List<String> prerequisiteNames;

    // Section information in open academic period
    private Long sectionId;
    private String sectionCode;
    private String teacherRun;
    private String teacherName;
    private Integer capacity;
    private Integer enrolledCount;
    private Integer availableQuota;
    private List<String> timeBlocks;

    // Student eligibility checks
    private Boolean isAlreadyApproved;
    private Boolean isCurrentlyEnrolled;
    private Boolean isPrerequisitesMet;
    private Boolean isOffered;
    private Boolean isSelectable;
    private String ineligibilityReason;
}
