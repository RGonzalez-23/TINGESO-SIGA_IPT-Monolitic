package cl.usach.tingeso.service;

import cl.usach.tingeso.dto.CoursePrerequisiteDTO;
import cl.usach.tingeso.dto.CourseRegistrationDTO;
import cl.usach.tingeso.dto.CourseResponseDTO;
import cl.usach.tingeso.dto.CourseUpdateDTO;
import cl.usach.tingeso.entity.CourseEntity;
import cl.usach.tingeso.entity.StudyPlanEntity;
import cl.usach.tingeso.exception.BusinessRuleException;
import cl.usach.tingeso.exception.ResourceNotFoundException;
import cl.usach.tingeso.repository.CourseRepository;
import cl.usach.tingeso.repository.StudyPlanRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * CourseService handles business logic related to subjects and prerequisites.
 * Layer: Service.
 * Corresponds to Epic 3.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class CourseService {

    private final CourseRepository courseRepository;
    private final StudyPlanRepository studyPlanRepository;

    /**
     * Retrieves all courses for a specific study plan.
     *
     * @param studyPlanId ID of the study plan
     * @return list of course response DTOs
     * @throws ResourceNotFoundException if study plan does not exist
     */
    @Transactional(readOnly = true)
    public List<CourseResponseDTO> getCoursesByStudyPlan(Long studyPlanId) {
        if (!studyPlanRepository.existsById(studyPlanId)) {
            throw new ResourceNotFoundException("Study plan not found with ID: " + studyPlanId);
        }

        return courseRepository.findByStudyPlan_IdOrderBySemesterAscCodeAsc(studyPlanId).stream()
                .map(this::mapToDTO)
                .toList();
    }

    /**
     * Retrieves courses for a specific study plan and semester.
     *
     * @param studyPlanId ID of the study plan
     * @param semester semester number (1 to 4)
     * @return list of course response DTOs
     */
    @Transactional(readOnly = true)
    public List<CourseResponseDTO> getCoursesByStudyPlanAndSemester(Long studyPlanId, Integer semester) {
        if (!studyPlanRepository.existsById(studyPlanId)) {
            throw new ResourceNotFoundException("Study plan not found with ID: " + studyPlanId);
        }

        validateSemester(semester);

        return courseRepository.findByStudyPlan_IdAndSemesterOrderByCodeAsc(studyPlanId, semester).stream()
                .map(this::mapToDTO)
                .toList();
    }

    /**
     * Retrieves a single course by ID.
     *
     * @param id course ID
     * @return course response DTO
     * @throws ResourceNotFoundException if course not found
     */
    @Transactional(readOnly = true)
    public CourseResponseDTO getCourseById(Long id) {
        CourseEntity course = courseRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Course not found with ID: " + id));
        return mapToDTO(course);
    }

    /**
     * Registers a new course within a study plan with strict validation.
     *
     * @param dto registration payload
     * @return created course response DTO
     */
    @Transactional
    public CourseResponseDTO createCourse(CourseRegistrationDTO dto) {
        StudyPlanEntity studyPlan = studyPlanRepository.findById(dto.getStudyPlanId())
                .orElseThrow(() -> new ResourceNotFoundException("Study plan not found with ID: " + dto.getStudyPlanId()));

        String cleanCode = dto.getCode().trim().toUpperCase();

        if (courseRepository.existsByStudyPlan_IdAndCode(dto.getStudyPlanId(), cleanCode)) {
            throw new BusinessRuleException("A course with code '" + cleanCode
                    + "' already exists in study plan " + studyPlan.getCode());
        }

        validateSemester(dto.getSemester());
        validateTelHours(dto.getTheoryHours(), dto.getExerciseHours(), dto.getLaboratoryHours());
        validateSctCredits(dto.getSctCredits());

        Set<CourseEntity> prerequisites = resolveAndValidatePrerequisites(
                dto.getStudyPlanId(), cleanCode, dto.getSemester(), dto.getPrerequisiteCodes());

        CourseEntity course = CourseEntity.builder()
                .code(cleanCode)
                .name(dto.getName().trim())
                .semester(dto.getSemester())
                .theoryHours(dto.getTheoryHours())
                .exerciseHours(dto.getExerciseHours())
                .laboratoryHours(dto.getLaboratoryHours())
                .sctCredits(dto.getSctCredits())
                .studyPlan(studyPlan)
                .prerequisites(prerequisites)
                .build();

        CourseEntity savedCourse = courseRepository.save(course);
        log.info("Course {} ('{}') successfully registered in study plan {}",
                savedCourse.getCode(), savedCourse.getName(), studyPlan.getCode());

        return mapToDTO(savedCourse);
    }

    /**
     * Updates an existing course's details and prerequisites.
     *
     * @param id course ID to update
     * @param dto update payload
     * @return updated course response DTO
     */
    @Transactional
    public CourseResponseDTO updateCourse(Long id, CourseUpdateDTO dto) {
        CourseEntity course = courseRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Course not found with ID: " + id));

        validateSemester(dto.getSemester());
        validateTelHours(dto.getTheoryHours(), dto.getExerciseHours(), dto.getLaboratoryHours());
        validateSctCredits(dto.getSctCredits());

        Set<CourseEntity> prerequisites = resolveAndValidatePrerequisites(
                course.getStudyPlan().getId(), course.getCode(), dto.getSemester(), dto.getPrerequisiteCodes());

        course.setName(dto.getName().trim());
        course.setSemester(dto.getSemester());
        course.setTheoryHours(dto.getTheoryHours());
        course.setExerciseHours(dto.getExerciseHours());
        course.setLaboratoryHours(dto.getLaboratoryHours());
        course.setSctCredits(dto.getSctCredits());
        course.setPrerequisites(prerequisites);

        CourseEntity updatedCourse = courseRepository.save(course);
        log.info("Course {} updated successfully", updatedCourse.getCode());

        return mapToDTO(updatedCourse);
    }

    /**
     * Physically deletes a course if it has no dependent prerequisite relationships.
     *
     * @param id course ID to delete
     */
    @Transactional
    public void deleteCourse(Long id) {
        CourseEntity course = courseRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Course not found with ID: " + id));

        long dependents = courseRepository.countByPrerequisitesContaining(course);
        if (dependents > 0) {
            throw new BusinessRuleException("Cannot delete course '" + course.getCode()
                    + "' because it is a prerequisite for " + dependents + " other course(s).");
        }

        courseRepository.delete(course);
        log.info("Course {} deleted successfully", course.getCode());
    }

    private void validateSemester(Integer semester) {
        if (semester == null || semester < 1 || semester > 4) {
            throw new BusinessRuleException("Course semester must be between 1 and 4.");
        }
    }

    private void validateTelHours(Integer theory, Integer exercise, Integer lab) {
        if (theory == null || theory < 0 || exercise == null || exercise < 0 || lab == null || lab < 0) {
            throw new BusinessRuleException("Theory, exercise, and laboratory hours cannot be negative.");
        }
        int total = theory + exercise + lab;
        if (total >= 8) {
            throw new BusinessRuleException("Total weekly TEL hours must be less than 8 (current: " + total + ").");
        }
        if (total <= 0) {
            throw new BusinessRuleException("Total weekly TEL hours must be greater than 0.");
        }
    }

    private void validateSctCredits(Integer sct) {
        if (sct == null || sct < 1 || sct > 7) {
            throw new BusinessRuleException("SCT credits must be between 1 and 7.");
        }
    }

    private Set<CourseEntity> resolveAndValidatePrerequisites(
            Long studyPlanId, String courseCode, Integer courseSemester, List<String> prerequisiteCodes) {

        Set<CourseEntity> prerequisites = new HashSet<>();

        if (prerequisiteCodes == null || prerequisiteCodes.isEmpty()) {
            return prerequisites;
        }

        if (prerequisiteCodes.size() > 3) {
            throw new BusinessRuleException("A course can have at most 3 prerequisites (received "
                    + prerequisiteCodes.size() + ").");
        }

        for (String rawCode : prerequisiteCodes) {
            if (rawCode == null || rawCode.isBlank()) continue;
            String cleanCode = rawCode.trim().toUpperCase();

            if (cleanCode.equalsIgnoreCase(courseCode)) {
                throw new BusinessRuleException("A course cannot have itself as a prerequisite.");
            }

            CourseEntity prereqCourse = courseRepository.findByStudyPlan_IdAndCode(studyPlanId, cleanCode)
                    .orElseThrow(() -> new BusinessRuleException("Prerequisite course '" + cleanCode
                            + "' does not exist in this study plan."));

            if (prereqCourse.getSemester() >= courseSemester) {
                throw new BusinessRuleException("Prerequisite '" + cleanCode + "' belongs to semester "
                        + prereqCourse.getSemester() + ", but course is in semester " + courseSemester
                        + ". Prerequisites must belong to an earlier semester.");
            }

            prerequisites.add(prereqCourse);
        }

        return prerequisites;
    }

    private CourseResponseDTO mapToDTO(CourseEntity entity) {
        int totalHours = entity.getTheoryHours() + entity.getExerciseHours() + entity.getLaboratoryHours();
        String tel = String.format("%d-%d-%d", entity.getTheoryHours(), entity.getExerciseHours(), entity.getLaboratoryHours());

        List<CoursePrerequisiteDTO> prereqDTOs = new ArrayList<>();
        List<String> prereqCodes = new ArrayList<>();

        if (entity.getPrerequisites() != null) {
            for (CourseEntity p : entity.getPrerequisites()) {
                prereqDTOs.add(CoursePrerequisiteDTO.builder()
                        .id(p.getId())
                        .code(p.getCode())
                        .name(p.getName())
                        .semester(p.getSemester())
                        .build());
                prereqCodes.add(p.getCode());
            }
        }

        return CourseResponseDTO.builder()
                .id(entity.getId())
                .code(entity.getCode())
                .name(entity.getName())
                .semester(entity.getSemester())
                .theoryHours(entity.getTheoryHours())
                .exerciseHours(entity.getExerciseHours())
                .laboratoryHours(entity.getLaboratoryHours())
                .totalHours(totalHours)
                .tel(tel)
                .sctCredits(entity.getSctCredits())
                .studyPlanId(entity.getStudyPlan().getId())
                .studyPlanCode(entity.getStudyPlan().getCode())
                .careerCode(entity.getStudyPlan().getCareer() != null ? entity.getStudyPlan().getCareer().getCode() : null)
                .careerName(entity.getStudyPlan().getCareer() != null ? entity.getStudyPlan().getCareer().getName() : null)
                .prerequisites(prereqDTOs)
                .prerequisiteCodes(prereqCodes)
                .build();
    }
}
