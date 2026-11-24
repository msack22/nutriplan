package com.nutriplan.common.security;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;

class KeycloakRealmRoleConverterTest {

    private final KeycloakRealmRoleConverter converter = new KeycloakRealmRoleConverter();

    @Test
    void mapsRealmRolesAndScopesToAuthorities() {
        Jwt jwt = Jwt.withTokenValue("t").header("alg", "RS256")
                .claim("sub", "user-1")
                .claim("scope", "profile email")
                .claim("realm_access", Map.of("roles", List.of("user", "admin")))
                .issuedAt(Instant.now()).expiresAt(Instant.now().plusSeconds(60))
                .build();

        assertThat(converter.convert(jwt)).extracting(GrantedAuthority::getAuthority)
                .containsExactlyInAnyOrder("SCOPE_profile", "SCOPE_email", "ROLE_USER", "ROLE_ADMIN");
    }

    @Test
    void returnsOnlyScopesWhenNoRealmAccessClaim() {
        Jwt jwt = Jwt.withTokenValue("t").header("alg", "RS256")
                .claim("sub", "svc").claim("scope", "openid").build();

        assertThat(converter.convert(jwt)).extracting(GrantedAuthority::getAuthority)
                .containsExactly("SCOPE_openid");
    }
}
