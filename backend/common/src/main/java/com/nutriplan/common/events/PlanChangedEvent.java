package com.nutriplan.common.events;

import java.time.Instant;
import java.time.LocalDate;

/** Evento: un usuario modificó su plan semanal (publicado con Transactional Outbox). */
public record PlanChangedEvent(String eventId, Instant occurredAt, long planId, String userId,
                               LocalDate weekStart, String change) {
}
