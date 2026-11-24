package com.nutriplan.planner.infrastructure.messaging;

import com.nutriplan.planner.domain.port.IntegrationEventPublisher;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.ObjectMapper;

/**
 * Implementación del puerto con Transactional Outbox.
 * {@code Propagation.MANDATORY}: exige que ya exista una transacción (la del caso de uso);
 * si alguien lo llama fuera de una, falla => protege la garantía de atomicidad.
 */
@Component
public class OutboxEventPublisher implements IntegrationEventPublisher {

    private final OutboxEventRepository outbox;
    private final ObjectMapper json;

    public OutboxEventPublisher(OutboxEventRepository outbox, ObjectMapper json) {
        this.outbox = outbox;
        this.json = json;
    }

    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public void publish(String topic, String key, Object event) {
        outbox.save(new OutboxEvent(topic, key, event.getClass().getSimpleName(), json.writeValueAsString(event)));
    }
}
