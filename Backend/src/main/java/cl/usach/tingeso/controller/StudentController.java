package cl.usach.tingeso.controller;

import cl.usach.tingeso.dto.StudentRegistrationDTO;
import cl.usach.tingeso.dto.StudentResponseDTO;
import cl.usach.tingeso.dto.StudentUpdateDTO;
import cl.usach.tingeso.exception.BusinessRuleException;
import cl.usach.tingeso.exception.ResourceNotFoundException;
import cl.usach.tingeso.service.StudentService;
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
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * StudentController exposes REST endpoints for student management.
 * Corresponds to Epic 1: Student Management.
 * Layer: Controller.
 */
@RestController
@RequestMapping("/api/students")
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
@Slf4j
public class StudentController {

    private final StudentService studentService;

    /**
     * Retrieves all registered students.
     * Accessible by ADMIN and TEACHER.
     *
     * @return 200 OK with list of students, or 500 on unexpected error
     */
    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'TEACHER')")
    public ResponseEntity<?> getAllStudents() {
        try {
            List<StudentResponseDTO> students = studentService.getAllStudents();
            return ResponseEntity.ok(students);
        } catch (Exception e) {
            log.error("Error retrieving all students: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error retrieving students: " + e.getMessage());
        }
    }

    /**
     * Retrieves a student by their unique RUN.
     * Accessible by ADMIN, TEACHER, or the STUDENT themselves.
     *
     * @param run National identification number
     * @return 200 OK with student data, 404 if not found, or 400 on invalid format
     */
    @GetMapping("/{run}")
    @PreAuthorize("hasAnyRole('ADMIN', 'TEACHER') or (hasRole('STUDENT') and #run == authentication.name)")
    public ResponseEntity<?> getStudentByRun(@PathVariable String run) {
        try {
            StudentResponseDTO student = studentService.getStudentByRun(run);
            return ResponseEntity.ok(student);
        } catch (ResourceNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        } catch (Exception e) {
            log.error("Error retrieving student with RUN {}: {}", run, e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        }
    }

    /**
     * Registers a new student in the system.
     * Accessible only by ADMIN.
     *
     * @param dto student registration payload
     * @return 201 Created with student response data, or 400 Bad Request on business validation errors
     */
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> registerStudent(@Valid @RequestBody StudentRegistrationDTO dto) {
        try {
            StudentResponseDTO created = studentService.registerStudent(dto);
            return ResponseEntity.status(HttpStatus.CREATED).body(created);
        } catch (BusinessRuleException e) {
            log.warn("Business rule violation registering student: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        } catch (ResourceNotFoundException e) {
            log.warn("Resource not found registering student: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        } catch (Exception e) {
            log.error("Unexpected error registering student: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Unexpected error: " + e.getMessage());
        }
    }

    /**
     * Updates student information.
     * Accessible only by ADMIN.
     *
     * @param run student RUN
     * @param dto update payload
     * @return 200 OK with updated data, 404 if not found, or 400 on validation failure
     */
    @PutMapping("/{run}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> updateStudent(@PathVariable String run, @Valid @RequestBody StudentUpdateDTO dto) {
        try {
            StudentResponseDTO updated = studentService.updateStudent(run, dto);
            return ResponseEntity.ok(updated);
        } catch (ResourceNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        } catch (BusinessRuleException e) {
            log.warn("Business rule violation updating student {}: {}", run, e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        } catch (Exception e) {
            log.error("Unexpected error updating student {}: {}", run, e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Unexpected error: " + e.getMessage());
        }
    }

    /**
     * Physically deletes a student if they have no enrollments or academic history.
     * Accessible only by ADMIN.
     *
     * @param run student RUN to delete
     * @return 204 No Content on success, 400 if blocked by business rule, or 404 if not found
     */
    @DeleteMapping("/{run}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> deleteStudent(@PathVariable String run) {
        try {
            studentService.deleteStudent(run);
            return ResponseEntity.noContent().build();
        } catch (ResourceNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        } catch (BusinessRuleException e) {
            log.warn("Business rule violation deleting student {}: {}", run, e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        } catch (Exception e) {
            log.error("Unexpected error deleting student {}: {}", run, e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Unexpected error: " + e.getMessage());
        }
    }
}
