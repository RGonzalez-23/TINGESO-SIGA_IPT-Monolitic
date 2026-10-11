package cl.usach.tingeso.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * AcademicPeriodCreateDTO for registering a new academic period.
 * Layer: DTO.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AcademicPeriodCreateDTO {

    @NotNull(message = "El año académico es obligatorio")
    @Min(value = 2000, message = "El año debe ser mayor o igual a 2000")
    @Max(value = 2100, message = "El año debe ser menor o igual a 2100")
    private Integer year;

    @NotNull(message = "El semestre es obligatorio")
    @Min(value = 1, message = "El semestre debe ser 1 o 2")
    @Max(value = 2, message = "El semestre debe ser 1 o 2")
    private Integer semester;
}
