package com.nutriplan.common.security;

import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Propiedades tipadas ({@code nutriplan.security.*}) para configurar el Resource Server.
 *
 * @param issuer     valor esperado del claim {@code iss} (URL pública del realm de Keycloak)
 * @param jwkSetUri  de dónde descargar las claves públicas para verificar la firma del JWT.
 *                   Puede diferir del issuer: dentro de Docker se usa {@code http://keycloak:8080},
 *                   pero el token dice {@code http://localhost:8180}.
 * @param audience   valor que debe aparecer en {@code aud}. Evita que un token emitido para
 *                   otra aplicación del mismo Keycloak sea aceptado acá (token confusion).
 * @param publicPaths paths adicionales sin autenticación.
 */
@ConfigurationProperties("nutriplan.security")
public record NutriplanSecurityProperties(
        String issuer,
        String jwkSetUri,
        String audience,
        List<String> publicPaths) {

    public NutriplanSecurityProperties {
        publicPaths = publicPaths == null ? List.of() : List.copyOf(publicPaths);
        audience = audience == null ? "nutriplan-api" : audience;
    }
}
