package cl.usach.tingeso.controller;

import cl.usach.tingeso.dto.AvailableCourseOfferDTO;
import cl.usach.tingeso.dto.EnrollmentRequestDTO;
import cl.usach.tingeso.dto.EnrollmentResponseDTO;
import cl.usach.tingeso.exception.BusinessRuleException;
import cl.usach.tingeso.exception.ResourceNotFoundException;
import cl.usach.tingeso.service.EnrollmentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * EnrollmentController exposes REST endpoints for student academic course registration.
 * Corresponds to Epic 6: Academic Enrollment.
 * Layer: Controller.
 */
@RestController
@RequestMapping("/api/enrollments")
@CrossOrigin(origins = "*")
@Slf4j
public class EnrollmentController {

    private final EnrollmentService enrollmentService;

    public EnrollmentController(EnrollmentService enrollmentService) {
        this.enrollmentService = enrollmentService;
    }

    /**
     * Registers a student into a list of selected sections in the active academic period.
     * Accessible by STUDENT (for own enrollment) and ADMIN (on behalf of any student).
     */
    @PostMapping("/register")
    @PreAuthorize("hasAnyRole('STUDENT', 'ADMIN')")
    public ResponseEntity<?> registerEnrollment(
            @Valid @RequestBody EnrollmentRequestDTO request,
            Authentication authentication
    ) {
        try {
            boolean isAdmin = checkIsAdmin(authentication);
            String username = authentication != null ? authentication.getName() : null;
            List<EnrollmentResponseDTO> enrollments = enrollmentService.registerEnrollment(request, username, isAdmin);
            return ResponseEntity.status(HttpStatus.CREATED).body(enrollments);
        } catch (BusinessRuleException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("message", e.getMessage()));
        } catch (ResourceNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("message", e.getMessage()));
        } catch (Exception e) {
            log.error("Unexpected error during enrollment registration: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", "Error al procesar la inscripción: " + e.getMessage()));
        }
    }

    /**
     * Cancels an active enrollment in the open academic period, releasing the seat.
     * Accessible by STUDENT (for own enrollment) and ADMIN.
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('STUDENT', 'ADMIN')")
    public ResponseEntity<?> cancelEnrollment(
            @PathVariable Long id,
            Authentication authentication
    ) {
        try {
            boolean isAdmin = checkIsAdmin(authentication);
            String username = authentication != null ? authentication.getName() : null;
            EnrollmentResponseDTO cancelled = enrollmentService.cancelEnrollment(id, username, isAdmin);
            return ResponseEntity.ok(cancelled);
        } catch (BusinessRuleException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("message", e.getMessage()));
        } catch (ResourceNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("message", e.getMessage()));
        } catch (Exception e) {
            log.error("Unexpected error cancelling enrollment {}: {}", id, e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", "Error al anular la inscripción: " + e.getMessage()));
        }
    }

    /**
     * Retrieves the catalog of available course sections for enrollment, annotated with eligibility checks.
     * Accessible by STUDENT (for self) and ADMIN (for specified student run).
     */
    @GetMapping("/available-offer")
    @PreAuthorize("hasAnyRole('STUDENT', 'ADMIN')")
    public ResponseEntity<?> getAvailableOffer(
            @RequestParam(required = false) String studentRun,
            Authentication authentication
    ) {
        try {
            boolean isAdmin = checkIsAdmin(authentication);
            String targetRun = isAdmin && studentRun != null && !studentRun.isBlank()
                    ? studentRun
                    : (authentication != null ? authentication.getName() : "");

            List<AvailableCourseOfferDTO> offer = enrollmentService.getAvailableOfferForStudent(targetRun);
            return ResponseEntity.ok(offer);
        } catch (ResourceNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("message", e.getMessage()));
        } catch (Exception e) {
            log.error("Error retrieving available offer: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", "Error al obtener la oferta disponible: " + e.getMessage()));
        }
    }

    /**
     * Retrieves active enrollments for the currently authenticated student in the open period.
     */
    @GetMapping("/my-enrollments")
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<List<EnrollmentResponseDTO>> getMyEnrollments(Authentication authentication) {
        String studentRun = authentication.getName();
        return ResponseEntity.ok(enrollmentService.getActiveEnrollments(studentRun));
    }

    /**
     * Retrieves all historical and current enrollments for a given student.
     * Accessible by ADMIN and the STUDENT themselves.
     */
    @GetMapping("/student/{run}")
    @PreAuthorize("hasRole('ADMIN') or (hasRole('STUDENT') and #run == authentication.name)")
    public ResponseEntity<List<EnrollmentResponseDTO>> getStudentEnrollments(@PathVariable String run) {
        return ResponseEntity.ok(enrollmentService.getAllStudentEnrollments(run));
    }

    /**
     * Retrieves active enrollments in the current open academic period for a specified student.
     * Accessible by ADMIN and the STUDENT themselves.
     */
    @GetMapping("/student/{run}/active")
    @PreAuthorize("hasRole('ADMIN') or (hasRole('STUDENT') and #run == authentication.name)")
    public ResponseEntity<List<EnrollmentResponseDTO>> getStudentActiveEnrollments(@PathVariable String run) {
        return ResponseEntity.ok(enrollmentService.getActiveEnrollments(run));
    }

    /**
     * Utility endpoint for ADMIN to seed a past approved course (e.g. for testing prerequisites).
     */
    @PostMapping("/seed-past")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> seedPastApproval(
            @RequestParam String studentRun,
            @RequestParam String courseCode,
            @RequestParam String periodId,
            @RequestParam Double grade
    ) {
        try {
            EnrollmentResponseDTO dto = enrollmentService.seedPastApprovedCourse(studentRun, courseCode, periodId, grade);
            return ResponseEntity.ok(dto);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("message", e.getMessage()));
        }
    }

    private boolean checkIsAdmin(Authentication authentication) {
        if (authentication == null) return false;
        return authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch(role -> role.equals("ROLE_ADMIN") || role.equals("ADMIN"));
    }
}
