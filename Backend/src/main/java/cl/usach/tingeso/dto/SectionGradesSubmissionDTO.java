package cl.usach.tingeso.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * SectionGradesSubmissionDTO encapsulates a batch of grade submissions for a section.
 * Corresponds to Epic 7: Final Grades Registration and Semester Close.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SectionGradesSubmissionDTO {

    @NotEmpty(message = "Grades list cannot be empty.")
    @Valid
    private List<StudentGradeEntryDTO> grades;
}
