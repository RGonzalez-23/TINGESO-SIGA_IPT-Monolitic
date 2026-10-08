package cl.usach.tingeso.controller;

import cl.usach.tingeso.dto.PasswordChangeDTO;
import cl.usach.tingeso.exception.BusinessRuleException;
import cl.usach.tingeso.exception.ResourceNotFoundException;
import cl.usach.tingeso.service.KeycloakUserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for UserController.
 * Layer: Controller.
 * Verifies password reset authorization, current password enforcement for self-service,
 * and error responses.
 */
@ExtendWith(MockitoExtension.class)
class UserControllerTest {

    @Mock
    private KeycloakUserService keycloakUserService;

    @Mock
    private Authentication authentication;

    @InjectMocks
    private UserController userController;

    private PasswordChangeDTO validDto;

    @BeforeEach
    void setUp() {
        validDto = PasswordChangeDTO.builder()
                .currentPassword("OldSecret123!")
                .newPassword("NewSecret456!")
                .build();
    }

    @Test
    @DisplayName("Should successfully change password when self-service user enters valid current password")
    void shouldChangePasswordSelfServiceSuccess() {
        doReturn(Collections.singletonList(new SimpleGrantedAuthority("ROLE_TEACHER"))).when(authentication).getAuthorities();
        when(authentication.getName()).thenReturn("11111111-1");
        when(keycloakUserService.verifyUserPassword("11111111-1", "OldSecret123!")).thenReturn(true);
        doNothing().when(keycloakUserService).resetPassword("11111111-1", "NewSecret456!", false);

        ResponseEntity<?> response = userController.changePassword("11111111-1", validDto, false, authentication);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        verify(keycloakUserService).verifyUserPassword("11111111-1", "OldSecret123!");
        verify(keycloakUserService).resetPassword("11111111-1", "NewSecret456!", false);
    }

    @Test
    @DisplayName("Should return 400 Bad Request when self-service user omits current password")
    void shouldFailWhenSelfServiceOmitsCurrentPassword() {
        doReturn(Collections.singletonList(new SimpleGrantedAuthority("ROLE_TEACHER"))).when(authentication).getAuthorities();
        when(authentication.getName()).thenReturn("11111111-1");

        PasswordChangeDTO dtoWithoutCurrent = PasswordChangeDTO.builder()
                .newPassword("NewSecret456!")
                .build();

        ResponseEntity<?> response = userController.changePassword("11111111-1", dtoWithoutCurrent, false, authentication);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        verify(keycloakUserService, never()).resetPassword(anyString(), anyString(), anyBoolean());
    }

    @Test
    @DisplayName("Should return 400 Bad Request when self-service user enters incorrect current password")
    void shouldFailWhenSelfServiceEntersIncorrectCurrentPassword() {
        doReturn(Collections.singletonList(new SimpleGrantedAuthority("ROLE_TEACHER"))).when(authentication).getAuthorities();
        when(authentication.getName()).thenReturn("11111111-1");
        when(keycloakUserService.verifyUserPassword("11111111-1", "OldSecret123!")).thenReturn(false);

        ResponseEntity<?> response = userController.changePassword("11111111-1", validDto, false, authentication);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        verify(keycloakUserService, never()).resetPassword(anyString(), anyString(), anyBoolean());
    }

    @Test
    @DisplayName("Should return 400 Bad Request when new password equals current password")
    void shouldFailWhenNewPasswordEqualsCurrentPassword() {
        doReturn(Collections.singletonList(new SimpleGrantedAuthority("ROLE_STUDENT"))).when(authentication).getAuthorities();
        when(authentication.getName()).thenReturn("22222222-2");

        PasswordChangeDTO sameDto = PasswordChangeDTO.builder()
                .currentPassword("SamePassword123!")
                .newPassword("SamePassword123!")
                .build();

        ResponseEntity<?> response = userController.changePassword("22222222-2", sameDto, false, authentication);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        verify(keycloakUserService, never()).resetPassword(anyString(), anyString(), anyBoolean());
    }

    @Test
    @DisplayName("Should allow admin to reset another user's password without current password")
    void shouldAllowAdminToResetUserPasswordWithoutCurrentPassword() {
        doReturn(Collections.singletonList(new SimpleGrantedAuthority("ROLE_ADMIN"))).when(authentication).getAuthorities();
        when(authentication.getName()).thenReturn("admin");

        PasswordChangeDTO adminResetDto = PasswordChangeDTO.builder()
                .newPassword("Temporary123!")
                .build();

        doNothing().when(keycloakUserService).resetPassword("11111111-1", "Temporary123!", true);

        ResponseEntity<?> response = userController.changePassword("11111111-1", adminResetDto, true, authentication);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        verify(keycloakUserService, never()).verifyUserPassword(anyString(), anyString());
        verify(keycloakUserService).resetPassword("11111111-1", "Temporary123!", true);
    }

    @Test
    @DisplayName("Should return 404 Not Found when user does not exist in Keycloak")
    void shouldReturn404WhenUserNotFoundInKeycloak() {
        doReturn(Collections.singletonList(new SimpleGrantedAuthority("ROLE_ADMIN"))).when(authentication).getAuthorities();
        when(authentication.getName()).thenReturn("admin");

        PasswordChangeDTO adminResetDto = PasswordChangeDTO.builder()
                .newPassword("Temporary123!")
                .build();

        doThrow(new ResourceNotFoundException("User not found")).when(keycloakUserService)
                .resetPassword("99999999-9", "Temporary123!", true);

        ResponseEntity<?> response = userController.changePassword("99999999-9", adminResetDto, true, authentication);

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
    }
}
