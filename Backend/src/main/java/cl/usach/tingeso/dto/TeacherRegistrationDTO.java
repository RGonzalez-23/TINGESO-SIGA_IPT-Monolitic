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
 * TeacherRegistrationDTO encapsulates the data required to register a new teacher.
 * Layer: DTO.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TeacherRegistrationDTO {

    /**
     * Chilean National Identification Number (RUN/RUT) with hyphen (e.g. "11111111-1").
     */
    @NotBlank(message = "Teacher RUN is required")
    @Size(min = 8, max = 12, message = "RUN must be between 8 and 12 characters")
    private String run;

    /**
     * First name(s).
     */
    @NotBlank(message = "First name is required")
    @Size(max = 100, message = "First name must not exceed 100 characters")
    private String firstName;

    /**
     * Paternal last name.
     */
    @NotBlank(message = "Paternal last name is required")
    @Size(max = 100, message = "Paternal last name must not exceed 100 characters")
    private String paternalLastName;

    /**
     * Maternal last name.
     */
    @NotBlank(message = "Maternal last name is required")
    @Size(max = 100, message = "Maternal last name must not exceed 100 characters")
    private String maternalLastName;

    /**
     * Professional title / degree.
     */
    @NotBlank(message = "Professional title is required")
    @Size(max = 150, message = "Professional title must not exceed 150 characters")
    private String professionalTitle;

    /**
     * Highest academic degree.
     */
    @NotNull(message = "Academic degree is required (LICENCIATURA, MAGISTER, DOCTORADO)")
    private AcademicDegree academicDegree;
}
