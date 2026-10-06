package cl.usach.tingeso.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * StudyPlanResponseDTO represents study plan details returned by the API.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StudyPlanResponseDTO {
    private Long id;
    private String code;
    private Boolean isActive;
    private String careerCode;
    private String careerName;
    private Long enrolledStudentsCount;
}
