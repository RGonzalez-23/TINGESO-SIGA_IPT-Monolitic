package cl.usach.tingeso.controller;

import cl.usach.tingeso.dto.StudentAcademicHistoryDTO;
import cl.usach.tingeso.dto.StudentCurriculumProgressDTO;
import cl.usach.tingeso.exception.ResourceNotFoundException;
import cl.usach.tingeso.service.AcademicHistoryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * StudentAcademicHistoryController exposes REST endpoints for student transcripts and curriculum progress.
 * Corresponds to Epic 7: Final Grades Registration and Semester Close.
 */
@RestController
@RequestMapping("/api/students")
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
@Slf4j
public class StudentAcademicHistoryController {

    private final AcademicHistoryService academicHistoryService;

    /**
     * Retrieves the chronological academic transcript for the authenticated student.
     * Accessible by STUDENT.
     */
    @GetMapping("/me/academic-history")
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<?> getMyAcademicHistory(Authentication authentication) {
        return getStudentAcademicHistory(authentication.getName());
    }

    /**
     * Retrieves the curriculum progress map (color-coded) for the authenticated student.
     * Accessible by STUDENT.
     */
    @GetMapping("/me/curriculum-progress")
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<?> getMyCurriculumProgress(Authentication authentication) {
        return getStudentCurriculumProgress(authentication.getName());
    }

    /**
     * Retrieves the chronological academic transcript of a specific student by RUN.
     * Accessible by ADMIN, or by the STUDENT themselves.
     */
    @GetMapping("/{run}/academic-history")
    @PreAuthorize("hasRole('ADMIN') or (hasRole('STUDENT') and #run == authentication.name)")
    public ResponseEntity<?> getStudentAcademicHistory(@PathVariable String run) {
        try {
            StudentAcademicHistoryDTO history = academicHistoryService.getStudentAcademicHistory(run);
            return ResponseEntity.ok(history);
        } catch (ResourceNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("message", e.getMessage()));
        } catch (Exception e) {
            log.error("Error retrieving academic history for student {}: {}", run, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", "Error retrieving academic history."));
        }
    }

    /**
     * Retrieves the curriculum progress map (color-coded) of a specific student by RUN.
     * Accessible by ADMIN, or by the STUDENT themselves.
     */
    @GetMapping("/{run}/curriculum-progress")
    @PreAuthorize("hasRole('ADMIN') or (hasRole('STUDENT') and #run == authentication.name)")
    public ResponseEntity<?> getStudentCurriculumProgress(@PathVariable String run) {
        try {
            StudentCurriculumProgressDTO progress = academicHistoryService.getStudentCurriculumProgress(run);
            return ResponseEntity.ok(progress);
        } catch (ResourceNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("message", e.getMessage()));
        } catch (Exception e) {
            log.error("Error retrieving curriculum progress for student {}: {}", run, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", "Error retrieving curriculum progress."));
        }
    }
}
