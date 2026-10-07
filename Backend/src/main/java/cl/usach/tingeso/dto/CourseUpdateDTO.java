package cl.usach.tingeso.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

/**
 * CourseUpdateDTO captures request data to update an existing course's details and prerequisites.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CourseUpdateDTO {

    @NotBlank(message = "Course name is mandatory")
    @Size(max = 150, message = "Course name cannot exceed 150 characters")
    private String name;

    @NotNull(message = "Semester is mandatory")
    @Min(value = 1, message = "Semester must be between 1 and 4")
    @Max(value = 4, message = "Semester must be between 1 and 4")
    private Integer semester;

    @NotNull(message = "Theory hours are mandatory")
    @Min(value = 0, message = "Theory hours cannot be negative")
    private Integer theoryHours;

    @NotNull(message = "Exercise hours are mandatory")
    @Min(value = 0, message = "Exercise hours cannot be negative")
    private Integer exerciseHours;

    @NotNull(message = "Laboratory hours are mandatory")
    @Min(value = 0, message = "Laboratory hours cannot be negative")
    private Integer laboratoryHours;

    @NotNull(message = "SCT credits are mandatory")
    @Min(value = 1, message = "SCT credits must be between 1 and 7")
    @Max(value = 7, message = "SCT credits must be between 1 and 7")
    private Integer sctCredits;

    @Builder.Default
    private List<String> prerequisiteCodes = new ArrayList<>();
}
