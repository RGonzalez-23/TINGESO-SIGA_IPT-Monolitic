package cl.usach.tingeso.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * EnrolledStudentDTO represents a student enrolled in a teacher's section roster.
 * Corresponds to Epic 6: Teacher section roster view.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EnrolledStudentDTO {

    private String run;
    private String firstName;
    private String paternalLastName;
    private String maternalLastName;
    private String fullName;
    private String email;
    private String careerName;
    private LocalDateTime enrollmentDate;
}
