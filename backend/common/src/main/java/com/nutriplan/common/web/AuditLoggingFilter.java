package com.nutriplan.common.web;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Audit logging + correlation id.
 *
 * <ul>
 *   <li>Lee (o genera) el header {@code X-Correlation-Id} y lo pone en el MDC de SLF4J, así
 *       todas las líneas de log de este request lo incluyen. El Gateway lo propaga a los
 *       servicios => podés seguir un request a través de todos los microservicios.</li>
 *   <li>Al finalizar, registra QUIÉN (subject del JWT) hizo QUÉ (método + path) y con qué
 *       resultado (status, duración). Es el "audit log" de la guía de seguridad.</li>
 * </ul>
 * Se registra DESPUÉS de Spring Security (ver auto-configuración) para conocer el usuario.
 */
public class AuditLoggingFilter extends OncePerRequestFilter {

    public static final String CORRELATION_HEADER = "X-Correlation-Id";
    private static final Logger audit = LoggerFactory.getLogger("AUDIT");

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {
        String correlationId = request.getHeader(CORRELATION_HEADER);
        if (correlationId == null || correlationId.isBlank() || correlationId.length() > 64) {
            correlationId = UUID.randomUUID().toString();
        }
        MDC.put("correlationId", correlationId);
        response.setHeader(CORRELATION_HEADER, correlationId);
        long start = System.nanoTime();
        try {
            chain.doFilter(request, response);
        } finally {
            long ms = (System.nanoTime() - start) / 1_000_000;
            if (!request.getRequestURI().startsWith("/actuator")) {
                audit.info("user={} method={} path={} status={} durationMs={}",
                        currentUser(), request.getMethod(), request.getRequestURI(),
                        response.getStatus(), ms);
            }
            MDC.remove("correlationId");
        }
    }

    private static String currentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return auth == null ? "anonymous" : auth.getName();
    }
}
