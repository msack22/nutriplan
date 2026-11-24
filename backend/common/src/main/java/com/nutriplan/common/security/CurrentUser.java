package com.nutriplan.common.security;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

/** Helper para obtener el usuario autenticado (claim {@code sub} del JWT). */
public final class CurrentUser {

    private CurrentUser() {
    }

    /** Id estable del usuario en Keycloak (UUID). Lo usamos como clave de sus datos. */
    public static String id() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth instanceof JwtAuthenticationToken jwt) {
            return jwt.getToken().getSubject();
        }
        throw new IllegalStateException("No hay un usuario autenticado con JWT");
    }

    /** El access token crudo, para propagarlo a otro microservicio (token propagation). */
    public static String bearerToken() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth instanceof JwtAuthenticationToken jwt) {
            return jwt.getToken().getTokenValue();
        }
        throw new IllegalStateException("No hay un token para propagar");
    }
}
