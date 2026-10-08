package cl.usach.tingeso.service;

import cl.usach.tingeso.repository.StudentRepository;
import cl.usach.tingeso.repository.TeacherRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

/**
 * Unit tests for InstitutionalEmailService.
 * Layer: Service.
 * Verifies email generation, accent stripping, and collision resolution rules across students and teachers.
 */
@ExtendWith(MockitoExtension.class)
class InstitutionalEmailServiceTest {

    @Mock
    private StudentRepository studentRepository;

    @Mock
    private TeacherRepository teacherRepository;

    @InjectMocks
    private InstitutionalEmailService emailService;

    @Test
    @DisplayName("Should generate base email when no collisions exist")
    void testGenerateBaseEmail_NoCollision() {
        when(studentRepository.existsByEmail("juan.perez@sigaipt.cl")).thenReturn(false);
        when(teacherRepository.existsByEmail("juan.perez@sigaipt.cl")).thenReturn(false);

        String email = emailService.generateUniqueEmail("Juan Carlos", "Pérez", "González");

        assertEquals("juan.perez@sigaipt.cl", email);
    }

    @Test
    @DisplayName("Should resolve collision with maternal initial on first collision")
    void testGenerateEmail_CollisionResolvedWithMaternalInitial() {
        // Base candidate exists in students
        when(studentRepository.existsByEmail("juan.perez@sigaipt.cl")).thenReturn(true);
        // Candidate with maternal initial 'g' does not exist
        when(studentRepository.existsByEmail("juan.perez.g@sigaipt.cl")).thenReturn(false);
        when(teacherRepository.existsByEmail("juan.perez.g@sigaipt.cl")).thenReturn(false);

        String email = emailService.generateUniqueEmail("Juan", "Pérez", "González");

        assertEquals("juan.perez.g@sigaipt.cl", email);
    }

    @Test
    @DisplayName("Should resolve collision by taking multiple characters from maternal last name")
    void testGenerateEmail_CollisionResolvedWithMultipleMaternalChars() {
        // Base candidate exists in teachers
        when(studentRepository.existsByEmail("carlos.valenzuela@sigaipt.cl")).thenReturn(false);
        when(teacherRepository.existsByEmail("carlos.valenzuela@sigaipt.cl")).thenReturn(true);

        // candidate with 's' also exists
        when(studentRepository.existsByEmail("carlos.valenzuela.s@sigaipt.cl")).thenReturn(true);

        // candidate with 'so' is free
        when(studentRepository.existsByEmail("carlos.valenzuela.so@sigaipt.cl")).thenReturn(false);
        when(teacherRepository.existsByEmail("carlos.valenzuela.so@sigaipt.cl")).thenReturn(false);

        String email = emailService.generateUniqueEmail("Carlos", "Valenzuela", "Soto");

        assertEquals("carlos.valenzuela.so@sigaipt.cl", email);
    }

    @Test
    @DisplayName("Should append numeric suffix when all maternal characters are exhausted")
    void testGenerateEmail_CollisionExhaustedMaternalChars() {
        when(studentRepository.existsByEmail("ana.rojas@sigaipt.cl")).thenReturn(true);
        when(studentRepository.existsByEmail("ana.rojas.p@sigaipt.cl")).thenReturn(true);
        when(studentRepository.existsByEmail("ana.rojas.pa@sigaipt.cl")).thenReturn(true);
        when(studentRepository.existsByEmail("ana.rojas.paz@sigaipt.cl")).thenReturn(true);

        when(studentRepository.existsByEmail("ana.rojas.paz1@sigaipt.cl")).thenReturn(false);
        when(teacherRepository.existsByEmail("ana.rojas.paz1@sigaipt.cl")).thenReturn(false);

        String email = emailService.generateUniqueEmail("Ana", "Rojas", "Paz");

        assertEquals("ana.rojas.paz1@sigaipt.cl", email);
    }

    @Test
    @DisplayName("Should append numeric suffix when maternal last name is empty")
    void testGenerateEmail_CollisionWithEmptyMaternal() {
        when(studentRepository.existsByEmail("john.doe@sigaipt.cl")).thenReturn(true);
        when(studentRepository.existsByEmail("john.doe.1@sigaipt.cl")).thenReturn(false);
        when(teacherRepository.existsByEmail("john.doe.1@sigaipt.cl")).thenReturn(false);

        String email = emailService.generateUniqueEmail("John", "Doe", "");

        assertEquals("john.doe.1@sigaipt.cl", email);
    }

    @Test
    @DisplayName("Should remove accents, tildes, and special characters properly")
    void testSanitizeForEmail_DiacriticsAndAccents() {
        assertEquals("gonzalez", emailService.sanitizeForEmail("González"));
        assertEquals("jose", emailService.sanitizeForEmail("José"));
        assertEquals("alvarez", emailService.sanitizeForEmail("Álvarez"));
        assertEquals("nunez", emailService.sanitizeForEmail("Núñez"));
        assertEquals("pena", emailService.sanitizeForEmail("Peña"));
        assertEquals("", emailService.sanitizeForEmail(null));
        assertEquals("", emailService.sanitizeForEmail("  "));
    }
}
