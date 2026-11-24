package com.nutriplan.common.security;

import com.nutriplan.common.web.AuditLoggingFilter;
import com.nutriplan.common.web.GlobalExceptionHandler;
import java.util.List;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtClaimValidator;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.AnonymousAuthenticationFilter;

/**
 * Auto-configuración compartida por todos los microservicios "Resource Server".
 *
 * <p>Se registra en {@code META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports}
 * (así funcionan los starters de Spring Boot). Cada servicio solo agrega la dependencia
 * {@code nutriplan-common} y obtiene: validación de JWT, roles de Keycloak, method security,
 * manejo de errores y audit log. Si un servicio necesita algo distinto, declara su propio
 * bean y este se desactiva gracias a {@link ConditionalOnMissingBean}.
 *
 * <p>Ver docs/03-seguridad.md para el detalle del flujo JWT.
 */
// Debe registrarse ANTES que la seguridad por defecto de Spring Boot; si no, el starter
// oauth2-client activa su propia cadena (form login => redirect 302 en lugar de 401).
@AutoConfiguration(beforeName = {
        "org.springframework.boot.security.autoconfigure.web.servlet.ServletWebSecurityAutoConfiguration",
        "org.springframework.boot.security.autoconfigure.actuate.web.servlet.ManagementWebSecurityAutoConfiguration",
        "org.springframework.boot.security.oauth2.client.autoconfigure.servlet.OAuth2ClientWebSecurityAutoConfiguration",
        "org.springframework.boot.security.oauth2.server.resource.autoconfigure.servlet.OAuth2ResourceServerAutoConfiguration"})
@EnableMethodSecurity // habilita @PreAuthorize / @Secured
@EnableConfigurationProperties(NutriplanSecurityProperties.class)
@ConditionalOnProperty(prefix = "nutriplan.security.resource-server", name = "enabled", matchIfMissing = true)
public class ResourceServerSecurityAutoConfiguration {

    private static final List<String> ALWAYS_PUBLIC = List.of(
            "/actuator/health/**", "/actuator/info", "/actuator/prometheus",
            "/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html", "/error");

    /**
     * Decodifica y VALIDA el JWT: firma (claves públicas JWKS), expiración (exp/nbf),
     * emisor (iss) y audiencia (aud).
     */
    @Bean
    @ConditionalOnMissingBean
    JwtDecoder jwtDecoder(NutriplanSecurityProperties props) {
        NimbusJwtDecoder decoder = NimbusJwtDecoder.withJwkSetUri(props.jwkSetUri()).build();
        OAuth2TokenValidator<Jwt> audience = new JwtClaimValidator<List<String>>(
                "aud", aud -> aud != null && aud.contains(props.audience()));
        decoder.setJwtValidator(new DelegatingOAuth2TokenValidator<>(
                JwtValidators.createDefaultWithIssuer(props.issuer()), audience));
        return decoder;
    }

    @Bean
    @ConditionalOnMissingBean
    JwtAuthenticationConverter jwtAuthenticationConverter() {
        JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
        converter.setJwtGrantedAuthoritiesConverter(new KeycloakRealmRoleConverter());
        converter.setPrincipalClaimName("sub");
        return converter;
    }

    /**
     * Cadena de filtros de seguridad por defecto (SecurityFilterChain).
     * <ul>
     *   <li>Stateless: no hay sesión HTTP, cada request trae su JWT.</li>
     *   <li>CSRF deshabilitado: es seguro porque NO usamos cookies acá (el token viaja en el
     *       header Authorization). En el Gateway (que sí usa cookie de sesión) CSRF está activo.</li>
     *   <li>{@code /internal/**}: solo otros microservicios (rol SERVICE, client credentials).</li>
     * </ul>
     */
    @Bean
    @ConditionalOnMissingBean(SecurityFilterChain.class)
    SecurityFilterChain resourceServerFilterChain(HttpSecurity http,
                                                  NutriplanSecurityProperties props,
                                                  JwtAuthenticationConverter converter) throws Exception {
        http.csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(ALWAYS_PUBLIC.toArray(String[]::new)).permitAll()
                        .requestMatchers(props.publicPaths().toArray(String[]::new)).permitAll()
                        .requestMatchers("/internal/**").hasRole("SERVICE")
                        .requestMatchers("/actuator/**").hasRole("ADMIN")
                        .anyRequest().authenticated())
                .oauth2ResourceServer(o -> o.jwt(jwt -> jwt.jwtAuthenticationConverter(converter)))
                .headers(h -> h
                        .frameOptions(f -> f.deny())
                        .contentTypeOptions(c -> {})
                        .httpStrictTransportSecurity(hsts -> hsts.maxAgeInSeconds(31_536_000)))
                .addFilterAfter(new AuditLoggingFilter(), AnonymousAuthenticationFilter.class);
        return http.build();
    }

    @Bean
    @ConditionalOnMissingBean
    GlobalExceptionHandler globalExceptionHandler() {
        return new GlobalExceptionHandler();
    }
}
