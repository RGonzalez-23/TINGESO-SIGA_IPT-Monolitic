package cl.usach.tingeso.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;

import java.time.Instant;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class KeycloakRoleConverterTest {

    private final KeycloakRoleConverter converter = new KeycloakRoleConverter();

    private Jwt createMockJwt(Map<String, Object> claims) {
        java.util.Map<String, Object> mergedClaims = new java.util.HashMap<>(claims);
        mergedClaims.putIfAbsent("sub", "mock-sub-123");
        return new Jwt(
                "mock-token-value",
                Instant.now(),
                Instant.now().plusSeconds(300),
                Map.of("alg", "RS256"),
                mergedClaims
        );
    }

    @Test
    @DisplayName("Should extract and map roles prefixed with ROLE_ when realm_access.roles is present")
    void shouldExtractRolesWithRolePrefix() {
        Map<String, Object> realmAccess = Map.of("roles", List.of("ADMIN", "STUDENT"));
        Jwt jwt = createMockJwt(Map.of("realm_access", realmAccess));

        Collection<GrantedAuthority> authorities = converter.convert(jwt);

        assertThat(authorities)
                .extracting(GrantedAuthority::getAuthority)
                .containsExactlyInAnyOrder("ROLE_ADMIN", "ROLE_STUDENT");
    }

    @Test
    @DisplayName("Should return empty list when realm_access claim is missing")
    void shouldReturnEmptyWhenRealmAccessMissing() {
        Jwt jwt = createMockJwt(Collections.emptyMap());

        Collection<GrantedAuthority> authorities = converter.convert(jwt);

        assertThat(authorities).isEmpty();
    }

    @Test
    @DisplayName("Should return empty list when roles claim is missing in realm_access")
    void shouldReturnEmptyWhenRolesMissing() {
        Map<String, Object> realmAccess = Map.of("other_field", "value");
        Jwt jwt = createMockJwt(Map.of("realm_access", realmAccess));

        Collection<GrantedAuthority> authorities = converter.convert(jwt);

        assertThat(authorities).isEmpty();
    }

    @Test
    @DisplayName("Should return empty list when roles claim is not a list")
    void shouldReturnEmptyWhenRolesNotAList() {
        Map<String, Object> realmAccess = Map.of("roles", "invalid_string");
        Jwt jwt = createMockJwt(Map.of("realm_access", realmAccess));

        Collection<GrantedAuthority> authorities = converter.convert(jwt);

        assertThat(authorities).isEmpty();
    }
}
