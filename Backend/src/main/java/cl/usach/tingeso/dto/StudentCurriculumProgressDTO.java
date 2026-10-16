package cl.usach.tingeso.dto;

import cl.usach.tingeso.entity.AcademicStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * StudentCurriculumProgressDTO aggregates curriculum progress for a student's study plan map.
 * Corresponds to Epic 7: Final Grades Registration and Semester Close.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StudentCurriculumProgressDTO {

    private String studentRun;
    private String studentFullName;
    private String careerName;
    private Long studyPlanId;
    private String studyPlanCode;
    private AcademicStatus academicStatus;
    private Integer approvedCoursesCount;
    private Integer failedCoursesCount;
    private Integer enrolledCoursesCount;
    private Integer totalPlanCoursesCount;
    private List<CurriculumCourseProgressDTO> courses;
}
