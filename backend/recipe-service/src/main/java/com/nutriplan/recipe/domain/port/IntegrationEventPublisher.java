package com.nutriplan.recipe.domain.port;

/**
 * Puerto para publicar eventos de integración (hacia otros microservicios).
 * La implementación usa el patrón <b>Transactional Outbox</b>: el evento se guarda en la misma
 * transacción de base de datos que el cambio de negocio, y otro proceso lo envía a Kafka.
 * Así nunca queda "guardé en la DB pero no publiqué" (o al revés).
 */
public interface IntegrationEventPublisher {

    void publish(String topic, String key, Object event);
}
