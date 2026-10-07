package cl.usach.tingeso.controller;

import cl.usach.tingeso.dto.CourseRegistrationDTO;
import cl.usach.tingeso.dto.CourseResponseDTO;
import cl.usach.tingeso.dto.CourseUpdateDTO;
import cl.usach.tingeso.exception.BusinessRuleException;
import cl.usach.tingeso.exception.ResourceNotFoundException;
import cl.usach.tingeso.service.CourseService;
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
 * CourseController exposes REST endpoints for courses and curriculum management.
 * Layer: Controller.
 * Corresponds to Epic 3.
 */
@RestController
@RequestMapping("/api/courses")
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
@Slf4j
public class CourseController {

    private final CourseService courseService;

    /**
     * Retrieves courses for a study plan, with optional semester filter.
     * Accessible by authenticated users (ADMIN, TEACHER, STUDENT).
     *
     * @param studyPlanId ID of the study plan
     * @param semester optional semester filter (1 to 4)
     * @return 200 OK with list of courses
     */
    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'TEACHER', 'STUDENT')")
    public ResponseEntity<List<CourseResponseDTO>> getCourses(
            @RequestParam(name = "studyPlanId") Long studyPlanId,
            @RequestParam(name = "semester", required = false) Integer semester) {
        if (semester != null) {
            return ResponseEntity.ok(courseService.getCoursesByStudyPlanAndSemester(studyPlanId, semester));
        }
        return ResponseEntity.ok(courseService.getCoursesByStudyPlan(studyPlanId));
    }

    /**
     * Retrieves a single course by ID.
     * Accessible by ADMIN, TEACHER, STUDENT.
     *
     * @param id course ID
     * @return 200 OK with course data, or 404 if not found
     */
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'TEACHER', 'STUDENT')")
    public ResponseEntity<?> getCourseById(@PathVariable Long id) {
        try {
            return ResponseEntity.ok(courseService.getCourseById(id));
        } catch (ResourceNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        }
    }

    /**
     * Registers a new course within a study plan.
     * Accessible only by ADMIN.
     *
     * @param dto registration payload
     * @return 201 Created on success, or 400 on business rule failure
     */
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> createCourse(@Valid @RequestBody CourseRegistrationDTO dto) {
        try {
            CourseResponseDTO created = courseService.createCourse(dto);
            return ResponseEntity.status(HttpStatus.CREATED).body(created);
        } catch (ResourceNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        } catch (BusinessRuleException e) {
            log.warn("Business rule violation registering course: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        } catch (Exception e) {
            log.error("Unexpected error registering course: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Unexpected error: " + e.getMessage());
        }
    }

    /**
     * Updates an existing course and its prerequisites.
     * Accessible only by ADMIN.
     *
     * @param id course ID
     * @param dto update payload
     * @return 200 OK with updated course, 404 if not found, or 400 on error
     */
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> updateCourse(@PathVariable Long id, @Valid @RequestBody CourseUpdateDTO dto) {
        try {
            CourseResponseDTO updated = courseService.updateCourse(id, dto);
            return ResponseEntity.ok(updated);
        } catch (ResourceNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        } catch (BusinessRuleException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        } catch (Exception e) {
            log.error("Unexpected error updating course {}: {}", id, e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Unexpected error: " + e.getMessage());
        }
    }

    /**
     * Deletes a course if it has no dependent courses.
     * Accessible only by ADMIN.
     *
     * @param id course ID
     * @return 204 No Content on success, 400 if blocked, or 404 if not found
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> deleteCourse(@PathVariable Long id) {
        try {
            courseService.deleteCourse(id);
            return ResponseEntity.noContent().build();
        } catch (ResourceNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        } catch (BusinessRuleException e) {
            log.warn("Business rule violation deleting course {}: {}", id, e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        } catch (Exception e) {
            log.error("Unexpected error deleting course {}: {}", id, e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Unexpected error: " + e.getMessage());
        }
    }
}
