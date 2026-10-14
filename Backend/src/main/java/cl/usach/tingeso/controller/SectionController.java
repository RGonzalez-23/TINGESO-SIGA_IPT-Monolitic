package cl.usach.tingeso.controller;

import cl.usach.tingeso.dto.EnrolledStudentDTO;
import cl.usach.tingeso.dto.SectionCreateDTO;
import cl.usach.tingeso.dto.SectionResponseDTO;
import cl.usach.tingeso.dto.SectionUpdateDTO;
import cl.usach.tingeso.exception.BusinessRuleException;
import cl.usach.tingeso.exception.ResourceNotFoundException;
import cl.usach.tingeso.service.EnrollmentService;
import cl.usach.tingeso.service.SectionService;
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
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * SectionController exposes REST endpoints for managing academic course sections.
 * Corresponds to Epic 5: Academic Offer and Sections Management.
 * Layer: Controller.
 */
@RestController
@RequestMapping("/api/sections")
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
@Slf4j
public class SectionController {

    private final SectionService sectionService;
    private final EnrollmentService enrollmentService;

    /**
     * Retrieves course sections with optional filters by period, study plan, or teacher RUN.
     */
    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<SectionResponseDTO>> getSections(
            @RequestParam(required = false) String periodId,
            @RequestParam(required = false) Long studyPlanId,
            @RequestParam(required = false) String teacherRun
    ) {
        return ResponseEntity.ok(sectionService.getSections(periodId, studyPlanId, teacherRun));
    }

    /**
     * Retrieves a single section by its ID.
     */
    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> getSectionById(@PathVariable Long id) {
        try {
            return ResponseEntity.ok(sectionService.getSectionById(id));
        } catch (ResourceNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("message", e.getMessage()));
        }
    }

    /**
     * Creates a new course section in an academic period.
     * Enforces single section per subject rule, teacher availability, and timetable conflicts.
     * Accessible only by ADMIN.
     */
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> createSection(@Valid @RequestBody SectionCreateDTO dto) {
        try {
            SectionResponseDTO created = sectionService.createSection(dto);
            return ResponseEntity.status(HttpStatus.CREATED).body(created);
        } catch (BusinessRuleException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("message", e.getMessage()));
        } catch (ResourceNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("message", e.getMessage()));
        } catch (Exception e) {
            log.error("Error creating section: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", "Error al crear la sección: " + e.getMessage()));
        }
    }

    /**
     * Updates an existing course section.
     * Accessible only by ADMIN.
     */
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> updateSection(
            @PathVariable Long id,
            @Valid @RequestBody SectionUpdateDTO dto
    ) {
        try {
            SectionResponseDTO updated = sectionService.updateSection(id, dto);
            return ResponseEntity.ok(updated);
        } catch (BusinessRuleException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("message", e.getMessage()));
        } catch (ResourceNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("message", e.getMessage()));
        } catch (Exception e) {
            log.error("Error updating section {}: {}", id, e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", "Error al actualizar la sección: " + e.getMessage()));
        }
    }

    /**
     * Deletes a section physically. Prohibited if students are enrolled.
     * Accessible only by ADMIN.
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> deleteSection(@PathVariable Long id) {
        try {
            sectionService.deleteSection(id);
            return ResponseEntity.ok(Map.of("message", "Sección eliminada exitosamente."));
        } catch (BusinessRuleException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("message", e.getMessage()));
        } catch (ResourceNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("message", e.getMessage()));
        } catch (Exception e) {
            log.error("Error deleting section {}: {}", id, e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", "Error al eliminar la sección: " + e.getMessage()));
        }
    }

    /**
     * Retrieves all sections assigned to the currently authenticated teacher.
     * Accessible by TEACHER.
     */
    @GetMapping("/teacher/my-sections")
    @PreAuthorize("hasRole('TEACHER')")
    public ResponseEntity<List<SectionResponseDTO>> getMyTeacherSections(Authentication authentication) {
        String teacherRun = authentication.getName();
        return ResponseEntity.ok(sectionService.getSectionsByTeacher(teacherRun));
    }

    /**
     * Retrieves the list of students enrolled in a section.
     * Accessible by the TEACHER assigned to that section, or ADMIN.
     */
    @GetMapping("/{id}/enrolled-students")
    @PreAuthorize("hasAnyRole('TEACHER', 'ADMIN')")
    public ResponseEntity<?> getEnrolledStudents(
            @PathVariable Long id,
            Authentication authentication
    ) {
        try {
            boolean isAdmin = authentication != null && authentication.getAuthorities().stream()
                    .map(GrantedAuthority::getAuthority)
                    .anyMatch(r -> r.equals("ROLE_ADMIN") || r.equals("ADMIN"));
            String username = authentication != null ? authentication.getName() : "";
            List<EnrolledStudentDTO> students = enrollmentService.getEnrolledStudentsForSection(id, username, isAdmin);
            return ResponseEntity.ok(students);
        } catch (BusinessRuleException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("message", e.getMessage()));
        } catch (ResourceNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("message", e.getMessage()));
        } catch (Exception e) {
            log.error("Error retrieving enrolled students for section {}: {}", id, e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", "Error al consultar alumnos de la sección: " + e.getMessage()));
        }
    }
}
