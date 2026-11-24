package com.nutriplan.common.error;

/**
 * Excepción de dominio: el recurso pedido no existe.
 * El {@link com.nutriplan.common.web.GlobalExceptionHandler} la traduce a HTTP 404.
 * Ventaja: la capa de aplicación no conoce HTTP (clean architecture).
 */
public class ResourceNotFoundException extends RuntimeException {
    public ResourceNotFoundException(String resource, Object id) {
        super("%s con id '%s' no encontrado".formatted(resource, id));
    }
}
