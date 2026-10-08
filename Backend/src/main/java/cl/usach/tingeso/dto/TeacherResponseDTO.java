package cl.usach.tingeso.dto;

import cl.usach.tingeso.entity.AcademicDegree;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * TeacherResponseDTO encapsulates teacher details returned to the frontend.
 * Layer: DTO.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TeacherResponseDTO {

    private String run;
    private String firstName;
    private String paternalLastName;
    private String maternalLastName;
    private String fullName;
    private String email;
    private String professionalTitle;
    private AcademicDegree academicDegree;
    private String academicDegreeDisplayName;
    private Boolean isActive;
}
