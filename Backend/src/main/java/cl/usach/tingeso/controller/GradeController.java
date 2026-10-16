package cl.usach.tingeso.controller;

import cl.usach.tingeso.dto.SectionGradesSubmissionDTO;
import cl.usach.tingeso.dto.StudentGradeEntryDTO;
import cl.usach.tingeso.exception.BusinessRuleException;
import cl.usach.tingeso.exception.ResourceNotFoundException;
import cl.usach.tingeso.service.GradeService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * GradeController exposes REST endpoints for registering and viewing final grades.
 * Corresponds to Epic 7: Final Grades Registration and Semester Close.
 */
@RestController
@RequestMapping("/api/sections/{sectionId}/grades")
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
@Slf4j
public class GradeController {

    private final GradeService gradeService;

    /**
     * Retrieves the current grade roster for a course section.
     * Accessible by the assigned TEACHER or an ADMIN.
     */
    @GetMapping
    @PreAuthorize("hasAnyRole('TEACHER', 'ADMIN')")
    public ResponseEntity<?> getSectionGrades(
            @PathVariable Long sectionId,
            Authentication authentication
    ) {
        try {
            boolean isAdmin = authentication.getAuthorities().stream()
                    .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
            String teacherRun = authentication.getName();

            List<StudentGradeEntryDTO> grades = gradeService.getSectionGrades(sectionId, teacherRun, isAdmin);
            return ResponseEntity.ok(grades);
        } catch (ResourceNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("message", e.getMessage()));
        } catch (BusinessRuleException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("message", e.getMessage()));
        } catch (Exception e) {
            log.error("Error retrieving section grades: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", "Error retrieving section grades."));
        }
    }

    /**
     * Registers or updates final grades for students in a course section.
     * Accessible only by the assigned TEACHER.
     */
    @PutMapping
    @PreAuthorize("hasRole('TEACHER')")
    public ResponseEntity<?> submitSectionGrades(
            @PathVariable Long sectionId,
            @Valid @RequestBody SectionGradesSubmissionDTO submissionDTO,
            Authentication authentication
    ) {
        try {
            boolean isAdmin = authentication.getAuthorities().stream()
                    .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
            String teacherRun = authentication.getName();

            List<StudentGradeEntryDTO> updatedGrades = gradeService.submitSectionGrades(sectionId, submissionDTO, teacherRun, isAdmin);
            return ResponseEntity.ok(updatedGrades);
        } catch (ResourceNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("message", e.getMessage()));
        } catch (BusinessRuleException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("message", e.getMessage()));
        } catch (Exception e) {
            log.error("Error submitting section grades: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", "Error submitting final grades."));
        }
    }
}
