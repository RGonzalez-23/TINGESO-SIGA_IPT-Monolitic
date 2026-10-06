package cl.usach.tingeso.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * CareerUpdateDTO captures request data to update an existing career's mutable information.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CareerUpdateDTO {

    @NotBlank(message = "Career name is mandatory")
    @Size(max = 150, message = "Career name cannot exceed 150 characters")
    private String name;

    @NotBlank(message = "Career description is mandatory")
    @Size(max = 500, message = "Career description cannot exceed 500 characters")
    private String description;

    @NotNull(message = "Active status is mandatory")
    private Boolean isActive;
}
