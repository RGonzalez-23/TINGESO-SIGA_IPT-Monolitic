package cl.usach.tingeso.service;

import cl.usach.tingeso.repository.StudentRepository;
import cl.usach.tingeso.repository.TeacherRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.text.Normalizer;

/**
 * InstitutionalEmailService centralizes unique email generation and collision resolution
 * for both Students and Teachers within SIGA IPT.
 * Layer: Service.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class InstitutionalEmailService {

    private final StudentRepository studentRepository;
    private final TeacherRepository teacherRepository;

    /**
     * Autogenerates a unique institutional email (@sigaipt.cl) ensuring no collision
     * exists across students and teachers.
     * Rule:
     * - Base format: firstName.paternalLastName@sigaipt.cl
     * - If collision occurs, append maternal last name initial: firstName.paternalLastName.x@sigaipt.cl
     * - If further collisions occur, append consecutive characters from maternal last name: .xy, .xyz, etc.
     * - If maternal characters are exhausted, append sequential numbers.
     *
     * @param firstName first name
     * @param paternalLastName paternal last name
     * @param maternalLastName maternal last name
     * @return unique institutional email
     */
    public String generateUniqueEmail(String firstName, String paternalLastName, String maternalLastName) {
        String sanitizedFirst = sanitizeForEmail(firstName != null ? firstName.split(" ")[0] : "");
        String sanitizedPaternal = sanitizeForEmail(paternalLastName);
        String sanitizedMaternal = sanitizeForEmail(maternalLastName);

        String baseCandidate = String.format("%s.%s@sigaipt.cl", sanitizedFirst, sanitizedPaternal).toLowerCase();

        if (!emailExists(baseCandidate)) {
            return baseCandidate;
        }

        // Collision occurred: try progressively adding characters from maternal last name
        if (!sanitizedMaternal.isEmpty()) {
            for (int i = 1; i <= sanitizedMaternal.length(); i++) {
                String maternalPart = sanitizedMaternal.substring(0, i);
                String candidate = String.format("%s.%s.%s@sigaipt.cl", sanitizedFirst, sanitizedPaternal, maternalPart).toLowerCase();
                if (!emailExists(candidate)) {
                    log.info("Resolved email collision with maternal prefix '{}': {}", maternalPart, candidate);
                    return candidate;
                }
            }
        }

        // If all characters exhausted or maternal empty, append sequential numbers
        int counter = 1;
        while (true) {
            String suffix = !sanitizedMaternal.isEmpty()
                    ? "." + sanitizedMaternal + counter
                    : "." + counter;
            String candidate = String.format("%s.%s%s@sigaipt.cl", sanitizedFirst, sanitizedPaternal, suffix).toLowerCase();
            if (!emailExists(candidate)) {
                log.info("Resolved email collision with numeric suffix '{}': {}", suffix, candidate);
                return candidate;
            }
            counter++;
        }
    }

    /**
     * Checks if an email is already in use by either a student or a teacher.
     *
     * @param email institutional email
     * @return true if taken, false otherwise
     */
    public boolean emailExists(String email) {
        if (email == null) return false;
        String normalized = email.trim().toLowerCase();
        return studentRepository.existsByEmail(normalized) || teacherRepository.existsByEmail(normalized);
    }

    /**
     * Normalizes names by stripping diacritical marks (accents) and non-alphanumeric characters.
     */
    public String sanitizeForEmail(String input) {
        if (input == null) return "";
        String normalized = Normalizer.normalize(input.trim(), Normalizer.Form.NFD);
        return normalized.replaceAll("\\p{M}", "")
                .replaceAll("[^a-zA-Z0-9]", "")
                .toLowerCase();
    }
}
