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
     * - ADMIN can change passwords for any user (teachers and students).
     * - TEACHER and STUDENT can only change their own password (#username == authentication.name).
     *
     * @param username user username or RUN
     * @param dto payload containing the new password
     * @param temporary optional flag indicating if the password must be changed at next login (default false)
     * @return 200 OK on success, 400 on validation failure, 403 on forbidden access, 404 if not found
     */
    @PutMapping("/{username}/password")
    @PreAuthorize("hasRole('ADMIN') or #username == authentication.name")
    public ResponseEntity<?> changePassword(
            @PathVariable String username,
            @Valid @RequestBody PasswordChangeDTO dto,
            @RequestParam(required = false, defaultValue = "false") boolean temporary
    ) {
        try {
            keycloakUserService.resetPassword(username, dto.getNewPassword(), temporary);
            return ResponseEntity.ok(Map.of(
                    "message", "Password updated successfully for user: " + username
            ));
        } catch (ResourceNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", e.getMessage()));
        } catch (BusinessRuleException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            log.error("Unexpected error updating password for user {}: {}", username, e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Unexpected error updating password: " + e.getMessage()));
        }
    }
}
