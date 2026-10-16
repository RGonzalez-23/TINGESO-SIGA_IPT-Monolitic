package cl.usach.tingeso.service;

import cl.usach.tingeso.dto.SectionGradesSubmissionDTO;
import cl.usach.tingeso.dto.StudentGradeEntryDTO;
import cl.usach.tingeso.entity.EnrollmentEntity;
import cl.usach.tingeso.entity.EnrollmentStatus;
import cl.usach.tingeso.entity.SectionEntity;
import cl.usach.tingeso.exception.BusinessRuleException;
import cl.usach.tingeso.exception.ResourceNotFoundException;
import cl.usach.tingeso.repository.EnrollmentRepository;
import cl.usach.tingeso.repository.SectionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

/**
 * GradeService manages the registration and retrieval of final grades for section enrollments.
 * Corresponds to Epic 7: Final Grades Registration and Semester Close.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class GradeService {

    private final SectionRepository sectionRepository;
    private final EnrollmentRepository enrollmentRepository;

    /**
     * Retrieves the current grade roster for a section.
     * Accessible by the assigned teacher or an academic administrator.
     */
    @Transactional(readOnly = true)
    public List<StudentGradeEntryDTO> getSectionGrades(Long sectionId, String teacherRun, boolean isAdmin) {
        SectionEntity section = getSectionOrThrow(sectionId);
        validateTeacherAssignment(section, teacherRun, isAdmin);

        List<EnrollmentEntity> enrollments;
        if (Boolean.TRUE.equals(section.getAcademicPeriod().getIsOpen())) {
            enrollments = enrollmentRepository.findBySection_IdAndStatus(sectionId, EnrollmentStatus.INSCRITA);
        } else {
            enrollments = enrollmentRepository.findBySection_Id(sectionId).stream()
                    .filter(e -> e.getStatus() != EnrollmentStatus.ANULADA)
                    .toList();
        }

        return enrollments.stream()
                .map(this::mapToGradeEntry)
                .toList();
    }

    /**
     * Registers or updates final grades for students enrolled in a section.
     * Enforces that the academic period is open and that the requester is the assigned teacher.
     */
    @Transactional
    public List<StudentGradeEntryDTO> submitSectionGrades(Long sectionId, SectionGradesSubmissionDTO submissionDTO, String teacherRun, boolean isAdmin) {
        SectionEntity section = getSectionOrThrow(sectionId);
        validateTeacherAssignment(section, teacherRun, isAdmin);

        if (Boolean.FALSE.equals(section.getAcademicPeriod().getIsOpen())) {
            throw new BusinessRuleException(
                    "Cannot register or modify grades: academic period " + section.getAcademicPeriod().getId() + " is CLOSED."
            );
        }

        List<StudentGradeEntryDTO> results = new ArrayList<>();

        for (StudentGradeEntryDTO entry : submissionDTO.getGrades()) {
            EnrollmentEntity enrollment = enrollmentRepository.findById(entry.getEnrollmentId())
                    .orElseThrow(() -> new ResourceNotFoundException("Enrollment not found with ID: " + entry.getEnrollmentId()));

            if (!enrollment.getSection().getId().equals(sectionId)) {
                throw new BusinessRuleException(
                        "Enrollment " + entry.getEnrollmentId() + " does not belong to section " + section.getSectionCode()
                );
            }

            if (enrollment.getStatus() == EnrollmentStatus.ANULADA) {
                throw new BusinessRuleException(
                        "Cannot assign grade to cancelled enrollment for student " + enrollment.getStudent().getRun()
                );
            }

            Double grade = entry.getFinalGrade();
            if (grade == null || grade < 1.0 || grade > 7.0) {
                throw new BusinessRuleException("Final grade must be between 1.0 and 7.0. Provided: " + grade);
            }

            double roundedGrade = BigDecimal.valueOf(grade)
                    .setScale(1, RoundingMode.HALF_UP)
                    .doubleValue();

            enrollment.setFinalGrade(roundedGrade);
            EnrollmentEntity saved = enrollmentRepository.save(enrollment);
            results.add(mapToGradeEntry(saved));
        }

        log.info("Registered {} final grades for section {} by teacher {}", results.size(), section.getSectionCode(), teacherRun);
        return results;
    }

    private SectionEntity getSectionOrThrow(Long sectionId) {
        return sectionRepository.findById(sectionId)
                .orElseThrow(() -> new ResourceNotFoundException("Section not found with ID: " + sectionId));
    }

    private void validateTeacherAssignment(SectionEntity section, String teacherRun, boolean isAdmin) {
        if (isAdmin) {
            return;
        }
        if (section.getTeacher() == null || !section.getTeacher().getRun().equalsIgnoreCase(teacherRun)) {
            throw new BusinessRuleException("Access denied: only the assigned teacher can manage grades for section " + section.getSectionCode());
        }
    }

    private StudentGradeEntryDTO mapToGradeEntry(EnrollmentEntity e) {
        return StudentGradeEntryDTO.builder()
                .enrollmentId(e.getId())
                .studentRun(e.getStudent().getRun())
                .studentName(e.getStudent().getFirstName() + " " + e.getStudent().getPaternalLastName())
                .finalGrade(e.getFinalGrade())
                .status(e.getStatus().name())
                .build();
    }
}
