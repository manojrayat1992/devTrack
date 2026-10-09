package com.devtrack.security;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Keycloak JWT Security Converter Tests")
class KeycloakJwtAuthenticationConverterTest {

    private final KeycloakJwtAuthenticationConverter converter = new KeycloakJwtAuthenticationConverter();

    @Test
    @DisplayName("Maps Keycloak realm roles to Spring Security GrantedAuthorities")
    void mapsRealmRolesToAuthorities() {
        Jwt jwt = createMockJwt(
            Map.of("roles", List.of("user", "admin")),
            Map.of()
        );

        AbstractAuthenticationToken token = converter.convert(jwt);
        assertNotNull(token);

        Collection<GrantedAuthority> authorities = token.getAuthorities();
        List<String> authorityNames = authorities.stream().map(GrantedAuthority::getAuthority).toList();

        assertTrue(authorityNames.contains("ROLE_USER"), "Expected ROLE_USER authority");
        assertTrue(authorityNames.contains("ROLE_ADMIN"), "Expected ROLE_ADMIN authority");
    }

    @Test
    @DisplayName("Maps Keycloak resource_access client roles to Spring Security GrantedAuthorities")
    void mapsClientRolesToAuthorities() {
        Map<String, Object> resourceAccess = Map.of(
            "devtrack-backend", Map.of("roles", List.of("maintainer"))
        );

        Jwt jwt = createMockJwt(Map.of("roles", List.of("user")), resourceAccess);

        AbstractAuthenticationToken token = converter.convert(jwt);
        assertNotNull(token);

        List<String> authorityNames = token.getAuthorities().stream().map(GrantedAuthority::getAuthority).toList();
        assertTrue(authorityNames.contains("ROLE_USER"));
        assertTrue(authorityNames.contains("ROLE_MAINTAINER"));
    }

    @Test
    @DisplayName("Extracts preferred_username as principal name")
    void extractsPreferredUsername() {
        Jwt jwt = createMockJwt(Map.of("roles", List.of("user")), Map.of());
        AbstractAuthenticationToken token = converter.convert(jwt);
        assertEquals("developer_test", token.getName());
    }

    private Jwt createMockJwt(Map<String, Object> realmAccess, Map<String, Object> resourceAccess) {
        return Jwt.withTokenValue("mock-jwt-token")
            .header("alg", "RS256")
            .claim("sub", "user-uuid-12345")
            .claim("preferred_username", "developer_test")
            .claim("email", "dev@devtrack.local")
            .claim("realm_access", realmAccess)
            .claim("resource_access", resourceAccess)
            .issuedAt(Instant.now())
            .expiresAt(Instant.now().plusSeconds(3600))
            .build();
    }
}
