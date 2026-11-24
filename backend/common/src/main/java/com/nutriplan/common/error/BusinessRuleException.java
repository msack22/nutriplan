package com.nutriplan.common.error;

/**
 * Se viola una regla de negocio (p.ej. "un plan ya existe para esa semana").
 * Se traduce a HTTP 422 Unprocessable Content.
 */
public class BusinessRuleException extends RuntimeException {
    public BusinessRuleException(String message) {
        super(message);
    }
}
