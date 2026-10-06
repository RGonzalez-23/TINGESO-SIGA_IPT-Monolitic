package cl.usach.tingeso.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * CareerResponseDTO represents a career program returned by the API.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CareerResponseDTO {
    private String code;
    private String name;
    private String description;
    private Integer durationSemesters;
    private Boolean isActive;
    private String activeStudyPlanCode;
    private Long totalStudyPlans;
    private Long totalEnrolledStudents;
}
