package cl.usach.tingeso.controller;

import cl.usach.tingeso.dto.StudyPlanRegistrationDTO;
import cl.usach.tingeso.dto.StudyPlanResponseDTO;
import cl.usach.tingeso.exception.BusinessRuleException;
import cl.usach.tingeso.exception.ResourceNotFoundException;
import cl.usach.tingeso.service.StudyPlanService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * StudyPlanController exposes REST endpoints for study plan lifecycle management.
 * Layer: Controller.
 */
@RestController
@RequestMapping("/api/study-plans")
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
@Slf4j
public class StudyPlanController {

    private final StudyPlanService studyPlanService;

    /**
     * Retrieves all study plans for a given career code.
     * Accessible by ADMIN and TEACHER.
     *
     * @param careerCode the unique code of the career
     * @return 200 OK with list of study plans
     */
    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'TEACHER')")
    public ResponseEntity<List<StudyPlanResponseDTO>> getPlansByCareer(
            @RequestParam(name = "careerCode") String careerCode) {
        return ResponseEntity.ok(studyPlanService.getPlansByCareer(careerCode));
    }

    /**
     * Retrieves a single study plan by ID.
     *
     * @param id study plan ID
     * @return 200 OK with study plan data, or 404 if not found
     */
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'TEACHER', 'STUDENT')")
    public ResponseEntity<?> getStudyPlanById(@PathVariable Long id) {
        try {
            return ResponseEntity.ok(studyPlanService.getStudyPlanById(id));
        } catch (ResourceNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        }
    }

    /**
     * Registers a new study plan for a career.
     * Accessible only by ADMIN.
     *
     * @param dto registration payload
     * @return 201 Created on success, or 400 on business rule failure
     */
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> createStudyPlan(@Valid @RequestBody StudyPlanRegistrationDTO dto) {
        try {
            StudyPlanResponseDTO created = studyPlanService.createStudyPlan(dto);
            return ResponseEntity.status(HttpStatus.CREATED).body(created);
        } catch (ResourceNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        } catch (BusinessRuleException e) {
            log.warn("Business rule violation creating study plan: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        } catch (Exception e) {
            log.error("Unexpected error creating study plan: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Unexpected error: " + e.getMessage());
        }
    }

    /**
     * Activates a study plan as the current active plan for its career.
     * Accessible only by ADMIN.
     *
     * @param id study plan ID to activate
     * @return 200 OK with activated study plan, 404 if not found, or 400 on error
     */
    @PutMapping("/{id}/activate")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> activateStudyPlan(@PathVariable Long id) {
        try {
            StudyPlanResponseDTO activated = studyPlanService.activateStudyPlan(id);
            return ResponseEntity.ok(activated);
        } catch (ResourceNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        } catch (BusinessRuleException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        } catch (Exception e) {
            log.error("Unexpected error activating study plan {}: {}", id, e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Unexpected error: " + e.getMessage());
        }
    }

    /**
     * Deletes a study plan if it has no enrolled students.
     * Accessible only by ADMIN.
     *
     * @param id study plan ID to delete
     * @return 204 No Content on success, 400 if blocked by business rule, or 404 if not found
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> deleteStudyPlan(@PathVariable Long id) {
        try {
            studyPlanService.deleteStudyPlan(id);
            return ResponseEntity.noContent().build();
        } catch (ResourceNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        } catch (BusinessRuleException e) {
            log.warn("Business rule violation deleting study plan {}: {}", id, e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        } catch (Exception e) {
            log.error("Unexpected error deleting study plan {}: {}", id, e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Unexpected error: " + e.getMessage());
        }
    }
}
