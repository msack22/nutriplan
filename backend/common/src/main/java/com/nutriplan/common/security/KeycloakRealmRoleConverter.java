package com.nutriplan.common.security;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;
import org.springframework.core.convert.converter.Converter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;

/**
 * Convierte los claims del JWT emitido por Keycloak en {@link GrantedAuthority} de Spring.
 *
 * <p>Keycloak pone los roles del realm en {@code "realm_access": {"roles": ["USER", ...]}}.
 * Spring por defecto solo mira el claim {@code scope}. Este converter combina ambos:
 * <ul>
 *   <li>roles de realm -> {@code ROLE_USER}, {@code ROLE_ADMIN}, {@code ROLE_SERVICE}
 *       (usables con {@code hasRole('ADMIN')} o {@code @PreAuthorize})</li>
 *   <li>scopes OAuth2 -> {@code SCOPE_profile}, {@code SCOPE_email}...</li>
 * </ul>
 */
public class KeycloakRealmRoleConverter implements Converter<Jwt, Collection<GrantedAuthority>> {

    private final JwtGrantedAuthoritiesConverter scopes = new JwtGrantedAuthoritiesConverter();

    @Override
    public Collection<GrantedAuthority> convert(Jwt jwt) {
        return Stream.concat(scopes.convert(jwt).stream(), realmRoles(jwt).stream()).toList();
    }

    @SuppressWarnings("unchecked")
    private static List<GrantedAuthority> realmRoles(Jwt jwt) {
        Object realmAccess = jwt.getClaims().get("realm_access");
        if (!(realmAccess instanceof Map<?, ?> map) || !(map.get("roles") instanceof Collection<?> roles)) {
            return List.of();
        }
        return ((Collection<Object>) roles).stream()
                .map(r -> (GrantedAuthority) new SimpleGrantedAuthority("ROLE_" + r.toString().toUpperCase()))
                .toList();
    }
}
