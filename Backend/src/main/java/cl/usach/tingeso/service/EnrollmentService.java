package cl.usach.tingeso.service;

import cl.usach.tingeso.dto.AvailableCourseOfferDTO;
import cl.usach.tingeso.dto.EnrolledStudentDTO;
import cl.usach.tingeso.dto.EnrollmentRequestDTO;
import cl.usach.tingeso.dto.EnrollmentResponseDTO;
import cl.usach.tingeso.entity.AcademicPeriodEntity;
import cl.usach.tingeso.entity.AcademicStatus;
import cl.usach.tingeso.entity.CourseEntity;
import cl.usach.tingeso.entity.EnrollmentEntity;
import cl.usach.tingeso.entity.EnrollmentStatus;
import cl.usach.tingeso.entity.SectionEntity;
import cl.usach.tingeso.entity.StudentEntity;
import cl.usach.tingeso.entity.TimeBlockEntity;
import cl.usach.tingeso.exception.BusinessRuleException;
import cl.usach.tingeso.exception.ResourceNotFoundException;
import cl.usach.tingeso.repository.AcademicPeriodRepository;
import cl.usach.tingeso.repository.CourseRepository;
import cl.usach.tingeso.repository.EnrollmentRepository;
import cl.usach.tingeso.repository.SectionRepository;
import cl.usach.tingeso.repository.StudentRepository;
import cl.usach.tingeso.util.RunValidatorUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * EnrollmentService coordinates course registration, validation of eligibility,
 * prerequisites, capacity, schedules, and progression rules for students.
 * Corresponds to Epic 6: Academic Enrollment.
 * Layer: Service.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class EnrollmentService {

    private final EnrollmentRepository enrollmentRepository;
    private final StudentRepository studentRepository;
    private final SectionRepository sectionRepository;
    private final CourseRepository courseRepository;
    private final AcademicPeriodRepository academicPeriodRepository;

    /**
     * Registers a student into a set of sections for the active academic period.
     * Enforces all Epic 6 business rules:
     * 1. Academic period must be OPEN.
     * 2. Student must be REGULAR and isMatriculated == true.
     * 3. Courses must belong to student's study plan.
     * 4. Approved courses cannot be enrolled again.
     * 5. All prerequisites must be APPROVED.
     * 6. Same course cannot be enrolled more than once in the same period.
     * 7. Must enroll between 3 and 6 courses (or all remaining if < 3).
     * 8. Lower semester progression rule: must include pending lower-semester
     * courses.
     * 9. Section capacity must be available (> 0).
     * 10. No schedule / timetable conflicts among selected sections.
     *
     * @param request               enrollment payload with section IDs
     * @param authenticatedUsername username (RUN) from JWT token
     * @param isAdmin               flag if authenticated user has ADMIN role
     * @return list of confirmed enrollment responses
     */
    @Transactional
    public List<EnrollmentResponseDTO> registerEnrollment(EnrollmentRequestDTO request, String authenticatedUsername,
            boolean isAdmin) {
        log.info("Processing enrollment request for user: {}, isAdmin: {}", authenticatedUsername, isAdmin);

        // 1. Period check: Must be OPEN
        AcademicPeriodEntity openPeriod = academicPeriodRepository.findByIsOpenTrue()
                .orElseThrow(() -> new BusinessRuleException(
                        "No hay ningún período académico ABIERTO para realizar inscripciones."));

        // 2. Determine target student RUN
        String targetRun = resolveTargetStudentRun(request.getStudentRun(), authenticatedUsername, isAdmin);
        String cleanedRun = RunValidatorUtil.cleanFormat(targetRun);

        StudentEntity student = studentRepository.findByRun(cleanedRun)
                .orElseThrow(() -> new ResourceNotFoundException("Student not found with RUN: " + cleanedRun));

        // 3. Eligibility check: REGULAR and Matriculated
        if (student.getAcademicStatus() != AcademicStatus.REGULAR) {
            throw new BusinessRuleException("El estudiante no tiene estado académico REGULAR (estado actual: "
                    + student.getAcademicStatus() + "). Solo estudiantes regulares pueden inscribir asignaturas.");
        }

        if (!Boolean.TRUE.equals(student.getIsMatriculated())) {
            throw new BusinessRuleException(
                    "El estudiante no se encuentra matriculado. Solo estudiantes matriculados pueden inscribir asignaturas.");
        }

        if (request.getSectionIds() == null || request.getSectionIds().isEmpty()) {
            throw new BusinessRuleException("Debe seleccionar al menos una asignatura para inscribir.");
        }

        // Deduplicate section IDs in request
        Set<Long> uniqueSectionIds = new LinkedHashSet<>(request.getSectionIds());

        // 4. Retrieve student's academic history
        List<EnrollmentEntity> studentEnrollments = enrollmentRepository.findByStudent_Run(cleanedRun);

        Set<String> approvedCourseCodes = studentEnrollments.stream()
                .filter(e -> e.getStatus() == EnrollmentStatus.APROBADA)
                .map(e -> e.getCourse().getCode())
                .collect(Collectors.toSet());

        List<EnrollmentEntity> currentActiveEnrollments = studentEnrollments.stream()
                .filter(e -> e.getStatus() == EnrollmentStatus.INSCRITA
                        && e.getAcademicPeriod().getId().equals(openPeriod.getId()))
                .toList();

        Set<String> currentlyEnrolledCourseCodes = currentActiveEnrollments.stream()
                .map(e -> e.getCourse().getCode())
                .collect(Collectors.toSet());

        // 5. Load and validate requested sections
        List<SectionEntity> requestedSections = new ArrayList<>();
        Set<String> selectedCourseCodes = new HashSet<>();

        for (Long secId : uniqueSectionIds) {
            SectionEntity section = sectionRepository.findById(secId)
                    .orElseThrow(() -> new ResourceNotFoundException("Section not found with ID: " + secId));

            if (!section.getAcademicPeriod().getId().equals(openPeriod.getId())) {
                throw new BusinessRuleException("La sección " + section.getSectionCode()
                        + " no corresponde al período académico abierto (" + openPeriod.getId() + ").");
            }

            CourseEntity course = section.getCourse();

            // Check study plan
            if (student.getStudyPlan() == null
                    || !course.getStudyPlan().getId().equals(student.getStudyPlan().getId())) {
                throw new BusinessRuleException("La asignatura " + course.getName() + " (" + course.getCode()
                        + ") no pertenece al plan de estudios del estudiante.");
            }

            // Check already approved
            if (approvedCourseCodes.contains(course.getCode())) {
                throw new BusinessRuleException("La asignatura " + course.getName() + " (" + course.getCode()
                        + ") ya fue aprobada y no puede ser inscrita nuevamente.");
            }

            // Check already enrolled in current period
            if (currentlyEnrolledCourseCodes.contains(course.getCode())) {
                throw new BusinessRuleException("La asignatura " + course.getName() + " (" + course.getCode()
                        + ") ya se encuentra inscrita en el período académico actual.");
            }

            // Check duplicate in same request
            if (!selectedCourseCodes.add(course.getCode())) {
                throw new BusinessRuleException(
                        "No puede seleccionar la misma asignatura más de una vez en la solicitud: " + course.getCode());
            }

            // Check prerequisites
            if (course.getPrerequisites() != null && !course.getPrerequisites().isEmpty()) {
                for (CourseEntity prereq : course.getPrerequisites()) {
                    if (!approvedCourseCodes.contains(prereq.getCode())) {
                        throw new BusinessRuleException(
                                "No cumple con los prerrequisitos para inscribir " + course.getName() + " ("
                                        + course.getCode() + "). Prerrequisito pendiente no aprobado: "
                                        + prereq.getName() + " (" + prereq.getCode() + ").");
                    }
                }
            }

            // Check capacity
            if (section.getEnrolledCount() >= section.getCapacity()) {
                throw new BusinessRuleException("La sección " + section.getSectionCode() + " de la asignatura "
                        + course.getName() + " no tiene cupos disponibles.");
            }

            requestedSections.add(section);
        }

        // 6. Check limits: Min 3, Max 6 (or remaining courses if < 3)
        List<CourseEntity> allPlanCourses = courseRepository.findByStudyPlan_Id(student.getStudyPlan().getId());
        long remainingPlanCourses = allPlanCourses.stream()
                .filter(c -> !approvedCourseCodes.contains(c.getCode()))
                .count();

        int totalResultingCourses = currentlyEnrolledCourseCodes.size() + requestedSections.size();

        if (remainingPlanCourses < 3) {
            if (totalResultingCourses < 1 || totalResultingCourses > remainingPlanCourses) {
                throw new BusinessRuleException("Al estudiante le restan " + remainingPlanCourses
                        + " asignaturas para completar su plan. Puede inscribir entre 1 y " + remainingPlanCourses
                        + " asignaturas.");
            }
        } else {
            if (totalResultingCourses < 3 || totalResultingCourses > 6) {
                throw new BusinessRuleException(
                        "El estudiante debe inscribir un mínimo de 3 y un máximo de 6 asignaturas en el período académico actual (total seleccionado: "
                                + totalResultingCourses + ").");
            }
        }

        // 7. Check Lower Semester Progression Rule
        Set<String> allActiveCourseCodes = Stream
                .concat(currentlyEnrolledCourseCodes.stream(), selectedCourseCodes.stream())
                .collect(Collectors.toSet());

        int maxSelectedSemester = Stream.concat(
                currentActiveEnrollments.stream().map(e -> e.getCourse().getSemester()),
                requestedSections.stream().map(s -> s.getCourse().getSemester())).max(Integer::compareTo).orElse(1);

        for (CourseEntity planCourse : allPlanCourses) {
            if (planCourse.getSemester() < maxSelectedSemester && !approvedCourseCodes.contains(planCourse.getCode())) {
                // Check if prerequisites for this lower course are met
                boolean prereqsMet = planCourse.getPrerequisites() == null || planCourse.getPrerequisites().stream()
                        .allMatch(p -> approvedCourseCodes.contains(p.getCode()));

                if (prereqsMet) {
                    // Check if an open section with quota exists for this lower course
                    Optional<SectionEntity> openSectionOpt = sectionRepository
                            .findByCourse_IdAndAcademicPeriod_Id(planCourse.getId(), openPeriod.getId());
                    if (openSectionOpt.isPresent()
                            && openSectionOpt.get().getEnrolledCount() < openSectionOpt.get().getCapacity()) {
                        // It was available for registration! Must be included
                        if (!allActiveCourseCodes.contains(planCourse.getCode())) {
                            throw new BusinessRuleException(
                                    "No puede inscribir asignaturas de un semestre superior mientras tenga asignaturas pendientes disponibles de semestres inferiores. Debe incluir en su inscripción: "
                                            + planCourse.getName() + " (" + planCourse.getCode() + ", Semestre "
                                            + planCourse.getSemester() + ").");
                        }
                    }
                }
            }
        }

        // 8. Check Schedule / Timetable Conflicts
        Set<String> occupiedBlockCodes = new HashSet<>();
        // Add existing active blocks
        for (EnrollmentEntity activeEnrollment : currentActiveEnrollments) {
            if (activeEnrollment.getSection() != null && activeEnrollment.getSection().getTimeBlocks() != null) {
                for (TimeBlockEntity block : activeEnrollment.getSection().getTimeBlocks()) {
                    occupiedBlockCodes.add(block.getCode());
                }
            }
        }

        // Check each newly requested section
        for (SectionEntity section : requestedSections) {
            if (section.getTimeBlocks() != null) {
                for (TimeBlockEntity block : section.getTimeBlocks()) {
                    if (!occupiedBlockCodes.add(block.getCode())) {
                        throw new BusinessRuleException("Conflicto de horario detectado en el bloque " + block.getCode()
                                + " (" + block.getDayOfWeek() + " Módulo " + block.getModuleNumber()
                                + ") para la asignatura "
                                + section.getCourse().getName() + ".");
                    }
                }
            }
        }

        // 9. Execute registrations and update capacities
        List<EnrollmentEntity> createdEnrollments = new ArrayList<>();
        LocalDateTime now = LocalDateTime.now();

        for (SectionEntity section : requestedSections) {
            section.setEnrolledCount(section.getEnrolledCount() + 1);
            sectionRepository.save(section);

            EnrollmentEntity enrollment = EnrollmentEntity.builder()
                    .student(student)
                    .course(section.getCourse())
                    .section(section)
                    .academicPeriod(openPeriod)
                    .enrollmentDate(now)
                    .status(EnrollmentStatus.INSCRITA)
                    .build();

            createdEnrollments.add(enrollmentRepository.save(enrollment));
        }

        log.info("Successfully enrolled student {} in {} sections for period {}", cleanedRun, createdEnrollments.size(),
                openPeriod.getId());
        return createdEnrollments.stream().map(this::mapToResponseDTO).toList();
    }

    /**
     * Cancels an active course registration during an open academic period.
     * Releases seat capacity back to the section.
     *
     * @param enrollmentId          ID of the enrollment to cancel
     * @param authenticatedUsername username (RUN) from JWT token
     * @param isAdmin               flag if user is ADMIN
     * @return updated enrollment response
     */
    @Transactional
    public EnrollmentResponseDTO cancelEnrollment(Long enrollmentId, String authenticatedUsername, boolean isAdmin) {
        log.info("Processing cancellation request for enrollment ID: {}, user: {}", enrollmentId,
                authenticatedUsername);

        AcademicPeriodEntity openPeriod = academicPeriodRepository.findByIsOpenTrue()
                .orElseThrow(() -> new BusinessRuleException(
                        "No hay ningún período académico ABIERTO. Las anulaciones solo se pueden realizar durante un período académico ABIERTO."));

        EnrollmentEntity enrollment = enrollmentRepository.findById(enrollmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Enrollment not found with ID: " + enrollmentId));

        if (!enrollment.getAcademicPeriod().getId().equals(openPeriod.getId())) {
            throw new BusinessRuleException(
                    "Solo se pueden anular inscripciones correspondientes al período académico actual abierto ("
                            + openPeriod.getId() + ").");
        }

        if (enrollment.getStatus() != EnrollmentStatus.INSCRITA) {
            throw new BusinessRuleException("Solo se pueden anular asignaturas con estado INSCRITA.");
        }

        if (!isAdmin) {
            String cleanedUsername = RunValidatorUtil.cleanFormat(authenticatedUsername);
            if (!enrollment.getStudent().getRun().equals(cleanedUsername)) {
                throw new BusinessRuleException("No tiene permisos para anular una inscripción de otro estudiante.");
            }
        }

        // Release seat in section
        SectionEntity section = enrollment.getSection();
        if (section != null && section.getEnrolledCount() > 0) {
            section.setEnrolledCount(section.getEnrolledCount() - 1);
            sectionRepository.save(section);
        }

        enrollment.setStatus(EnrollmentStatus.ANULADA);
        enrollment.setCancellationDate(LocalDateTime.now());
        EnrollmentEntity saved = enrollmentRepository.save(enrollment);

        log.info("Enrollment ID {} successfully cancelled. Released 1 quota for section {}", enrollmentId,
                section != null ? section.getSectionCode() : "N/A");
        return mapToResponseDTO(saved);
    }

    /**
     * Retrieves the available academic offer for a student, annotated with
     * eligibility flags.
     *
     * @param studentRun student RUN
     * @return list of offer DTOs
     */
    @Transactional(readOnly = true)
    public List<AvailableCourseOfferDTO> getAvailableOfferForStudent(String studentRun) {
        String cleaned = RunValidatorUtil.cleanFormat(studentRun);
        StudentEntity student = studentRepository.findByRun(cleaned)
                .orElseThrow(() -> new ResourceNotFoundException("Student not found with RUN: " + cleaned));

        Optional<AcademicPeriodEntity> openPeriodOpt = academicPeriodRepository.findByIsOpenTrue();
        if (openPeriodOpt.isEmpty()) {
            return List.of();
        }
        AcademicPeriodEntity openPeriod = openPeriodOpt.get();

        if (student.getStudyPlan() == null) {
            return List.of();
        }

        List<CourseEntity> planCourses = courseRepository.findByStudyPlan_Id(student.getStudyPlan().getId());
        List<EnrollmentEntity> studentEnrollments = enrollmentRepository.findByStudent_Run(cleaned);

        Set<String> approvedCourseCodes = studentEnrollments.stream()
                .filter(e -> e.getStatus() == EnrollmentStatus.APROBADA)
                .map(e -> e.getCourse().getCode())
                .collect(Collectors.toSet());

        Set<String> currentlyEnrolledCodes = studentEnrollments.stream()
                .filter(e -> e.getStatus() == EnrollmentStatus.INSCRITA
                        && e.getAcademicPeriod().getId().equals(openPeriod.getId()))
                .map(e -> e.getCourse().getCode())
                .collect(Collectors.toSet());

        List<AvailableCourseOfferDTO> result = new ArrayList<>();

        for (CourseEntity course : planCourses) {
            Optional<SectionEntity> sectionOpt = sectionRepository.findByCourse_IdAndAcademicPeriod_Id(course.getId(),
                    openPeriod.getId());

            List<String> prereqCodes = course.getPrerequisites() != null
                    ? course.getPrerequisites().stream().map(CourseEntity::getCode).toList()
                    : List.of();
            List<String> prereqNames = course.getPrerequisites() != null
                    ? course.getPrerequisites().stream().map(CourseEntity::getName).toList()
                    : List.of();

            boolean isAlreadyApproved = approvedCourseCodes.contains(course.getCode());
            boolean isCurrentlyEnrolled = currentlyEnrolledCodes.contains(course.getCode());
            boolean isPrerequisitesMet = course.getPrerequisites() == null || course.getPrerequisites().stream()
                    .allMatch(p -> approvedCourseCodes.contains(p.getCode()));
            boolean isOffered = sectionOpt.isPresent();

            boolean hasQuota = false;
            Long sectionId = null;
            String sectionCode = null;
            String teacherRun = null;
            String teacherName = null;
            Integer capacity = null;
            Integer enrolledCount = null;
            Integer availableQuota = null;
            List<String> timeBlocks = List.of();

            if (sectionOpt.isPresent()) {
                SectionEntity sec = sectionOpt.get();
                sectionId = sec.getId();
                sectionCode = sec.getSectionCode();
                teacherRun = sec.getTeacher() != null ? sec.getTeacher().getRun() : null;
                teacherName = sec.getTeacher() != null
                        ? sec.getTeacher().getFirstName() + " " + sec.getTeacher().getPaternalLastName()
                        : null;
                capacity = sec.getCapacity();
                enrolledCount = sec.getEnrolledCount();
                availableQuota = Math.max(0, capacity - enrolledCount);
                hasQuota = availableQuota > 0;
                timeBlocks = sec.getTimeBlocks() != null
                        ? sec.getTimeBlocks().stream().map(TimeBlockEntity::getCode).sorted().toList()
                        : List.of();
            }

            boolean isSelectable = false;
            String ineligibilityReason = null;

            if (isAlreadyApproved) {
                ineligibilityReason = "Asignatura ya aprobada en períodos anteriores.";
            } else if (isCurrentlyEnrolled) {
                ineligibilityReason = "Asignatura actualmente inscrita.";
            } else if (!isOffered) {
                ineligibilityReason = "No hay sección ofertada en el período académico actual.";
            } else if (!isPrerequisitesMet) {
                ineligibilityReason = "No cumple con los prerrequisitos requeridos.";
            } else if (!hasQuota) {
                ineligibilityReason = "Sección sin cupos disponibles.";
            } else {
                isSelectable = true;
            }

            int telTotal = (course.getTheoryHours() != null ? course.getTheoryHours() : 0)
                    + (course.getExerciseHours() != null ? course.getExerciseHours() : 0)
                    + (course.getLaboratoryHours() != null ? course.getLaboratoryHours() : 0);

            result.add(AvailableCourseOfferDTO.builder()
                    .courseId(course.getId())
                    .courseCode(course.getCode())
                    .courseName(course.getName())
                    .semester(course.getSemester())
                    .sctCredits(course.getSctCredits())
                    .theoryHours(course.getTheoryHours())
                    .exerciseHours(course.getExerciseHours())
                    .laboratoryHours(course.getLaboratoryHours())
                    .totalTelHours(telTotal)
                    .prerequisiteCodes(prereqCodes)
                    .prerequisiteNames(prereqNames)
                    .sectionId(sectionId)
                    .sectionCode(sectionCode)
                    .teacherRun(teacherRun)
                    .teacherName(teacherName)
                    .capacity(capacity)
                    .enrolledCount(enrolledCount)
                    .availableQuota(availableQuota)
                    .timeBlocks(timeBlocks)
                    .isAlreadyApproved(isAlreadyApproved)
                    .isCurrentlyEnrolled(isCurrentlyEnrolled)
                    .isPrerequisitesMet(isPrerequisitesMet)
                    .isOffered(isOffered)
                    .isSelectable(isSelectable)
                    .ineligibilityReason(ineligibilityReason)
                    .build());
        }

        result.sort(Comparator.comparing(AvailableCourseOfferDTO::getSemester)
                .thenComparing(AvailableCourseOfferDTO::getCourseCode));
        return result;
    }

    /**
     * Retrieves active enrollments for the current student in the open period.
     */
    @Transactional(readOnly = true)
    public List<EnrollmentResponseDTO> getActiveEnrollments(String studentRun) {
        String cleaned = RunValidatorUtil.cleanFormat(studentRun);
        Optional<AcademicPeriodEntity> openPeriodOpt = academicPeriodRepository.findByIsOpenTrue();
        if (openPeriodOpt.isEmpty()) {
            return List.of();
        }
        return enrollmentRepository.findByStudent_RunAndAcademicPeriod_IdAndStatus(
                cleaned, openPeriodOpt.get().getId(), EnrollmentStatus.INSCRITA)
                .stream().map(this::mapToResponseDTO).toList();
    }

    /**
     * Retrieves all enrollments (active and historical) of a student.
     */
    @Transactional(readOnly = true)
    public List<EnrollmentResponseDTO> getAllStudentEnrollments(String studentRun) {
        String cleaned = RunValidatorUtil.cleanFormat(studentRun);
        return enrollmentRepository.findByStudent_Run(cleaned)
                .stream().map(this::mapToResponseDTO).toList();
    }

    /**
     * Retrieves the list of enrolled students for a specific section.
     * Enforces that only the assigned teacher or ADMIN can access.
     */
    @Transactional(readOnly = true)
    public List<EnrolledStudentDTO> getEnrolledStudentsForSection(Long sectionId, String authenticatedUsername,
            boolean isAdmin) {
        SectionEntity section = sectionRepository.findById(sectionId)
                .orElseThrow(() -> new ResourceNotFoundException("Section not found with ID: " + sectionId));

        if (!isAdmin) {
            String cleanedUsername = RunValidatorUtil.cleanFormat(authenticatedUsername);
            if (section.getTeacher() == null || !section.getTeacher().getRun().equals(cleanedUsername)) {
                throw new BusinessRuleException(
                        "No tiene permisos para consultar los alumnos de una sección que no tiene asignada.");
            }
        }

        List<EnrollmentEntity> activeEnrollments = enrollmentRepository.findBySection_IdAndStatus(sectionId,
                EnrollmentStatus.INSCRITA);

        return activeEnrollments.stream().map(e -> {
            StudentEntity s = e.getStudent();
            String fullName = String.format("%s %s %s",
                    s.getFirstName() != null ? s.getFirstName().trim() : "",
                    s.getPaternalLastName() != null ? s.getPaternalLastName().trim() : "",
                    s.getMaternalLastName() != null ? s.getMaternalLastName().trim() : "").trim();

            return EnrolledStudentDTO.builder()
                    .run(s.getRun())
                    .firstName(s.getFirstName())
                    .paternalLastName(s.getPaternalLastName())
                    .maternalLastName(s.getMaternalLastName())
                    .fullName(fullName)
                    .email(s.getEmail())
                    .careerName(s.getCareer() != null ? s.getCareer().getName() : null)
                    .enrollmentDate(e.getEnrollmentDate())
                    .build();
        }).toList();
    }

    /**
     * Utility method to register past course approvals for test scenarios or
     * historical data.
     */
    @Transactional
    public EnrollmentResponseDTO seedPastApprovedCourse(String studentRun, String courseCode, String periodId,
            Double grade) {
        String cleaned = RunValidatorUtil.cleanFormat(studentRun);
        StudentEntity student = studentRepository.findByRun(cleaned)
                .orElseThrow(() -> new ResourceNotFoundException("Student not found with RUN: " + cleaned));

        CourseEntity course = courseRepository.findByCode(courseCode)
                .orElseThrow(() -> new ResourceNotFoundException("Course not found with code: " + courseCode));

        AcademicPeriodEntity period = academicPeriodRepository.findById(periodId)
                .orElseGet(() -> academicPeriodRepository.save(AcademicPeriodEntity.builder()
                        .id(periodId)
                        .year(Integer.parseInt(periodId.split("-")[0]))
                        .semester(Integer.parseInt(periodId.split("-")[1]))
                        .isOpen(false)
                        .build()));

        EnrollmentEntity enrollment = EnrollmentEntity.builder()
                .student(student)
                .course(course)
                .academicPeriod(period)
                .enrollmentDate(LocalDateTime.now().minusMonths(6))
                .status(grade >= 4.0 ? EnrollmentStatus.APROBADA : EnrollmentStatus.REPROBADA)
                .finalGrade(grade)
                .build();

        EnrollmentEntity saved = enrollmentRepository.save(enrollment);
        return mapToResponseDTO(saved);
    }

    private String resolveTargetStudentRun(String requestRun, String authenticatedUsername, boolean isAdmin) {
        if (isAdmin) {
            if (requestRun == null || requestRun.isBlank()) {
                throw new BusinessRuleException(
                        "El RUN del estudiante es obligatorio para inscripciones realizadas por el administrador.");
            }
            return requestRun;
        }
        return authenticatedUsername;
    }

    public EnrollmentResponseDTO mapToResponseDTO(EnrollmentEntity entity) {
        List<String> scheduleBlocks = List.of();
        String teacherRun = null;
        String teacherName = null;
        Long sectionId = null;
        String sectionCode = null;

        if (entity.getSection() != null) {
            sectionId = entity.getSection().getId();
            sectionCode = entity.getSection().getSectionCode();
            if (entity.getSection().getTeacher() != null) {
                teacherRun = entity.getSection().getTeacher().getRun();
                teacherName = entity.getSection().getTeacher().getFirstName() + " "
                        + entity.getSection().getTeacher().getPaternalLastName();
            }
            if (entity.getSection().getTimeBlocks() != null) {
                scheduleBlocks = entity.getSection().getTimeBlocks().stream()
                        .map(TimeBlockEntity::getCode)
                        .sorted()
                        .toList();
            }
        }

        StudentEntity s = entity.getStudent();
        String studentName = s != null
                ? String.format("%s %s %s", s.getFirstName(), s.getPaternalLastName(), s.getMaternalLastName()).trim()
                : null;

        CourseEntity c = entity.getCourse();

        return EnrollmentResponseDTO.builder()
                .id(entity.getId())
                .studentRun(s != null ? s.getRun() : null)
                .studentName(studentName)
                .courseId(c != null ? c.getId() : null)
                .courseCode(c != null ? c.getCode() : null)
                .courseName(c != null ? c.getName() : null)
                .semester(c != null ? c.getSemester() : null)
                .sctCredits(c != null ? c.getSctCredits() : null)
                .sectionId(sectionId)
                .sectionCode(sectionCode)
                .academicPeriodId(entity.getAcademicPeriod() != null ? entity.getAcademicPeriod().getId() : null)
                .teacherRun(teacherRun)
                .teacherName(teacherName)
                .scheduleTimeBlocks(scheduleBlocks)
                .enrollmentDate(entity.getEnrollmentDate())
                .cancellationDate(entity.getCancellationDate())
                .status(entity.getStatus())
                .finalGrade(entity.getFinalGrade())
                .build();
    }
}
