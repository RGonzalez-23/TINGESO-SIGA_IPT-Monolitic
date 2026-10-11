package cl.usach.tingeso.service;

import cl.usach.tingeso.dto.SectionCreateDTO;
import cl.usach.tingeso.dto.SectionResponseDTO;
import cl.usach.tingeso.dto.SectionUpdateDTO;
import cl.usach.tingeso.dto.TimeBlockResponseDTO;
import cl.usach.tingeso.entity.AcademicPeriodEntity;
import cl.usach.tingeso.entity.CourseEntity;
import cl.usach.tingeso.entity.SectionEntity;
import cl.usach.tingeso.entity.TeacherEntity;
import cl.usach.tingeso.entity.TimeBlockEntity;
import cl.usach.tingeso.exception.BusinessRuleException;
import cl.usach.tingeso.exception.ResourceNotFoundException;
import cl.usach.tingeso.repository.AcademicPeriodRepository;
import cl.usach.tingeso.repository.CourseRepository;
import cl.usach.tingeso.repository.SectionRepository;
import cl.usach.tingeso.repository.TeacherRepository;
import cl.usach.tingeso.repository.TimeBlockRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * SectionService implements all business rules for Academic Offer and Sections.
 * Enforces single section per subject per period, active teacher requirement,
 * exact TEL blocks calculation, timetable conflict prevention, and deletion guards.
 * Layer: Service.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class SectionService {

    private final SectionRepository sectionRepository;
    private final AcademicPeriodRepository academicPeriodRepository;
    private final CourseRepository courseRepository;
    private final TeacherRepository teacherRepository;
    private final TimeBlockRepository timeBlockRepository;
    private final TimeBlockService timeBlockService;

    /**
     * Retrieves sections with optional filters (periodId, studyPlanId, teacherRun).
     */
    @Transactional(readOnly = true)
    public List<SectionResponseDTO> getSections(String periodId, Long studyPlanId, String teacherRun) {
        List<SectionEntity> sections;

        if (periodId != null && !periodId.isBlank()) {
            if (studyPlanId != null) {
                sections = sectionRepository.findByAcademicPeriodIdAndCourseStudyPlanId(periodId, studyPlanId);
            } else if (teacherRun != null && !teacherRun.isBlank()) {
                sections = sectionRepository.findByTeacherRunAndAcademicPeriodId(teacherRun, periodId);
            } else {
                sections = sectionRepository.findByAcademicPeriodId(periodId);
            }
        } else {
            sections = sectionRepository.findAll();
        }

        return sections.stream()
                .map(this::toResponseDTO)
                .toList();
    }

    /**
     * Retrieves a single section by ID.
     */
    @Transactional(readOnly = true)
    public SectionResponseDTO getSectionById(Long id) {
        SectionEntity entity = sectionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Sección no encontrada con ID: " + id));
        return toResponseDTO(entity);
    }

    /**
     * Creates a new section for a course within an academic period.
     */
    @Transactional
    public SectionResponseDTO createSection(SectionCreateDTO dto) {
        AcademicPeriodEntity period = academicPeriodRepository.findById(dto.getAcademicPeriodId())
                .orElseThrow(() -> new ResourceNotFoundException("Período académico no encontrado con ID: " + dto.getAcademicPeriodId()));

        if (!period.getIsOpen()) {
            throw new BusinessRuleException("No es posible crear secciones en un período académico CERRADO.");
        }

        CourseEntity course = courseRepository.findById(dto.getCourseId())
                .orElseThrow(() -> new ResourceNotFoundException("Asignatura no encontrada con ID: " + dto.getCourseId()));

        // Unique section rule: Each course can have at most one section per academic period
        if (sectionRepository.existsByCourseIdAndAcademicPeriodId(course.getId(), period.getId())) {
            throw new BusinessRuleException(String.format(
                    "La asignatura '%s' (%s) ya cuenta con una sección en el período %s. Cada asignatura debe tener una ÚNICA sección por período.",
                    course.getCode(), course.getName(), period.getId()
            ));
        }

        TeacherEntity teacher = teacherRepository.findById(dto.getTeacherRun())
                .orElseThrow(() -> new ResourceNotFoundException("Docente no encontrado con RUN: " + dto.getTeacherRun()));

        if (!teacher.getIsActive()) {
            throw new BusinessRuleException(String.format(
                    "El docente '%s' (%s) se encuentra INACTIVO y no puede ser asignado a nuevas secciones.",
                    teacher.getFullName(), teacher.getRun()
            ));
        }

        // Validate TEL hours and blocks requirement
        int totalTEL = course.getTheoryHours() + course.getExerciseHours() + course.getLaboratoryHours();
        int requiredBlocks = Math.max(1, totalTEL / 2);

        if (dto.getTimeBlockCodes().size() != requiredBlocks) {
            throw new BusinessRuleException(String.format(
                    "La cantidad de bloques horarios asignados (%d) debe coincidir exactamente con el total de horas TEL de la asignatura (%d hrs TEL = %d bloques de 80 min).",
                    dto.getTimeBlockCodes().size(), totalTEL, requiredBlocks
            ));
        }

        // Load time blocks
        Set<TimeBlockEntity> blocks = new HashSet<>();
        for (String blockCode : dto.getTimeBlockCodes()) {
            TimeBlockEntity block = timeBlockRepository.findById(blockCode)
                    .orElseThrow(() -> new ResourceNotFoundException("Bloque horario no válido o inexistente: " + blockCode));
            blocks.add(block);
        }

        // Verify teacher schedule conflicts in the same period
        verifyTeacherConflicts(teacher, period.getId(), blocks, null);

        // Section code formatted strictly as "[COURSE_CODE]-A-1" (e.g. "TAP101-A-1")
        String sectionCode = String.format("%s-A-1", course.getCode());

        SectionEntity section = SectionEntity.builder()
                .sectionCode(sectionCode)
                .course(course)
                .academicPeriod(period)
                .teacher(teacher)
                .capacity(dto.getCapacity())
                .enrolledCount(0)
                .timeBlocks(blocks)
                .build();

        SectionEntity saved = sectionRepository.save(section);
        log.info("Section {} created successfully for course {} in period {}", sectionCode, course.getCode(), period.getId());
        return toResponseDTO(saved);
    }

    /**
     * Updates an existing section (teacher, capacity, time blocks).
     */
    @Transactional
    public SectionResponseDTO updateSection(Long id, SectionUpdateDTO dto) {
        SectionEntity section = sectionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Sección no encontrada con ID: " + id));

        if (!section.getAcademicPeriod().getIsOpen()) {
            throw new BusinessRuleException("No es posible modificar una sección de un período académico CERRADO.");
        }

        if (dto.getCapacity() < section.getEnrolledCount()) {
            throw new BusinessRuleException(String.format(
                    "La nueva capacidad (%d) no puede ser inferior al número de estudiantes ya inscritos (%d).",
                    dto.getCapacity(), section.getEnrolledCount()
            ));
        }

        TeacherEntity teacher = teacherRepository.findById(dto.getTeacherRun())
                .orElseThrow(() -> new ResourceNotFoundException("Docente no encontrado con RUN: " + dto.getTeacherRun()));

        if (!teacher.getIsActive()) {
            throw new BusinessRuleException(String.format(
                    "El docente '%s' (%s) se encuentra INACTIVO y no puede ser asignado.",
                    teacher.getFullName(), teacher.getRun()
            ));
        }

        CourseEntity course = section.getCourse();
        int totalTEL = course.getTheoryHours() + course.getExerciseHours() + course.getLaboratoryHours();
        int requiredBlocks = Math.max(1, totalTEL / 2);

        if (dto.getTimeBlockCodes().size() != requiredBlocks) {
            throw new BusinessRuleException(String.format(
                    "La cantidad de bloques horarios asignados (%d) debe coincidir exactamente con el total de horas TEL de la asignatura (%d hrs TEL = %d bloques de 80 min).",
                    dto.getTimeBlockCodes().size(), totalTEL, requiredBlocks
            ));
        }

        Set<TimeBlockEntity> blocks = new HashSet<>();
        for (String blockCode : dto.getTimeBlockCodes()) {
            TimeBlockEntity block = timeBlockRepository.findById(blockCode)
                    .orElseThrow(() -> new ResourceNotFoundException("Bloque horario no válido o inexistente: " + blockCode));
            blocks.add(block);
        }

        // Verify teacher conflicts excluding current section
        verifyTeacherConflicts(teacher, section.getAcademicPeriod().getId(), blocks, section.getId());

        section.setTeacher(teacher);
        section.setCapacity(dto.getCapacity());
        section.setTimeBlocks(blocks);

        SectionEntity updated = sectionRepository.save(section);
        log.info("Section {} (ID: {}) updated successfully", updated.getSectionCode(), id);
        return toResponseDTO(updated);
    }

    /**
     * Deletes a section physically. Prohibited if students are enrolled.
     */
    @Transactional
    public void deleteSection(Long id) {
        SectionEntity section = sectionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Sección no encontrada con ID: " + id));

        if (section.getEnrolledCount() > 0) {
            throw new BusinessRuleException(String.format(
                    "No es posible eliminar la sección '%s' porque cuenta con %d estudiantes inscritos.",
                    section.getSectionCode(), section.getEnrolledCount()
            ));
        }

        sectionRepository.delete(section);
        log.info("Section {} (ID: {}) deleted successfully", section.getSectionCode(), id);
    }

    /**
     * Checks if a teacher already has sections in the same academic period with overlapping time blocks.
     */
    private void verifyTeacherConflicts(TeacherEntity teacher, String periodId, Set<TimeBlockEntity> proposedBlocks, Long excludeSectionId) {
        List<SectionEntity> teacherSections = sectionRepository.findByTeacherRunAndAcademicPeriodId(teacher.getRun(), periodId);

        Set<String> proposedCodes = proposedBlocks.stream()
                .map(TimeBlockEntity::getCode)
                .collect(Collectors.toSet());

        for (SectionEntity existing : teacherSections) {
            if (excludeSectionId != null && existing.getId().equals(excludeSectionId)) {
                continue;
            }

            for (TimeBlockEntity existingBlock : existing.getTimeBlocks()) {
                if (proposedCodes.contains(existingBlock.getCode())) {
                    throw new BusinessRuleException(String.format(
                            "Conflicto de horario: El docente %s ya tiene asignada la asignatura '%s' (%s) en el bloque horario %s (%s Módulo %d).",
                            teacher.getFullName(),
                            existing.getCourse().getName(),
                            existing.getSectionCode(),
                            existingBlock.getCode(),
                            TimeBlockResponseDTO.getSpanishDayName(existingBlock.getDayOfWeek()),
                            existingBlock.getModuleNumber()
                    ));
                }
            }
        }
    }

    public SectionResponseDTO toResponseDTO(SectionEntity entity) {
        CourseEntity course = entity.getCourse();
        int totalTEL = course.getTheoryHours() + course.getExerciseHours() + course.getLaboratoryHours();

        List<TimeBlockResponseDTO> blockDTOs = entity.getTimeBlocks().stream()
                .sorted(Comparator.comparing(TimeBlockEntity::getDayOfWeek)
                        .thenComparing(TimeBlockEntity::getModuleNumber))
                .map(timeBlockService::toResponseDTO)
                .toList();

        String summary = blockDTOs.stream()
                .map(TimeBlockResponseDTO::getCode)
                .collect(Collectors.joining(", "));

        return SectionResponseDTO.builder()
                .id(entity.getId())
                .sectionCode(entity.getSectionCode())
                .courseId(course.getId())
                .courseCode(course.getCode())
                .courseName(course.getName())
                .semester(course.getSemester())
                .sctCredits(course.getSctCredits())
                .theoryHours(course.getTheoryHours())
                .exerciseHours(course.getExerciseHours())
                .laboratoryHours(course.getLaboratoryHours())
                .totalTELHours(totalTEL)
                .requiredBlocksCount(Math.max(1, totalTEL / 2))
                .studyPlanId(course.getStudyPlan() != null ? course.getStudyPlan().getId() : null)
                .studyPlanCode(course.getStudyPlan() != null ? course.getStudyPlan().getCode() : null)
                .careerCode(course.getStudyPlan() != null && course.getStudyPlan().getCareer() != null
                        ? course.getStudyPlan().getCareer().getCode() : null)
                .careerName(course.getStudyPlan() != null && course.getStudyPlan().getCareer() != null
                        ? course.getStudyPlan().getCareer().getName() : null)
                .academicPeriodId(entity.getAcademicPeriod().getId())
                .academicPeriodOpen(entity.getAcademicPeriod().getIsOpen())
                .teacherRun(entity.getTeacher().getRun())
                .teacherName(entity.getTeacher().getFullName())
                .capacity(entity.getCapacity())
                .enrolledCount(entity.getEnrolledCount())
                .availableQuota(entity.getCapacity() - entity.getEnrolledCount())
                .timeBlocks(blockDTOs)
                .scheduleSummary(summary)
                .build();
    }
}
