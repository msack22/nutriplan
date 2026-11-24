package com.nutriplan.planner.domain.port;

/** Publica eventos de integración (implementado con Transactional Outbox + Kafka). */
public interface IntegrationEventPublisher {

    void publish(String topic, String key, Object event);
}
