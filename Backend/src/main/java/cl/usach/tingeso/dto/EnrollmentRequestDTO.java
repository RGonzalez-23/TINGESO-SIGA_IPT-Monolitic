package cl.usach.tingeso.dto;

import jakarta.validation.constraints.NotEmpty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * EnrollmentRequestDTO represents the request to enroll in a set of sections.
 * Corresponds to Epic 6: Academic Enrollment.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EnrollmentRequestDTO {

    /**
     * Target student RUN.
     * Optional when called by an authenticated STUDENT (inferred from token).
     * Required when called by ADMIN.
     */
    private String studentRun;

    /**
     * List of section IDs to enroll in the current open academic period.
     */
    @NotEmpty(message = "At least one section must be selected")
    private List<Long> sectionIds;
}
