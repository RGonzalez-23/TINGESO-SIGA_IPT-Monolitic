package cl.usach.tingeso.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * StudyPlanRegistrationDTO captures request data to register a new study plan for a career.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StudyPlanRegistrationDTO {

    @NotBlank(message = "Study plan code is mandatory")
    @Size(max = 50, message = "Study plan code cannot exceed 50 characters")
    private String code;

    @NotBlank(message = "Career code is mandatory")
    @Size(max = 20, message = "Career code cannot exceed 20 characters")
    private String careerCode;

    /**
     * Whether this newly registered plan should immediately become the active plan.
     * Defaults to true if not specified.
     */
    @Builder.Default
    private Boolean isActive = true;
}
