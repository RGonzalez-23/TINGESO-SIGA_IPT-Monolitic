package cl.usach.tingeso.dto;

import cl.usach.tingeso.entity.AcademicDegree;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * TeacherUpdateDTO encapsulates the data allowed to update an existing teacher.
 * Layer: DTO.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TeacherUpdateDTO {

    /**
     * Updated first name(s).
     */
    @NotBlank(message = "First name is required")
    @Size(max = 100, message = "First name must not exceed 100 characters")
    private String firstName;

    /**
     * Updated paternal last name.
     */
    @NotBlank(message = "Paternal last name is required")
    @Size(max = 100, message = "Paternal last name must not exceed 100 characters")
    private String paternalLastName;

    /**
     * Updated maternal last name.
     */
    @NotBlank(message = "Maternal last name is required")
    @Size(max = 100, message = "Maternal last name must not exceed 100 characters")
    private String maternalLastName;

    /**
     * Updated professional title.
     */
    @NotBlank(message = "Professional title is required")
    @Size(max = 150, message = "Professional title must not exceed 150 characters")
    private String professionalTitle;

    /**
     * Updated academic degree.
     */
    @NotNull(message = "Academic degree is required (LICENCIATURA, MAGISTER, DOCTORADO)")
    private AcademicDegree academicDegree;

    /**
     * Active status flag.
     * Cannot be set to false if the teacher has assigned sections in an OPEN period.
     */
    @NotNull(message = "Active status is required")
    private Boolean isActive;
}
