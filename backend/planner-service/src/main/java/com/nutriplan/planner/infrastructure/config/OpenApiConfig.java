package com.nutriplan.planner.infrastructure.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.OAuthFlow;
import io.swagger.v3.oas.models.security.OAuthFlows;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Documentación OpenAPI 3 (Swagger UI en /swagger-ui.html).
 * El botón "Authorize" hace login real contra Keycloak con Authorization Code + PKCE
 * (cliente público nutriplan-swagger), así se prueban los endpoints protegidos.
 */
@Configuration
public class OpenApiConfig {

    @Bean
    OpenAPI plannerServiceOpenApi(@Value("${nutriplan.security.issuer}") String issuer) {
        String scheme = "keycloak";
        return new OpenAPI()
                .info(new Info().title("NutriPlan - Planner Service").version("v1")
                        .description("Perfil, planes semanales, generación automática, estadísticas y lista de compras."))
                .components(new Components().addSecuritySchemes(scheme, new SecurityScheme()
                        .type(SecurityScheme.Type.OAUTH2)
                        .flows(new OAuthFlows().authorizationCode(new OAuthFlow()
                                .authorizationUrl(issuer + "/protocol/openid-connect/auth")
                                .tokenUrl(issuer + "/protocol/openid-connect/token")))))
                .addSecurityItem(new SecurityRequirement().addList(scheme));
    }
}
