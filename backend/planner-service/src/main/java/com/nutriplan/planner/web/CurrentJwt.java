package com.nutriplan.planner.web;

import org.springframework.security.oauth2.jwt.Jwt;

/** Extrae datos del usuario del JWT ya validado. */
final class CurrentJwt {

    private CurrentJwt() {
    }

    static String userId(Jwt jwt) {
        return jwt.getSubject();
    }

    static String name(Jwt jwt) {
        String name = jwt.getClaimAsString("name");
        return name != null ? name : jwt.getClaimAsString("preferred_username");
    }
}
