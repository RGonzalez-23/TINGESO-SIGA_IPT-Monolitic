package cl.usach.tingeso.service;

import cl.usach.tingeso.dto.SectionCreateDTO;
import cl.usach.tingeso.dto.SectionResponseDTO;
import cl.usach.tingeso.dto.SectionUpdateDTO;
import cl.usach.tingeso.dto.TimeBlockResponseDTO;
import cl.usach.tingeso.entity.AcademicDegree;
import cl.usach.tingeso.entity.AcademicPeriodEntity;
import cl.usach.tingeso.entity.CareerEntity;
import cl.usach.tingeso.entity.CourseEntity;
import cl.usach.tingeso.entity.SectionEntity;
import cl.usach.tingeso.entity.StudyPlanEntity;
import cl.usach.tingeso.entity.TeacherEntity;
import cl.usach.tingeso.entity.TimeBlockEntity;
import cl.usach.tingeso.exception.BusinessRuleException;
import cl.usach.tingeso.exception.ResourceNotFoundException;
import cl.usach.tingeso.repository.AcademicPeriodRepository;
import cl.usach.tingeso.repository.CourseRepository;
import cl.usach.tingeso.repository.SectionRepository;
import cl.usach.tingeso.repository.TeacherRepository;
import cl.usach.tingeso.repository.TimeBlockRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalTime;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for SectionService.
 * Layer: Service.
 * Verifies all rules for course sections:
 * - Single section per subject per period
 * - Code format "[COURSE_CODE]-A-1"
 * - Teacher availability and timetable conflicts
 * - Exact TEL block allocation (TEL / 2)
 * - Deletion protection when students are enrolled
 */
@ExtendWith(MockitoExtension.class)
class SectionServiceTest {

    @Mock
    private SectionRepository sectionRepository;

    @Mock
    private AcademicPeriodRepository academicPeriodRepository;

    @Mock
    private CourseRepository courseRepository;

    @Mock
    private TeacherRepository teacherRepository;

    @Mock
    private TimeBlockRepository timeBlockRepository;

    @Mock
    private TimeBlockService timeBlockService;

    @InjectMocks
    private SectionService sectionService;

    private AcademicPeriodEntity openPeriod;
    private AcademicPeriodEntity closedPeriod;
    private CourseEntity tap101;
    private TeacherEntity activeTeacher;
    private TeacherEntity inactiveTeacher;
    private TimeBlockEntity blockL1;
    private TimeBlockEntity blockW1;
    private TimeBlockEntity blockV1;
    private TimeBlockEntity blockM1;

    @BeforeEach
    void setUp() {
        openPeriod = AcademicPeriodEntity.builder()
                .id("2027-1")
                .year(2027)
                .semester(1)
                .isOpen(true)
                .build();

        closedPeriod = AcademicPeriodEntity.builder()
                .id("2026-2")
                .year(2026)
                .semester(2)
                .isOpen(false)
                .build();

        CareerEntity career = CareerEntity.builder()
                .code("10450")
                .name("Técnico Analista Programador")
                .build();

        StudyPlanEntity plan = StudyPlanEntity.builder()
                .id(1L)
                .code("2021.3")
                .career(career)
                .build();

        // TAP101: TEL 2-2-2 -> Total 6 hrs -> Requires exactly 3 blocks (6 / 2)
        tap101 = CourseEntity.builder()
                .id(10L)
                .code("TAP101")
                .name("Fundamentos de Programación")
                .semester(1)
                .theoryHours(2)
                .exerciseHours(2)
                .laboratoryHours(2)
                .sctCredits(6)
                .studyPlan(plan)
                .build();

        activeTeacher = TeacherEntity.builder()
                .run("11111111-1")
                .firstName("Pedro")
                .paternalLastName("González")
                .maternalLastName("Pérez")
                .email("pedro.gonzalez@sigaipt.cl")
                .professionalTitle("Ingeniero de Software")
                .academicDegree(AcademicDegree.MAGISTER)
                .isActive(true)
                .build();

        inactiveTeacher = TeacherEntity.builder()
                .run("99999999-9")
                .firstName("Juan")
                .paternalLastName("Inactivo")
                .maternalLastName("Pérez")
                .email("juan.inactivo@sigaipt.cl")
                .isActive(false)
                .build();

        blockL1 = TimeBlockEntity.builder().code("L1").dayOfWeek("L").moduleNumber(1).startTime(LocalTime.of(8, 15)).endTime(LocalTime.of(9, 35)).build();
        blockW1 = TimeBlockEntity.builder().code("W1").dayOfWeek("W").moduleNumber(1).startTime(LocalTime.of(8, 15)).endTime(LocalTime.of(9, 35)).build();
        blockV1 = TimeBlockEntity.builder().code("V1").dayOfWeek("V").moduleNumber(1).startTime(LocalTime.of(8, 15)).endTime(LocalTime.of(9, 35)).build();
        blockM1 = TimeBlockEntity.builder().code("M1").dayOfWeek("M").moduleNumber(1).startTime(LocalTime.of(8, 15)).endTime(LocalTime.of(9, 35)).build();

        lenient().when(timeBlockService.toResponseDTO(any())).thenAnswer(inv -> {
            TimeBlockEntity b = inv.getArgument(0);
            return TimeBlockResponseDTO.builder()
                    .code(b.getCode())
                    .dayOfWeek(b.getDayOfWeek())
                    .moduleNumber(b.getModuleNumber())
                    .build();
        });
    }

    @Test
    @DisplayName("Should successfully create a section with format TAP101-A-1 and correct TEL blocks")
    void testCreateSection_Success() {
        SectionCreateDTO dto = SectionCreateDTO.builder()
                .courseId(10L)
                .academicPeriodId("2027-1")
                .teacherRun("11111111-1")
                .capacity(45)
                .timeBlockCodes(Set.of("L1", "W1", "V1"))
                .build();

        when(academicPeriodRepository.findById("2027-1")).thenReturn(Optional.of(openPeriod));
        when(courseRepository.findById(10L)).thenReturn(Optional.of(tap101));
        when(sectionRepository.existsByCourseIdAndAcademicPeriodId(10L, "2027-1")).thenReturn(false);
        when(teacherRepository.findById("11111111-1")).thenReturn(Optional.of(activeTeacher));

        when(timeBlockRepository.findById("L1")).thenReturn(Optional.of(blockL1));
        when(timeBlockRepository.findById("W1")).thenReturn(Optional.of(blockW1));
        when(timeBlockRepository.findById("V1")).thenReturn(Optional.of(blockV1));

        // Teacher has no other sections in this period
        when(sectionRepository.findByTeacherRunAndAcademicPeriodId("11111111-1", "2027-1")).thenReturn(List.of());

        when(sectionRepository.save(any(SectionEntity.class))).thenAnswer(invocation -> {
            SectionEntity s = invocation.getArgument(0);
            s.setId(100L);
            return s;
        });

        SectionResponseDTO response = sectionService.createSection(dto);

        assertNotNull(response);
        assertEquals("TAP101-A-1", response.getSectionCode());
        assertEquals("TAP101", response.getCourseCode());
        assertEquals("2027-1", response.getAcademicPeriodId());
        assertEquals("11111111-1", response.getTeacherRun());
        assertEquals(45, response.getCapacity());
        assertEquals(0, response.getEnrolledCount());
        verify(sectionRepository).save(any(SectionEntity.class));
    }

    @Test
    @DisplayName("Should throw BusinessRuleException when attempting to create a section in a closed period")
    void testCreateSection_ClosedPeriod_Throws() {
        SectionCreateDTO dto = SectionCreateDTO.builder()
                .courseId(10L)
                .academicPeriodId("2026-2")
                .teacherRun("11111111-1")
                .capacity(45)
                .timeBlockCodes(Set.of("L1", "W1", "V1"))
                .build();

        when(academicPeriodRepository.findById("2026-2")).thenReturn(Optional.of(closedPeriod));

        assertThrows(BusinessRuleException.class, () -> sectionService.createSection(dto));
        verify(sectionRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should throw BusinessRuleException when course already has a section in the period")
    void testCreateSection_DuplicateCourseSection_Throws() {
        SectionCreateDTO dto = SectionCreateDTO.builder()
                .courseId(10L)
                .academicPeriodId("2027-1")
                .teacherRun("11111111-1")
                .capacity(45)
                .timeBlockCodes(Set.of("L1", "W1", "V1"))
                .build();

        when(academicPeriodRepository.findById("2027-1")).thenReturn(Optional.of(openPeriod));
        when(courseRepository.findById(10L)).thenReturn(Optional.of(tap101));
        when(sectionRepository.existsByCourseIdAndAcademicPeriodId(10L, "2027-1")).thenReturn(true);

        assertThrows(BusinessRuleException.class, () -> sectionService.createSection(dto));
        verify(sectionRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should throw BusinessRuleException when assigned teacher is inactive")
    void testCreateSection_InactiveTeacher_Throws() {
        SectionCreateDTO dto = SectionCreateDTO.builder()
                .courseId(10L)
                .academicPeriodId("2027-1")
                .teacherRun("99999999-9")
                .capacity(45)
                .timeBlockCodes(Set.of("L1", "W1", "V1"))
                .build();

        when(academicPeriodRepository.findById("2027-1")).thenReturn(Optional.of(openPeriod));
        when(courseRepository.findById(10L)).thenReturn(Optional.of(tap101));
        when(sectionRepository.existsByCourseIdAndAcademicPeriodId(10L, "2027-1")).thenReturn(false);
        when(teacherRepository.findById("99999999-9")).thenReturn(Optional.of(inactiveTeacher));

        assertThrows(BusinessRuleException.class, () -> sectionService.createSection(dto));
    }

    @Test
    @DisplayName("Should throw BusinessRuleException when time blocks count does not match TEL / 2")
    void testCreateSection_WrongBlocksCount_Throws() {
        // TAP101 has 6 TEL hours = 3 blocks. Only passing 2 blocks:
        SectionCreateDTO dto = SectionCreateDTO.builder()
                .courseId(10L)
                .academicPeriodId("2027-1")
                .teacherRun("11111111-1")
                .capacity(45)
                .timeBlockCodes(Set.of("L1", "W1")) // 2 instead of 3
                .build();

        when(academicPeriodRepository.findById("2027-1")).thenReturn(Optional.of(openPeriod));
        when(courseRepository.findById(10L)).thenReturn(Optional.of(tap101));
        when(sectionRepository.existsByCourseIdAndAcademicPeriodId(10L, "2027-1")).thenReturn(false);
        when(teacherRepository.findById("11111111-1")).thenReturn(Optional.of(activeTeacher));

        assertThrows(BusinessRuleException.class, () -> sectionService.createSection(dto));
    }

    @Test
    @DisplayName("Should throw BusinessRuleException when teacher has a timetable conflict with another section")
    void testCreateSection_TeacherScheduleConflict_Throws() {
        SectionCreateDTO dto = SectionCreateDTO.builder()
                .courseId(10L)
                .academicPeriodId("2027-1")
                .teacherRun("11111111-1")
                .capacity(45)
                .timeBlockCodes(Set.of("L1", "W1", "V1"))
                .build();

        when(academicPeriodRepository.findById("2027-1")).thenReturn(Optional.of(openPeriod));
        when(courseRepository.findById(10L)).thenReturn(Optional.of(tap101));
        when(sectionRepository.existsByCourseIdAndAcademicPeriodId(10L, "2027-1")).thenReturn(false);
        when(teacherRepository.findById("11111111-1")).thenReturn(Optional.of(activeTeacher));

        when(timeBlockRepository.findById("L1")).thenReturn(Optional.of(blockL1));
        when(timeBlockRepository.findById("W1")).thenReturn(Optional.of(blockW1));
        when(timeBlockRepository.findById("V1")).thenReturn(Optional.of(blockV1));

        // Teacher already has section TAP102 on Monday module 1 (L1)
        CourseEntity course2 = CourseEntity.builder().name("Matemática Aplicada").code("TAP102").build();
        SectionEntity conflictingSection = SectionEntity.builder()
                .id(99L)
                .sectionCode("TAP102-A-1")
                .course(course2)
                .teacher(activeTeacher)
                .timeBlocks(Set.of(blockL1)) // Collides on L1!
                .build();

        when(sectionRepository.findByTeacherRunAndAcademicPeriodId("11111111-1", "2027-1"))
                .thenReturn(List.of(conflictingSection));

        assertThrows(BusinessRuleException.class, () -> sectionService.createSection(dto));
        verify(sectionRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should successfully update section capacity and blocks")
    void testUpdateSection_Success() {
        SectionEntity existingSection = SectionEntity.builder()
                .id(100L)
                .sectionCode("TAP101-A-1")
                .course(tap101)
                .academicPeriod(openPeriod)
                .teacher(activeTeacher)
                .capacity(40)
                .enrolledCount(10)
                .timeBlocks(new HashSet<>(Set.of(blockL1, blockW1, blockV1)))
                .build();

        SectionUpdateDTO updateDto = SectionUpdateDTO.builder()
                .teacherRun("11111111-1")
                .capacity(50)
                .timeBlockCodes(Set.of("M1", "W1", "V1"))
                .build();

        when(sectionRepository.findById(100L)).thenReturn(Optional.of(existingSection));
        when(teacherRepository.findById("11111111-1")).thenReturn(Optional.of(activeTeacher));
        when(timeBlockRepository.findById("M1")).thenReturn(Optional.of(blockM1));
        when(timeBlockRepository.findById("W1")).thenReturn(Optional.of(blockW1));
        when(timeBlockRepository.findById("V1")).thenReturn(Optional.of(blockV1));
        when(sectionRepository.findByTeacherRunAndAcademicPeriodId("11111111-1", "2027-1")).thenReturn(List.of(existingSection));
        when(sectionRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        SectionResponseDTO updated = sectionService.updateSection(100L, updateDto);

        assertNotNull(updated);
        assertEquals(50, updated.getCapacity());
        verify(sectionRepository).save(existingSection);
    }

    @Test
    @DisplayName("Should throw BusinessRuleException when updating capacity below currently enrolled students")
    void testUpdateSection_CapacityLessThanEnrolled_Throws() {
        SectionEntity existingSection = SectionEntity.builder()
                .id(100L)
                .sectionCode("TAP101-A-1")
                .academicPeriod(openPeriod)
                .capacity(40)
                .enrolledCount(25) // 25 enrolled
                .build();

        SectionUpdateDTO updateDto = SectionUpdateDTO.builder()
                .teacherRun("11111111-1")
                .capacity(20) // Cannot reduce to 20!
                .timeBlockCodes(Set.of("L1", "W1", "V1"))
                .build();

        when(sectionRepository.findById(100L)).thenReturn(Optional.of(existingSection));

        assertThrows(BusinessRuleException.class, () -> sectionService.updateSection(100L, updateDto));
    }

    @Test
    @DisplayName("Should throw BusinessRuleException when attempting to delete section with enrolled students")
    void testDeleteSection_WithEnrolledStudents_Throws() {
        SectionEntity existingSection = SectionEntity.builder()
                .id(100L)
                .sectionCode("TAP101-A-1")
                .enrolledCount(5) // Has 5 students
                .build();

        when(sectionRepository.findById(100L)).thenReturn(Optional.of(existingSection));

        assertThrows(BusinessRuleException.class, () -> sectionService.deleteSection(100L));
        verify(sectionRepository, never()).delete(any());
    }

    @Test
    @DisplayName("Should successfully delete section without enrolled students")
    void testDeleteSection_Success() {
        SectionEntity existingSection = SectionEntity.builder()
                .id(100L)
                .sectionCode("TAP101-A-1")
                .enrolledCount(0)
                .build();

        when(sectionRepository.findById(100L)).thenReturn(Optional.of(existingSection));

        sectionService.deleteSection(100L);

        verify(sectionRepository).delete(existingSection);
    }

    @Test
    @DisplayName("Should return sections with various filters")
    void testGetSections_Filters() {
        SectionEntity s1 = SectionEntity.builder()
                .id(1L)
                .sectionCode("TAP101-A-1")
                .course(tap101)
                .academicPeriod(openPeriod)
                .teacher(activeTeacher)
                .capacity(45)
                .enrolledCount(0)
                .timeBlocks(Set.of(blockL1, blockW1, blockV1))
                .build();

        // Filter by periodId
        when(sectionRepository.findByAcademicPeriodId("2027-1")).thenReturn(List.of(s1));
        List<SectionResponseDTO> res1 = sectionService.getSections("2027-1", null, null);
        assertEquals(1, res1.size());

        // Filter by periodId and studyPlanId
        when(sectionRepository.findByAcademicPeriodIdAndCourseStudyPlanId("2027-1", 1L)).thenReturn(List.of(s1));
        List<SectionResponseDTO> res2 = sectionService.getSections("2027-1", 1L, null);
        assertEquals(1, res2.size());

        // Filter by periodId and teacherRun
        when(sectionRepository.findByTeacherRunAndAcademicPeriodId("11111111-1", "2027-1")).thenReturn(List.of(s1));
        List<SectionResponseDTO> res3 = sectionService.getSections("2027-1", null, "11111111-1");
        assertEquals(1, res3.size());

        // No filter
        when(sectionRepository.findAll()).thenReturn(List.of(s1));
        List<SectionResponseDTO> res4 = sectionService.getSections(null, null, null);
        assertEquals(1, res4.size());
    }

    @Test
    @DisplayName("Should get section by ID or throw ResourceNotFoundException")
    void testGetSectionById() {
        SectionEntity s1 = SectionEntity.builder()
                .id(1L)
                .sectionCode("TAP101-A-1")
                .course(tap101)
                .academicPeriod(openPeriod)
                .teacher(activeTeacher)
                .capacity(45)
                .enrolledCount(0)
                .timeBlocks(Set.of(blockL1, blockW1, blockV1))
                .build();

        when(sectionRepository.findById(1L)).thenReturn(Optional.of(s1));
        when(sectionRepository.findById(999L)).thenReturn(Optional.empty());

        assertNotNull(sectionService.getSectionById(1L));
        assertThrows(ResourceNotFoundException.class, () -> sectionService.getSectionById(999L));
    }

    @Test
    @DisplayName("Should throw BusinessRuleException when updating section in closed period")
    void testUpdateSection_ClosedPeriod_Throws() {
        SectionEntity sClosed = SectionEntity.builder()
                .id(100L)
                .academicPeriod(closedPeriod)
                .capacity(45)
                .enrolledCount(0)
                .build();

        when(sectionRepository.findById(100L)).thenReturn(Optional.of(sClosed));

        SectionUpdateDTO dto = SectionUpdateDTO.builder()
                .teacherRun("11111111-1")
                .capacity(45)
                .timeBlockCodes(Set.of("L1", "W1", "V1"))
                .build();

        assertThrows(BusinessRuleException.class, () -> sectionService.updateSection(100L, dto));
    }

    @Test
    @DisplayName("Should throw BusinessRuleException when updating section with inactive teacher")
    void testUpdateSection_InactiveTeacher_Throws() {
        SectionEntity sOpen = SectionEntity.builder()
                .id(100L)
                .academicPeriod(openPeriod)
                .course(tap101)
                .capacity(45)
                .enrolledCount(0)
                .build();

        when(sectionRepository.findById(100L)).thenReturn(Optional.of(sOpen));
        when(teacherRepository.findById("99999999-9")).thenReturn(Optional.of(inactiveTeacher));

        SectionUpdateDTO dto = SectionUpdateDTO.builder()
                .teacherRun("99999999-9")
                .capacity(45)
                .timeBlockCodes(Set.of("L1", "W1", "V1"))
                .build();

        assertThrows(BusinessRuleException.class, () -> sectionService.updateSection(100L, dto));
    }
}
