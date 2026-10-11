package cl.usach.tingeso.service;

import cl.usach.tingeso.dto.AcademicPeriodCreateDTO;
import cl.usach.tingeso.dto.AcademicPeriodResponseDTO;
import cl.usach.tingeso.entity.AcademicPeriodEntity;
import cl.usach.tingeso.exception.BusinessRuleException;
import cl.usach.tingeso.exception.ResourceNotFoundException;
import cl.usach.tingeso.repository.AcademicPeriodRepository;
import cl.usach.tingeso.repository.SectionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for AcademicPeriodService.
 * Layer: Service.
 * Verifies period creation in open state, status toggling, and queries.
 */
@ExtendWith(MockitoExtension.class)
class AcademicPeriodServiceTest {

    @Mock
    private AcademicPeriodRepository academicPeriodRepository;

    @Mock
    private SectionRepository sectionRepository;

    @InjectMocks
    private AcademicPeriodService academicPeriodService;

    private AcademicPeriodEntity period1;

    @BeforeEach
    void setUp() {
        period1 = AcademicPeriodEntity.builder()
                .id("2027-1")
                .year(2027)
                .semester(1)
                .isOpen(true)
                .build();
    }

    @Test
    @DisplayName("Should retrieve all academic periods ordered descending")
    void testGetAllPeriods() {
        when(academicPeriodRepository.findAllByOrderByYearDescSemesterDesc()).thenReturn(List.of(period1));
        when(sectionRepository.countByAcademicPeriodId("2027-1")).thenReturn(5L);

        List<AcademicPeriodResponseDTO> list = academicPeriodService.getAllPeriods();

        assertEquals(1, list.size());
        assertEquals("2027-1", list.get(0).getId());
        assertEquals(5L, list.get(0).getSectionCount());
    }

    @Test
    @DisplayName("Should retrieve academic period by ID successfully")
    void testGetPeriodById_Success() {
        when(academicPeriodRepository.findById("2027-1")).thenReturn(Optional.of(period1));
        when(sectionRepository.countByAcademicPeriodId("2027-1")).thenReturn(3L);

        AcademicPeriodResponseDTO result = academicPeriodService.getPeriodById("2027-1");

        assertNotNull(result);
        assertEquals("2027-1", result.getId());
        assertTrue(result.getIsOpen());
    }

    @Test
    @DisplayName("Should throw ResourceNotFoundException when period does not exist")
    void testGetPeriodById_NotFound() {
        when(academicPeriodRepository.findById("2099-2")).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> academicPeriodService.getPeriodById("2099-2"));
    }

    @Test
    @DisplayName("Should successfully create a new academic period in ABIERTO status")
    void testCreatePeriod_Success() {
        AcademicPeriodCreateDTO dto = AcademicPeriodCreateDTO.builder()
                .year(2027)
                .semester(2)
                .build();

        when(academicPeriodRepository.existsById("2027-2")).thenReturn(false);
        when(academicPeriodRepository.findAllByIsOpenTrue()).thenReturn(List.of(period1));
        when(academicPeriodRepository.save(any(AcademicPeriodEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(sectionRepository.countByAcademicPeriodId("2027-2")).thenReturn(0L);

        AcademicPeriodResponseDTO created = academicPeriodService.createPeriod(dto);

        assertNotNull(created);
        assertEquals("2027-2", created.getId());
        assertEquals(2027, created.getYear());
        assertEquals(2, created.getSemester());
        assertTrue(created.getIsOpen()); // Initial status must be ABIERTO
        assertFalse(period1.getIsOpen()); // Previous period must have been closed automatically!
    }

    @Test
    @DisplayName("Should throw BusinessRuleException when creating a duplicate period")
    void testCreatePeriod_Duplicate() {
        AcademicPeriodCreateDTO dto = AcademicPeriodCreateDTO.builder()
                .year(2027)
                .semester(1)
                .build();

        when(academicPeriodRepository.existsById("2027-1")).thenReturn(true);

        assertThrows(BusinessRuleException.class, () -> academicPeriodService.createPeriod(dto));
    }

    @Test
    @DisplayName("Should successfully toggle period status from ABIERTO to CERRADO")
    void testTogglePeriodStatus() {
        when(academicPeriodRepository.findById("2027-1")).thenReturn(Optional.of(period1));
        when(academicPeriodRepository.save(any(AcademicPeriodEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        AcademicPeriodResponseDTO updated = academicPeriodService.togglePeriodStatus("2027-1");

        assertFalse(updated.getIsOpen()); // Was true, now false
        verify(academicPeriodRepository).save(period1);
    }

    @Test
    @DisplayName("Should automatically close any other open period when toggling a closed period to ABIERTO")
    void testTogglePeriodStatus_OpeningClosesOtherOpenPeriods() {
        AcademicPeriodEntity closedPeriod = AcademicPeriodEntity.builder()
                .id("2026-2")
                .year(2026)
                .semester(2)
                .isOpen(false)
                .build();

        when(academicPeriodRepository.findById("2026-2")).thenReturn(Optional.of(closedPeriod));
        when(academicPeriodRepository.findAllByIsOpenTrue()).thenReturn(List.of(period1));
        when(academicPeriodRepository.save(any(AcademicPeriodEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        AcademicPeriodResponseDTO updated = academicPeriodService.togglePeriodStatus("2026-2");

        assertTrue(updated.getIsOpen()); // Closed period became open
        assertFalse(period1.getIsOpen()); // Open period was automatically closed
    }
}
