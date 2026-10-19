package cl.usach.tingeso.integration;

import cl.usach.tingeso.dto.AcademicPeriodCreateDTO;
import cl.usach.tingeso.dto.AcademicPeriodResponseDTO;
import cl.usach.tingeso.dto.SectionCreateDTO;
import cl.usach.tingeso.dto.SectionResponseDTO;
import cl.usach.tingeso.entity.AcademicDegree;
import cl.usach.tingeso.entity.AcademicPeriodEntity;
import cl.usach.tingeso.entity.CareerEntity;
import cl.usach.tingeso.entity.CourseEntity;
import cl.usach.tingeso.entity.SectionEntity;
import cl.usach.tingeso.entity.StudyPlanEntity;
import cl.usach.tingeso.entity.TeacherEntity;
import cl.usach.tingeso.exception.BusinessRuleException;
import cl.usach.tingeso.repository.AcademicPeriodRepository;
import cl.usach.tingeso.repository.CareerRepository;
import cl.usach.tingeso.repository.CourseRepository;
import cl.usach.tingeso.repository.SectionRepository;
import cl.usach.tingeso.repository.StudyPlanRepository;
import cl.usach.tingeso.repository.TeacherRepository;
import cl.usach.tingeso.service.AcademicPeriodService;
import cl.usach.tingeso.service.SectionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Integration test suite for Process 3: Academic Offer and Section Management (Epic 5).
 * Tests business rules for academic periods, single-open constraints, TEL block mapping,
 * teacher schedule collisions, and section creation integrity in database.
 * Includes 3 success scenarios and 3 rejection scenarios as mandated by the evaluation rubric.
 */
@SpringBootTest
@ActiveProfiles("test")
@TestPropertySource(locations = "classpath:application-test.properties")
@Transactional
public class AcademicOfferProcessIntegrationTest {

    @Autowired
    private AcademicPeriodService academicPeriodService;

    @Autowired
    private SectionService sectionService;

    @Autowired
    private AcademicPeriodRepository academicPeriodRepository;

    @Autowired
    private SectionRepository sectionRepository;

    @Autowired
    private CourseRepository courseRepository;

    @Autowired
    private TeacherRepository teacherRepository;

    @Autowired
    private CareerRepository careerRepository;

    @Autowired
    private StudyPlanRepository studyPlanRepository;

    private AcademicPeriodEntity initialPeriod;
    private StudyPlanEntity studyPlan;
    private TeacherEntity activeTeacher;

    @BeforeEach
    void setUp() {
        initialPeriod = academicPeriodRepository.findByIsOpenTrue()
                .orElseGet(() -> academicPeriodRepository.save(
                        AcademicPeriodEntity.builder().id("2027-1").year(2027).semester(1).isOpen(true).build()
                ));

        studyPlan = studyPlanRepository.findByCareer_CodeAndIsActiveTrue("10450")
                .orElseThrow(() -> new IllegalStateException("Study plan not found for 10450"));

        activeTeacher = teacherRepository.findById("11111111-1")
                .orElseThrow(() -> new IllegalStateException("Teacher 11111111-1 not found"));
    }

    private CourseEntity getCourse(String code) {
        return courseRepository.findByStudyPlan_IdAndCode(studyPlan.getId(), code)
                .orElseThrow(() -> new IllegalStateException("Course " + code + " not found"));
    }

    // =========================================================================
    // ESCENARIOS EXITOSOS (3 CASOS)
    // =========================================================================

    @Test
    @DisplayName("Éxito 1: Creación de nuevo período académico cierra automáticamente el anterior")
    void scenario01_successfulPeriodCreation_closesPreviousOpenPeriodAtomically() {
        assertTrue(initialPeriod.getIsOpen(), "Initial period 2027-1 should be OPEN");

        AcademicPeriodCreateDTO dto = AcademicPeriodCreateDTO.builder()
                .year(2027)
                .semester(2)
                .build();

        AcademicPeriodResponseDTO created = academicPeriodService.createPeriod(dto);

        assertNotNull(created);
        assertEquals("2027-2", created.getId());
        assertTrue(created.getIsOpen(), "New period 2027-2 must be OPEN");

        // Verify previous period 2027-1 is now CLOSED in database
        AcademicPeriodEntity prev = academicPeriodRepository.findById(initialPeriod.getId()).orElseThrow();
        assertFalse(prev.getIsOpen(), "Previous period 2027-1 must have been automatically closed");

        // Verify strictly only one period is open in the entire system
        assertEquals(1, academicPeriodRepository.findAllByIsOpenTrue().size());
    }

    @Test
    @DisplayName("Éxito 2: Creación exitosa de sección con docente activo y cantidad exacta de bloques TEL")
    void scenario02_successfulSectionCreation_withActiveTeacherAndExactTELBlocks() {
        CourseEntity course = getCourse("TAP101"); // TEL: 2-2-2 = 6 hrs -> requires 3 blocks
        int totalTEL = course.getTheoryHours() + course.getExerciseHours() + course.getLaboratoryHours();
        assertEquals(6, totalTEL);

        SectionCreateDTO dto = SectionCreateDTO.builder()
                .courseId(course.getId())
                .academicPeriodId(initialPeriod.getId())
                .teacherRun(activeTeacher.getRun())
                .capacity(35)
                .timeBlockCodes(Set.of("L1", "M1", "W1"))
                .build();

        SectionResponseDTO response = sectionService.createSection(dto);

        assertNotNull(response);
        assertEquals("TAP101-A-1", response.getSectionCode());
        assertEquals(35, response.getCapacity());
        assertEquals(0, response.getEnrolledCount());

        // Verify persistence in database
        SectionEntity saved = sectionRepository.findById(response.getId()).orElseThrow();
        assertEquals("TAP101-A-1", saved.getSectionCode());
        assertEquals(3, saved.getTimeBlocks().size());
    }

    @Test
    @DisplayName("Éxito 3: Alternancia de estado de período reabre período previo y cierra el activo")
    void scenario03_successfulPeriodToggle_reopensOrClosesPeriodExclusively() {
        // Create 2027-2 (which becomes open and closes 2027-1)
        academicPeriodService.createPeriod(AcademicPeriodCreateDTO.builder().year(2027).semester(2).build());

        AcademicPeriodEntity p1 = academicPeriodRepository.findById("2027-1").orElseThrow();
        AcademicPeriodEntity p2 = academicPeriodRepository.findById("2027-2").orElseThrow();
        assertFalse(p1.getIsOpen());
        assertTrue(p2.getIsOpen());

        // Toggle 2027-1 to open again
        academicPeriodService.togglePeriodStatus("2027-1");

        p1 = academicPeriodRepository.findById("2027-1").orElseThrow();
        p2 = academicPeriodRepository.findById("2027-2").orElseThrow();

        assertTrue(p1.getIsOpen(), "2027-1 should now be reopened");
        assertFalse(p2.getIsOpen(), "2027-2 should have been closed");
        assertEquals(1, academicPeriodRepository.findAllByIsOpenTrue().size());
    }

    // =========================================================================
    // ESCENARIOS DE RECHAZO (3 CASOS)
    // =========================================================================

    @Test
    @DisplayName("Rechazo 1: Asignación de docente con conflicto de horario en el mismo bloque es rechazada")
    void scenario04_rejection_teacherScheduleConflictInSamePeriod_throwsException() {
        CourseEntity c1 = getCourse("TAP101"); // TEL: 6 hrs -> 3 blocks
        CourseEntity c2 = getCourse("TAP102"); // TEL: 4 hrs -> 2 blocks

        // Section 1 for c1 with blocks L1, M1, W1 assigned to activeTeacher
        sectionService.createSection(SectionCreateDTO.builder()
                .courseId(c1.getId())
                .academicPeriodId(initialPeriod.getId())
                .teacherRun(activeTeacher.getRun())
                .capacity(30)
                .timeBlockCodes(Set.of("L1", "M1", "W1"))
                .build());

        // Attempt to create Section 2 for c2 with blocks L1, J1 assigned to the SAME teacher
        SectionCreateDTO conflictDto = SectionCreateDTO.builder()
                .courseId(c2.getId())
                .academicPeriodId(initialPeriod.getId())
                .teacherRun(activeTeacher.getRun())
                .capacity(30)
                .timeBlockCodes(Set.of("L1", "J1")) // L1 collides!
                .build();

        BusinessRuleException exception = assertThrows(BusinessRuleException.class, () ->
                sectionService.createSection(conflictDto));

        assertTrue(exception.getMessage().contains("conflicto de horario") || exception.getMessage().contains("L1"));
    }

    @Test
    @DisplayName("Rechazo 2: Intento de crear dos secciones para la misma asignatura en un mismo período es rechazado")
    void scenario05_rejection_duplicateSectionForSameCourseInPeriod_throwsException() {
        CourseEntity course = getCourse("TAP101");

        // First section succeeds
        sectionService.createSection(SectionCreateDTO.builder()
                .courseId(course.getId())
                .academicPeriodId(initialPeriod.getId())
                .teacherRun(activeTeacher.getRun())
                .capacity(30)
                .timeBlockCodes(Set.of("L1", "M1", "W1"))
                .build());

        // Second section for the exact same course and period
        TeacherEntity otherTeacher = teacherRepository.save(TeacherEntity.builder()
                .run("22222222-2")
                .firstName("Ana")
                .paternalLastName("Torres")
                .maternalLastName("Reyes")
                .email("ana.torres@sigaipt.cl")
                .professionalTitle("Ingeniera Informática")
                .academicDegree(AcademicDegree.LICENCIATURA)
                .isActive(true)
                .build());

        SectionCreateDTO duplicateDto = SectionCreateDTO.builder()
                .courseId(course.getId())
                .academicPeriodId(initialPeriod.getId())
                .teacherRun(otherTeacher.getRun())
                .capacity(30)
                .timeBlockCodes(Set.of("L2", "M2", "W2"))
                .build();

        BusinessRuleException exception = assertThrows(BusinessRuleException.class, () ->
                sectionService.createSection(duplicateDto));

        assertTrue(exception.getMessage().contains("ya cuenta con una sección en el período"));
    }

    @Test
    @DisplayName("Rechazo 3: Asignación de docente en estado INACTIVO a una sección es rechazada")
    void scenario06_rejection_assignInactiveTeacherToSection_throwsException() {
        CourseEntity course = getCourse("TAP103"); // TEL: 4 hrs -> 2 blocks

        TeacherEntity inactiveTeacher = teacherRepository.save(TeacherEntity.builder()
                .run("33333333-3")
                .firstName("Carlos")
                .paternalLastName("Inactivo")
                .maternalLastName("Vega")
                .email("carlos.inactivo@sigaipt.cl")
                .professionalTitle("Ingeniero de Software")
                .academicDegree(AcademicDegree.MAGISTER)
                .isActive(false) // INACTIVE
                .build());

        SectionCreateDTO dto = SectionCreateDTO.builder()
                .courseId(course.getId())
                .academicPeriodId(initialPeriod.getId())
                .teacherRun(inactiveTeacher.getRun())
                .capacity(30)
                .timeBlockCodes(Set.of("L3", "M3"))
                .build();

        BusinessRuleException exception = assertThrows(BusinessRuleException.class, () ->
                sectionService.createSection(dto));

        assertTrue(exception.getMessage().contains("INACTIVO"));
    }
}
