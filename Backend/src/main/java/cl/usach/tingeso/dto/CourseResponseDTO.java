package cl.usach.tingeso.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

/**
 * CourseResponseDTO represents subject details returned by the API.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CourseResponseDTO {
    private Long id;
    private String code;
    private String name;
    private Integer semester;
    private Integer theoryHours;
    private Integer exerciseHours;
    private Integer laboratoryHours;
    private Integer totalHours;
    private String tel;
    private Integer sctCredits;
    private Long studyPlanId;
    private String studyPlanCode;
    private String careerCode;
    private String careerName;
    @Builder.Default
    private List<CoursePrerequisiteDTO> prerequisites = new ArrayList<>();
    @Builder.Default
    private List<String> prerequisiteCodes = new ArrayList<>();
}
