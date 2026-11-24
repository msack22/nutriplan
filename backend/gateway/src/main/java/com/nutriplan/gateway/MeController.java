package com.nutriplan.gateway;

import java.util.List;
import java.util.Map;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Datos del usuario logueado para la SPA (nombre, email, roles). Angular lo llama al iniciar:
 * 200 = hay sesión, 401 = mostrar botón "Ingresar". Nunca devuelve tokens.
 */
@RestController
public class MeController {

    @GetMapping("/api/me")
    public Map<String, Object> me(@AuthenticationPrincipal OidcUser user) {
        List<String> roles = extractRoles(user);
        return Map.of(
                "username", user.getPreferredUsername() == null ? "" : user.getPreferredUsername(),
                "name", user.getFullName() == null ? user.getPreferredUsername() : user.getFullName(),
                "email", user.getEmail() == null ? "" : user.getEmail(),
                "roles", roles);
    }

    @SuppressWarnings("unchecked")
    private static List<String> extractRoles(OidcUser user) {
        // los roles del realm vienen en el access token; en el id token/userinfo usamos las authorities
        Object realm = user.getClaims().get("realm_access");
        if (realm instanceof Map<?, ?> m && m.get("roles") instanceof List<?> l) {
            return (List<String>) l;
        }
        return user.getAuthorities().stream().map(GrantedAuthority::getAuthority).toList();
    }
}
