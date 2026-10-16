package cl.usach.tingeso.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * StudentGradeEntryDTO represents a grade submission or display entry for an enrolled student.
 * Corresponds to Epic 7: Final Grades Registration and Semester Close.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StudentGradeEntryDTO {

    @NotNull(message = "Enrollment ID cannot be null.")
    private Long enrollmentId;

    private String studentRun;

    private String studentName;

    @NotNull(message = "Final grade cannot be null.")
    @DecimalMin(value = "1.0", message = "Final grade must be greater than or equal to 1.0.")
    @DecimalMax(value = "7.0", message = "Final grade must be less than or equal to 7.0.")
    private Double finalGrade;

    private String status;
}
