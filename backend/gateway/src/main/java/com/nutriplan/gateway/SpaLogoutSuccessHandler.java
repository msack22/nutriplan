package com.nutriplan.gateway;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.oidc.web.logout.OidcClientInitiatedLogoutSuccessHandler;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;

/**
 * Logout para SPA: un POST /logout hecho con XHR no puede seguir un redirect a otro origen
 * (Keycloak) por CORS. En vez de 302 devolvemos 200 con la URL de "end session" de Keycloak y
 * Angular navega a ella => se cierra la sesión del gateway (Redis) y la de Keycloak (SSO).
 */
public class SpaLogoutSuccessHandler extends OidcClientInitiatedLogoutSuccessHandler {

    public SpaLogoutSuccessHandler(ClientRegistrationRepository registrations, String postLogoutRedirectUri) {
        super(registrations);
        setPostLogoutRedirectUri(postLogoutRedirectUri);
    }

    @Override
    public void onLogoutSuccess(HttpServletRequest request, HttpServletResponse response,
                                Authentication authentication) throws IOException {
        String target = determineTargetUrl(request, response, authentication);
        response.setStatus(HttpServletResponse.SC_OK);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.getWriter().write("{\"logoutUrl\":\"" + target.replace("\"", "%22") + "\"}");
    }
}
