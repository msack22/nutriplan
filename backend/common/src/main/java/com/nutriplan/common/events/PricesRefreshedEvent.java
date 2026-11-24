package com.nutriplan.common.events;

import java.time.Instant;

/** Evento: terminó la actualización de precios. planner-service invalida su caché. */
public record PricesRefreshedEvent(String eventId, Instant occurredAt, int productsUpdated) {
}
