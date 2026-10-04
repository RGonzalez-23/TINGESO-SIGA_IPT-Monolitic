package cl.usach.tingeso.dto;

import cl.usach.tingeso.entity.AcademicStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * StudentResponseDTO represents the data returned by the API for student queries.
 * Presentation logic like formatted full name is computed here to keep entities pure.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StudentResponseDTO {

    private String run;
    private String firstName;
    private String paternalLastName;
    private String maternalLastName;
    private String fullName;
    private String email;
    private AcademicStatus academicStatus;
    private String careerCode;
    private String careerName;
    private String studyPlanCode;
}
