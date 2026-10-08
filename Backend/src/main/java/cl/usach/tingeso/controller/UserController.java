package cl.usach.tingeso.controller;

import cl.usach.tingeso.dto.PasswordChangeDTO;
import cl.usach.tingeso.exception.BusinessRuleException;
import cl.usach.tingeso.exception.ResourceNotFoundException;
import cl.usach.tingeso.service.KeycloakUserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * UserController exposes REST endpoints for user account actions such as password resets.
 * Layer: Controller.
 */
@RestController
@RequestMapping("/api/users")
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
@Slf4j
public class UserController {

    private final KeycloakUserService keycloakUserService;

    /**
     * Updates or resets a user's password in Keycloak.
     * Authorization rules:
     * - ADMIN can change passwords for any user (teachers and students) without knowing current password.
     * - TEACHER and STUDENT (and any user changing their own password) must provide and verify their current password.
     *
     * @param username user username or RUN
     * @param dto payload containing currentPassword (if self-service) and newPassword
     * @param temporary optional flag indicating if the password must be changed at next login (default false)
     * @param authentication active security context
     * @return 200 OK on success, 400 on validation failure, 403 on forbidden access, 404 if not found
     */
    @PutMapping("/{username}/password")
    @PreAuthorize("hasRole('ADMIN') or #username == authentication.name")
    public ResponseEntity<?> changePassword(
            @PathVariable String username,
            @Valid @RequestBody PasswordChangeDTO dto,
            @RequestParam(required = false, defaultValue = "false") boolean temporary,
            Authentication authentication
    ) {
        try {
            boolean isAdmin = authentication != null && authentication.getAuthorities().stream()
                    .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));

            boolean isSelfService = authentication != null && username.equals(authentication.getName());

            // If a user is changing their own password (or non-admin caller), current password is required and verified
            if (!isAdmin || isSelfService) {
                if (dto.getCurrentPassword() == null || dto.getCurrentPassword().isBlank()) {
                    throw new BusinessRuleException("Debes ingresar tu contraseña actual para cambiarla.");
                }

                if (dto.getCurrentPassword().equals(dto.getNewPassword())) {
                    throw new BusinessRuleException("La nueva contraseña debe ser distinta a la contraseña actual.");
                }

                boolean isCurrentValid = keycloakUserService.verifyUserPassword(username, dto.getCurrentPassword());
                if (!isCurrentValid) {
                    throw new BusinessRuleException("La contraseña actual ingresada es incorrecta.");
                }
            }

            keycloakUserService.resetPassword(username, dto.getNewPassword(), temporary);
            return ResponseEntity.ok(Map.of(
                    "message", "Contraseña actualizada exitosamente para el usuario: " + username
            ));
        } catch (ResourceNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", e.getMessage()));
        } catch (BusinessRuleException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            log.error("Unexpected error updating password for user {}: {}", username, e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Error inesperado al actualizar contraseña: " + e.getMessage()));
        }
    }
}
