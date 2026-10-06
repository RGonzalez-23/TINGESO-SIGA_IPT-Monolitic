package cl.usach.tingeso.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * CareerRegistrationDTO captures request data to register a new academic career
 * along with its initial active study plan.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CareerRegistrationDTO {

    @NotBlank(message = "Career code is mandatory")
    @Size(max = 20, message = "Career code cannot exceed 20 characters")
    private String code;

    @NotBlank(message = "Career name is mandatory")
    @Size(max = 150, message = "Career name cannot exceed 150 characters")
    private String name;

    @NotBlank(message = "Career description is mandatory")
    @Size(max = 500, message = "Career description cannot exceed 500 characters")
    private String description;

    @NotNull(message = "Duration in semesters is mandatory")
    @Min(value = 1, message = "Duration must be at least 1 semester")
    @Builder.Default
    private Integer durationSemesters = 4;

    @NotBlank(message = "Initial study plan code is mandatory")
    @Size(max = 50, message = "Study plan code cannot exceed 50 characters")
    private String initialPlanCode;
}
