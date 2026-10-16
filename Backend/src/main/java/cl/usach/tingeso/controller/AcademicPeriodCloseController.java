package cl.usach.tingeso.controller;

import cl.usach.tingeso.dto.PeriodCloseResultDTO;
import cl.usach.tingeso.dto.PeriodCloseValidationDTO;
import cl.usach.tingeso.exception.BusinessRuleException;
import cl.usach.tingeso.exception.ResourceNotFoundException;
import cl.usach.tingeso.service.PeriodCloseService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * AcademicPeriodCloseController exposes REST endpoints for semester closing operations.
 * Corresponds to Epic 7: Final Grades Registration and Semester Close.
 */
@RestController
@RequestMapping("/api/academic-periods/{id}")
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
@Slf4j
public class AcademicPeriodCloseController {

    private final PeriodCloseService periodCloseService;

    /**
     * Checks whether an academic period is ready for semester closure.
     * Reports any sections with missing final grades.
     * Accessible only by ADMIN.
     */
    @GetMapping("/close-readiness")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> checkCloseReadiness(@PathVariable String id) {
        try {
            PeriodCloseValidationDTO validation = periodCloseService.checkCloseReadiness(id);
            return ResponseEntity.ok(validation);
        } catch (ResourceNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("message", e.getMessage()));
        } catch (Exception e) {
            log.error("Error auditing close readiness for period {}: {}", id, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", "Error checking period close readiness."));
        }
    }

    /**
     * Executes the formal semester close:
     * - Validates 100% grade completion.
     * - Transitions enrollments to APROBADA/REPROBADA.
     * - Evaluates student academic statuses (ELIMINADO / EGRESADO).
     * - Closes the academic period.
     * Accessible only by ADMIN.
     */
    @PostMapping("/close")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> executePeriodClose(@PathVariable String id) {
        try {
            PeriodCloseResultDTO result = periodCloseService.executePeriodClose(id);
            return ResponseEntity.ok(result);
        } catch (ResourceNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("message", e.getMessage()));
        } catch (BusinessRuleException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("message", e.getMessage()));
        } catch (Exception e) {
            log.error("Error executing semester close for period {}: {}", id, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", "Unexpected error executing semester close: " + e.getMessage()));
        }
    }
}
