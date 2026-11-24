package com.nutriplan.gateway;

import com.nutriplan.common.web.AuditLoggingFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientManager;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientProviderBuilder;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.web.DefaultOAuth2AuthorizationRequestResolver;
import org.springframework.security.oauth2.client.web.DefaultOAuth2AuthorizedClientManager;
import org.springframework.security.oauth2.client.web.OAuth2AuthorizationRequestCustomizers;
import org.springframework.security.oauth2.client.web.OAuth2AuthorizedClientRepository;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.AnonymousAuthenticationFilter;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.security.web.header.writers.ReferrerPolicyHeaderWriter;
import org.springframework.security.web.servlet.util.matcher.PathPatternRequestMatcher;

/**
 * Seguridad del BFF:
 * <ul>
 *   <li><b>oauth2Login</b> con Authorization Code + <b>PKCE</b> contra Keycloak. Cliente
 *       CONFIDENCIAL: el secret vive en el servidor, nunca en el JavaScript.</li>
 *   <li>Sesión HTTP guardada en <b>Redis</b> (Spring Session) => el gateway puede escalar
 *       horizontalmente y la sesión sobrevive reinicios. Cookie HttpOnly + SameSite=Lax.</li>
 *   <li><b>CSRF</b> activo (usamos cookies => es necesario). {@code csrf.spa()} configura el
 *       patrón cookie XSRF-TOKEN + header X-XSRF-TOKEN que Angular maneja automáticamente.</li>
 *   <li>Para /api/** devolvemos 401 (no redirect): la SPA decide redirigir al login.</li>
 *   <li>Logout "RP-initiated": cierra la sesión local Y la de Keycloak (SSO).</li>
 * </ul>
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    SecurityFilterChain bffSecurity(HttpSecurity http, ClientRegistrationRepository registrations) throws Exception {
        var pkce = new DefaultOAuth2AuthorizationRequestResolver(registrations, "/oauth2/authorization");
        pkce.setAuthorizationRequestCustomizer(OAuth2AuthorizationRequestCustomizers.withPkce());

        // "{baseUrl}" = el host por el que entró el usuario (localhost o la IP de la red):
        // la app funciona desde cualquier PC de la red sin URLs fijas.
        var oidcLogout = new SpaLogoutSuccessHandler(registrations, "{baseUrl}/");

        http.authorizeHttpRequests(auth -> auth
                        .requestMatchers("/actuator/health/**", "/actuator/info", "/error").permitAll()
                        .requestMatchers("/oauth2/**", "/login/**").permitAll()
                        .requestMatchers("/internal/**").denyAll()
                        .anyRequest().authenticated())
                .oauth2Login(o -> o
                        .authorizationEndpoint(a -> a.authorizationRequestResolver(pkce))
                        .defaultSuccessUrl("/", true))   // relativo: vuelve al mismo host
                .oauth2Client(c -> { })
                .logout(l -> l.logoutSuccessHandler(oidcLogout))
                .csrf(csrf -> csrf.spa())
                .exceptionHandling(e -> e.defaultAuthenticationEntryPointFor(
                        new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED),
                        PathPatternRequestMatcher.withDefaults().matcher("/api/**")))
                .headers(h -> h
                        .contentSecurityPolicy(csp -> csp.policyDirectives(
                                "default-src 'self'; frame-ancestors 'none'; object-src 'none'"))
                        .referrerPolicy(r -> r.policy(ReferrerPolicyHeaderWriter.ReferrerPolicy.SAME_ORIGIN)))
                .addFilterAfter(new AuditLoggingFilter(), AnonymousAuthenticationFilter.class);
        return http.build();
    }

    /** Permite al tokenRelay renovar el access token con el refresh token cuando expira. */
    @Bean
    OAuth2AuthorizedClientManager authorizedClientManager(ClientRegistrationRepository registrations,
                                                          OAuth2AuthorizedClientRepository clients) {
        var manager = new DefaultOAuth2AuthorizedClientManager(registrations, clients);
        manager.setAuthorizedClientProvider(OAuth2AuthorizedClientProviderBuilder.builder()
                .authorizationCode().refreshToken().build());
        return manager;
    }
}
