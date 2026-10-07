package cl.usach.tingeso.controller;

import cl.usach.tingeso.dto.CareerRegistrationDTO;
import cl.usach.tingeso.dto.CareerResponseDTO;
import cl.usach.tingeso.dto.CareerUpdateDTO;
import cl.usach.tingeso.exception.BusinessRuleException;
import cl.usach.tingeso.exception.ResourceNotFoundException;
import cl.usach.tingeso.service.CareerService;
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
 * CareerController exposes REST endpoints for career management and queries.
 * Layer: Controller.
 */
@RestController
@RequestMapping("/api/careers")
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
@Slf4j
public class CareerController {

    private final CareerService careerService;

    /**
     * Retrieves careers.
     * If activeOnly=true is requested, returns only active careers.
     * Otherwise returns all careers.
     *
     * @param activeOnly optional filter parameter
     * @return 200 OK with list of careers
     */
    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'TEACHER')")
    public ResponseEntity<List<CareerResponseDTO>> getCareers(
            @RequestParam(name = "activeOnly", required = false, defaultValue = "false") boolean activeOnly) {
        if (activeOnly) {
            return ResponseEntity.ok(careerService.getActiveCareers());
        }
        return ResponseEntity.ok(careerService.getAllCareers());
    }

    /**
     * Retrieves a single career by its unique code.
     * Accessible by ADMIN and TEACHER.
     *
     * @param code unique code of the career
     * @return 200 OK with career data, or 404 if not found
     */
    @GetMapping("/{code}")
    @PreAuthorize("hasAnyRole('ADMIN', 'TEACHER')")
    public ResponseEntity<?> getCareerByCode(@PathVariable String code) {
        try {
            return ResponseEntity.ok(careerService.getCareerByCode(code));
        } catch (ResourceNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        }
    }

    /**
     * Registers a new career and its initial study plan.
     * Accessible only by ADMIN.
     *
     * @param dto registration payload
     * @return 201 Created on success, or 400 on business rule failure
     */
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> registerCareer(@Valid @RequestBody CareerRegistrationDTO dto) {
        try {
            CareerResponseDTO created = careerService.registerCareer(dto);
            return ResponseEntity.status(HttpStatus.CREATED).body(created);
        } catch (BusinessRuleException e) {
            log.warn("Business rule violation registering career: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        } catch (Exception e) {
            log.error("Unexpected error registering career: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Unexpected error: " + e.getMessage());
        }
    }

    /**
     * Updates an existing career.
     * Accessible only by ADMIN.
     *
     * @param code unique code of the career
     * @param dto update payload
     * @return 200 OK with updated career, 404 if not found, or 400 on error
     */
    @PutMapping("/{code}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> updateCareer(@PathVariable String code, @Valid @RequestBody CareerUpdateDTO dto) {
        try {
            CareerResponseDTO updated = careerService.updateCareer(code, dto);
            return ResponseEntity.ok(updated);
        } catch (ResourceNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        } catch (BusinessRuleException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        } catch (Exception e) {
            log.error("Unexpected error updating career {}: {}", code, e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Unexpected error: " + e.getMessage());
        }
    }

    /**
     * Deletes a career if it has no associated study plans or enrolled students.
     * Accessible only by ADMIN.
     *
     * @param code unique code of the career
     * @return 204 No Content on success, 400 if blocked by business rule, or 404 if not found
     */
    @DeleteMapping("/{code}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> deleteCareer(@PathVariable String code) {
        try {
            careerService.deleteCareer(code);
            return ResponseEntity.noContent().build();
        } catch (ResourceNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        } catch (BusinessRuleException e) {
            log.warn("Business rule violation deleting career {}: {}", code, e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        } catch (Exception e) {
            log.error("Unexpected error deleting career {}: {}", code, e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Unexpected error: " + e.getMessage());
        }
    }
}
