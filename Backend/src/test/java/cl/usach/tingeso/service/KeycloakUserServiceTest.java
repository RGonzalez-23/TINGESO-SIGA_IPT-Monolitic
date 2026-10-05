package cl.usach.tingeso.service;

import cl.usach.tingeso.exception.BusinessRuleException;
import cl.usach.tingeso.exception.ResourceNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

/**
 * Unit tests for KeycloakUserService using MockRestServiceServer.
 * Layer: Service.
 * Verifies service token acquisition, user creation, role assignment, and deletion.
 */
class KeycloakUserServiceTest {

    private MockRestServiceServer mockServer;
    private KeycloakUserService keycloakUserService;

    private static final String SERVER_URL = "http://localhost:8080";
    private static final String REALM = "siga-ipt-realm";
    private static final String CLIENT_ID = "siga-backend-service";
    private static final String CLIENT_SECRET = "test-secret";
    private static final String DEFAULT_PASSWORD = "Siga2026!";

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder();
        this.mockServer = MockRestServiceServer.bindTo(builder).build();
        this.keycloakUserService = new KeycloakUserService(
                builder,
                SERVER_URL,
                REALM,
                CLIENT_ID,
                CLIENT_SECRET,
                DEFAULT_PASSWORD
        );
    }

    @Test
    @DisplayName("Should successfully retrieve service account token")
    void shouldRetrieveServiceAccountToken() {
        mockServer.expect(requestTo("http://localhost:8080/realms/siga-ipt-realm/protocol/openid-connect/token"))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withSuccess("{\"access_token\":\"mock-token-xyz\"}", MediaType.APPLICATION_JSON));

        String token = keycloakUserService.getServiceAccountToken();

        assertEquals("mock-token-xyz", token);
        mockServer.verify();
    }

    @Test
    @DisplayName("Should throw BusinessRuleException when token request fails")
    void shouldThrowExceptionWhenTokenRequestFails() {
        mockServer.expect(requestTo("http://localhost:8080/realms/siga-ipt-realm/protocol/openid-connect/token"))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withStatus(HttpStatus.UNAUTHORIZED));

        assertThrows(BusinessRuleException.class, () -> keycloakUserService.getServiceAccountToken());
        mockServer.verify();
    }

    @Test
    @DisplayName("Should successfully provision student user and assign STUDENT realm role")
    void shouldProvisionStudentUserSuccessfully() {
        // 1. Service token request
        mockServer.expect(requestTo("http://localhost:8080/realms/siga-ipt-realm/protocol/openid-connect/token"))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withSuccess("{\"access_token\":\"admin-token-123\"}", MediaType.APPLICATION_JSON));

        // 2. Create user request returning 201 Created with Location header
        HttpHeaders headers = new HttpHeaders();
        headers.setLocation(java.net.URI.create("http://localhost:8080/admin/realms/siga-ipt-realm/users/user-uuid-999"));
        mockServer.expect(requestTo("http://localhost:8080/admin/realms/siga-ipt-realm/users"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header(HttpHeaders.AUTHORIZATION, "Bearer admin-token-123"))
                .andRespond(withStatus(HttpStatus.CREATED).headers(headers));

        // 3. Get realm role STUDENT representation
        mockServer.expect(requestTo("http://localhost:8080/admin/realms/siga-ipt-realm/roles/STUDENT"))
                .andExpect(method(HttpMethod.GET))
                .andExpect(header(HttpHeaders.AUTHORIZATION, "Bearer admin-token-123"))
                .andRespond(withSuccess("{\"id\":\"role-uuid-1\",\"name\":\"STUDENT\"}", MediaType.APPLICATION_JSON));

        // 4. Assign role to user
        mockServer.expect(requestTo("http://localhost:8080/admin/realms/siga-ipt-realm/users/user-uuid-999/role-mappings/realm"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header(HttpHeaders.AUTHORIZATION, "Bearer admin-token-123"))
                .andRespond(withStatus(HttpStatus.NO_CONTENT));

        String userId = keycloakUserService.createStudentUser("12345678-5", "juan.perez@sigaipt.cl", "Juan", "Perez");

        assertEquals("user-uuid-999", userId);
        mockServer.verify();
    }

    @Test
    @DisplayName("Should throw BusinessRuleException when user creation returns 409 Conflict")
    void shouldThrowConflictWhenUserAlreadyExistsInKeycloak() {
        mockServer.expect(requestTo("http://localhost:8080/realms/siga-ipt-realm/protocol/openid-connect/token"))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withSuccess("{\"access_token\":\"admin-token-123\"}", MediaType.APPLICATION_JSON));

        mockServer.expect(requestTo("http://localhost:8080/admin/realms/siga-ipt-realm/users"))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withStatus(HttpStatus.CONFLICT));

        assertThrows(BusinessRuleException.class, () ->
                keycloakUserService.createStudentUser("12345678-5", "juan.perez@sigaipt.cl", "Juan", "Perez"));

        mockServer.verify();
    }

    @Test
    @DisplayName("Should successfully delete user when user exists in Keycloak")
    void shouldDeleteUserWhenExists() {
        // 1. Service token request
        mockServer.expect(requestTo("http://localhost:8080/realms/siga-ipt-realm/protocol/openid-connect/token"))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withSuccess("{\"access_token\":\"admin-token-123\"}", MediaType.APPLICATION_JSON));

        // 2. Search user by username
        mockServer.expect(requestTo("http://localhost:8080/admin/realms/siga-ipt-realm/users?username=12345678-5&exact=true"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess("[{\"id\":\"user-uuid-999\",\"username\":\"12345678-5\"}]", MediaType.APPLICATION_JSON));

        // 3. Delete user request
        mockServer.expect(requestTo("http://localhost:8080/admin/realms/siga-ipt-realm/users/user-uuid-999"))
                .andExpect(method(HttpMethod.DELETE))
                .andRespond(withStatus(HttpStatus.NO_CONTENT));

        keycloakUserService.deleteUser("12345678-5");
        mockServer.verify();
    }

    @Test
    @DisplayName("Should handle deleteUser gracefully when user is not found in Keycloak")
    void shouldHandleDeleteUserWhenNotFound() {
        mockServer.expect(requestTo("http://localhost:8080/realms/siga-ipt-realm/protocol/openid-connect/token"))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withSuccess("{\"access_token\":\"admin-token-123\"}", MediaType.APPLICATION_JSON));

        mockServer.expect(requestTo("http://localhost:8080/admin/realms/siga-ipt-realm/users?username=12345678-5&exact=true"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess("[]", MediaType.APPLICATION_JSON));

        // Should complete without throwing exception
        keycloakUserService.deleteUser("12345678-5");
        mockServer.verify();
    }

    @Test
    @DisplayName("Should find userId by username search when Location header is missing")
    void shouldFindUserIdByUsernameWhenLocationHeaderMissing() {
        // 1. Token
        mockServer.expect(requestTo("http://localhost:8080/realms/siga-ipt-realm/protocol/openid-connect/token"))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withSuccess("{\"access_token\":\"admin-token-123\"}", MediaType.APPLICATION_JSON));

        // 2. Create user with no Location header
        mockServer.expect(requestTo("http://localhost:8080/admin/realms/siga-ipt-realm/users"))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withStatus(HttpStatus.CREATED));

        // 3. User search by username
        mockServer.expect(requestTo("http://localhost:8080/admin/realms/siga-ipt-realm/users?username=12345678-5&exact=true"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess("[{\"id\":\"found-user-id\",\"username\":\"12345678-5\"}]", MediaType.APPLICATION_JSON));

        // 4. Role lookup
        mockServer.expect(requestTo("http://localhost:8080/admin/realms/siga-ipt-realm/roles/STUDENT"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess("{\"id\":\"role-1\",\"name\":\"STUDENT\"}", MediaType.APPLICATION_JSON));

        // 5. Role mapping
        mockServer.expect(requestTo("http://localhost:8080/admin/realms/siga-ipt-realm/users/found-user-id/role-mappings/realm"))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withStatus(HttpStatus.NO_CONTENT));

        String userId = keycloakUserService.createStudentUser("12345678-5", "test@sigaipt.cl", "Ana", "Rojas");
        assertEquals("found-user-id", userId);
        mockServer.verify();
    }

    @Test
    @DisplayName("Should successfully reset user password in Keycloak")
    void shouldResetPasswordSuccessfully() {
        // 1. Service token request
        mockServer.expect(requestTo("http://localhost:8080/realms/siga-ipt-realm/protocol/openid-connect/token"))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withSuccess("{\"access_token\":\"admin-token-123\"}", MediaType.APPLICATION_JSON));

        // 2. Search user by username
        mockServer.expect(requestTo("http://localhost:8080/admin/realms/siga-ipt-realm/users?username=12345678-5&exact=true"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess("[{\"id\":\"user-uuid-999\",\"username\":\"12345678-5\"}]", MediaType.APPLICATION_JSON));

        // 3. Reset password PUT request
        mockServer.expect(requestTo("http://localhost:8080/admin/realms/siga-ipt-realm/users/user-uuid-999/reset-password"))
                .andExpect(method(HttpMethod.PUT))
                .andExpect(header(HttpHeaders.AUTHORIZATION, "Bearer admin-token-123"))
                .andRespond(withStatus(HttpStatus.NO_CONTENT));

        keycloakUserService.resetPassword("12345678-5", "NewSecret123!", false);
        mockServer.verify();
    }

    @Test
    @DisplayName("Should throw ResourceNotFoundException when resetting password for non-existent user")
    void shouldThrowWhenResettingPasswordForNonExistentUser() {
        // 1. Service token request
        mockServer.expect(requestTo("http://localhost:8080/realms/siga-ipt-realm/protocol/openid-connect/token"))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withSuccess("{\"access_token\":\"admin-token-123\"}", MediaType.APPLICATION_JSON));

        // 2. Search user by username returns empty
        mockServer.expect(requestTo("http://localhost:8080/admin/realms/siga-ipt-realm/users?username=unknown-user&exact=true"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess("[]", MediaType.APPLICATION_JSON));

        assertThrows(ResourceNotFoundException.class, () ->
                keycloakUserService.resetPassword("unknown-user", "NewSecret123!", false));

        mockServer.verify();
    }
}
