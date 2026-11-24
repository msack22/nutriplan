package com.nutriplan.common.events;

import java.time.Instant;
import java.util.List;

/**
 * Evento de integración: el catálogo de ingredientes cambió (p.ej. tras importar recetas).
 * pricing-service lo consume para saber qué productos debe cotizar los lunes.
 *
 * @param eventId id único -> permite a los consumidores ser idempotentes
 */
public record IngredientsChangedEvent(String eventId, Instant occurredAt, List<String> ingredientNames) {
}
