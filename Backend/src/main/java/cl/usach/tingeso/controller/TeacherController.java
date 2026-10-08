package cl.usach.tingeso.controller;

import cl.usach.tingeso.dto.TeacherRegistrationDTO;
import cl.usach.tingeso.dto.TeacherResponseDTO;
import cl.usach.tingeso.dto.TeacherUpdateDTO;
import cl.usach.tingeso.exception.BusinessRuleException;
import cl.usach.tingeso.exception.ResourceNotFoundException;
import cl.usach.tingeso.service.InstitutionalEmailService;
import cl.usach.tingeso.service.TeacherService;
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
import java.util.Map;

/**
 * TeacherController exposes REST endpoints for teacher management.
 * Corresponds to Epic 4: Teacher Management.
 * Layer: Controller.
 */
@RestController
@RequestMapping("/api/teachers")
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
@Slf4j
public class TeacherController {

    private final TeacherService teacherService;
    private final InstitutionalEmailService institutionalEmailService;

    /**
     * Retrieves all teachers with optional status and search filtering.
     * Accessible by ADMIN.
     */
    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<TeacherResponseDTO>> getAllTeachers(
            @RequestParam(required = false) Boolean active,
            @RequestParam(required = false) String search
    ) {
        return ResponseEntity.ok(teacherService.getAllTeachers(active, search));
    }

    /**
     * Previews the unique institutional email that would be generated for a teacher.
     * Accessible by ADMIN.
     */
    @GetMapping("/preview-email")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, String>> previewEmail(
            @RequestParam String firstName,
            @RequestParam String paternalLastName,
            @RequestParam(required = false, defaultValue = "") String maternalLastName
    ) {
        String email = institutionalEmailService.generateUniqueEmail(firstName, paternalLastName, maternalLastName);
        return ResponseEntity.ok(Map.of("email", email));
    }

    /**
     * Retrieves a teacher by RUN.
     * Accessible by ADMIN, or by TEACHER if querying their own RUN profile.
     */
    @GetMapping("/{run}")
    @PreAuthorize("hasRole('ADMIN') or (hasRole('TEACHER') and #run == authentication.name)")
    public ResponseEntity<?> getTeacherByRun(@PathVariable String run) {
        try {
            TeacherResponseDTO teacher = teacherService.getTeacherByRun(run);
            return ResponseEntity.ok(teacher);
        } catch (ResourceNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("message", e.getMessage()));
        }
    }

    /**
     * Registers a new teacher.
     * Accessible only by ADMIN.
     */
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> registerTeacher(@Valid @RequestBody TeacherRegistrationDTO dto) {
        try {
            TeacherResponseDTO created = teacherService.registerTeacher(dto);
            return ResponseEntity.status(HttpStatus.CREATED).body(created);
        } catch (BusinessRuleException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("message", e.getMessage()));
        } catch (Exception e) {
            log.error("Unexpected error creating teacher: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", "An unexpected error occurred while registering the teacher."));
        }
    }

    /**
     * Updates an existing teacher.
     * Accessible only by ADMIN.
     */
    @PutMapping("/{run}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> updateTeacher(
            @PathVariable String run,
            @Valid @RequestBody TeacherUpdateDTO dto
    ) {
        try {
            TeacherResponseDTO updated = teacherService.updateTeacher(run, dto);
            return ResponseEntity.ok(updated);
        } catch (ResourceNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("message", e.getMessage()));
        } catch (BusinessRuleException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("message", e.getMessage()));
        } catch (Exception e) {
            log.error("Unexpected error updating teacher {}: {}", run, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", "An unexpected error occurred while updating the teacher."));
        }
    }

    /**
     * Deletes a teacher physically if they have no associated sections.
     * Accessible only by ADMIN.
     */
    @DeleteMapping("/{run}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> deleteTeacher(@PathVariable String run) {
        try {
            teacherService.deleteTeacher(run);
            return ResponseEntity.ok(Map.of("message", "Teacher deleted successfully."));
        } catch (ResourceNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("message", e.getMessage()));
        } catch (BusinessRuleException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("message", e.getMessage()));
        } catch (Exception e) {
            log.error("Unexpected error deleting teacher {}: {}", run, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", "An unexpected error occurred while deleting the teacher."));
        }
    }
}
