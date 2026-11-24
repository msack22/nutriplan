package com.nutriplan.gateway;

import static org.springframework.cloud.gateway.server.mvc.filter.BeforeFilterFunctions.uri;
import static org.springframework.cloud.gateway.server.mvc.filter.TokenRelayFilterFunctions.tokenRelay;
import static org.springframework.cloud.gateway.server.mvc.handler.GatewayRouterFunctions.route;
import static org.springframework.cloud.gateway.server.mvc.handler.HandlerFunctions.http;
import static org.springframework.web.servlet.function.RequestPredicates.path;

import com.nutriplan.common.web.AuditLoggingFilter;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.function.RouterFunction;
import org.springframework.web.servlet.function.ServerRequest;
import org.springframework.web.servlet.function.ServerResponse;

/**
 * Rutas del Gateway (Spring Cloud Gateway Server MVC, DSL Java => verificado al compilar).
 *
 * <p>{@code tokenRelay()}: toma el access token guardado en la SESIÓN del usuario (Redis) y lo
 * agrega como {@code Authorization: Bearer ...} al request hacia el microservicio. Si expiró,
 * lo renueva con el refresh token. El navegador nunca ve el token.
 *
 * <p>Los paths {@code /internal/**} NO se enrutan: son solo para tráfico entre servicios.
 */
@Configuration
public class RoutesConfig {

    @Bean
    RouterFunction<ServerResponse> recipeRoutes(@Value("${nutriplan.routes.recipe}") String url) {
        return route("recipe-service")
                .route(path("/api/recipes/**").or(path("/api/categories/**")).or(path("/api/admin/recipes/**")), http())
                .before(uri(url))
                .before(RoutesConfig::correlationId)
                .filter(tokenRelay())
                .build();
    }

    @Bean
    RouterFunction<ServerResponse> plannerRoutes(@Value("${nutriplan.routes.planner}") String url) {
        return route("planner-service")
                .route(path("/api/profile/**").or(path("/api/plans/**")).or(path("/api/progress/**")), http())
                .before(uri(url))
                .before(RoutesConfig::correlationId)
                .filter(tokenRelay())
                .build();
    }

    @Bean
    RouterFunction<ServerResponse> pricingRoutes(@Value("${nutriplan.routes.pricing}") String url) {
        return route("pricing-service")
                .route(path("/api/prices/**").or(path("/api/admin/prices/**")), http())
                .before(uri(url))
                .before(RoutesConfig::correlationId)
                .filter(tokenRelay())
                .build();
    }

    /** Propaga el correlation id para poder seguir el request en los logs de todos los servicios. */
    private static ServerRequest correlationId(ServerRequest request) {
        String id = MDC.get("correlationId");
        return id == null ? request
                : ServerRequest.from(request).header(AuditLoggingFilter.CORRELATION_HEADER, id).build();
    }
}
