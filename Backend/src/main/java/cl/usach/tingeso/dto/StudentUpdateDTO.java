package cl.usach.tingeso.dto;

import cl.usach.tingeso.entity.AcademicStatus;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * StudentUpdateDTO represents the payload to update student information.
 * Corresponds to Epic 1: Student Management.
 * Business rules:
 * - Only ADMIN can modify academicStatus (only to REGULAR, POSTERGACION, or RETIRO_TEMPORAL).
 * - Career can only be modified if the student has no enrollments or academic history.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StudentUpdateDTO {

    /**
     * Updated first name (optional).
     */
    @Size(max = 100, message = "First name cannot exceed 100 characters")
    private String firstName;

    /**
     * Updated paternal last name (optional).
     */
    @Size(max = 100, message = "Paternal last name cannot exceed 100 characters")
    private String paternalLastName;

    /**
     * Updated maternal last name (optional).
     */
    @Size(max = 100, message = "Maternal last name cannot exceed 100 characters")
    private String maternalLastName;

    /**
     * Updated career code. Only permitted if no enrollments or history exist.
     */
    private String careerCode;

    /**
     * Updated academic status.
     * Only REGULAR, POSTERGACION, or RETIRO_TEMPORAL are permitted manually.
     */
    private AcademicStatus academicStatus;
}
