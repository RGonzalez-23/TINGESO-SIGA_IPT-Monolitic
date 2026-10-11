package cl.usach.tingeso.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Set;

/**
 * SectionUpdateDTO for updating an existing course section.
 * Layer: DTO.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SectionUpdateDTO {

    @NotBlank(message = "El RUN del docente responsable es obligatorio")
    private String teacherRun;

    @NotNull(message = "La capacidad es obligatoria")
    @Min(value = 1, message = "La capacidad debe ser mayor a 0")
    @Max(value = 60, message = "La capacidad no puede superar los 60 estudiantes")
    private Integer capacity;

    @NotEmpty(message = "Debe asignar al menos un bloque horario")
    private Set<String> timeBlockCodes;
}
