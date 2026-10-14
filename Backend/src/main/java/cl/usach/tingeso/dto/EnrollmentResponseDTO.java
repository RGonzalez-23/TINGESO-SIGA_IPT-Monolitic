package cl.usach.tingeso.dto;

import cl.usach.tingeso.entity.EnrollmentStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * EnrollmentResponseDTO represents a student's course registration.
 * Corresponds to Epic 6: Academic Enrollment.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EnrollmentResponseDTO {

    private Long id;
    private String studentRun;
    private String studentName;
    private Long courseId;
    private String courseCode;
    private String courseName;
    private Integer semester;
    private Integer sctCredits;
    private Long sectionId;
    private String sectionCode;
    private String academicPeriodId;
    private String teacherRun;
    private String teacherName;
    private List<String> scheduleTimeBlocks;
    private LocalDateTime enrollmentDate;
    private LocalDateTime cancellationDate;
    private EnrollmentStatus status;
    private Double finalGrade;
}
