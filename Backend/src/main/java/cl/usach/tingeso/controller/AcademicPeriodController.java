package cl.usach.tingeso.controller;

import cl.usach.tingeso.dto.AcademicPeriodCreateDTO;
import cl.usach.tingeso.dto.AcademicPeriodResponseDTO;
import cl.usach.tingeso.exception.BusinessRuleException;
import cl.usach.tingeso.exception.ResourceNotFoundException;
import cl.usach.tingeso.service.AcademicPeriodService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * AcademicPeriodController exposes REST endpoints for managing academic periods.
 * Corresponds to Epic 5: Academic Offer and Sections Management.
 * Layer: Controller.
 */
@RestController
@RequestMapping("/api/academic-periods")
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
@Slf4j
public class AcademicPeriodController {

    private final AcademicPeriodService academicPeriodService;

    /**
     * Retrieves all academic periods.
     * Accessible by any authenticated user.
     */
    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<AcademicPeriodResponseDTO>> getAllPeriods() {
        return ResponseEntity.ok(academicPeriodService.getAllPeriods());
    }

    /**
     * Retrieves an academic period by ID (e.g., "2027-1").
     */
    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> getPeriodById(@PathVariable String id) {
        try {
            return ResponseEntity.ok(academicPeriodService.getPeriodById(id));
        } catch (ResourceNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("message", e.getMessage()));
        }
    }

    /**
     * Creates a new academic period in initial ABIERTO state.
     * Accessible only by ADMIN.
     */
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> createPeriod(@Valid @RequestBody AcademicPeriodCreateDTO dto) {
        try {
            AcademicPeriodResponseDTO created = academicPeriodService.createPeriod(dto);
            return ResponseEntity.status(HttpStatus.CREATED).body(created);
        } catch (BusinessRuleException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("message", e.getMessage()));
        } catch (Exception e) {
            log.error("Error creating academic period: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", "Error al crear período académico: " + e.getMessage()));
        }
    }

    /**
     * Toggles an academic period status between ABIERTO and CERRADO.
     * Accessible only by ADMIN.
     */
    @PatchMapping("/{id}/toggle-status")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> togglePeriodStatus(@PathVariable String id) {
        try {
            AcademicPeriodResponseDTO updated = academicPeriodService.togglePeriodStatus(id);
            return ResponseEntity.ok(updated);
        } catch (ResourceNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("message", e.getMessage()));
        } catch (Exception e) {
            log.error("Error toggling academic period status for {}: {}", id, e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", "Error al modificar estado del período: " + e.getMessage()));
        }
    }
}
