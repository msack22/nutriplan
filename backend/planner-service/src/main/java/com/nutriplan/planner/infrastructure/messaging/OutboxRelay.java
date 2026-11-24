package com.nutriplan.planner.infrastructure.messaging;

import java.util.concurrent.TimeUnit;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * "Message relay" del patrón Outbox: cada 5 s toma eventos pendientes y los publica en Kafka.
 *
 * <p>Garantía: <b>at-least-once</b>. Si se cae después de enviar pero antes de marcar como
 * publicado, el evento se reenvía => los consumidores deben ser IDEMPOTENTES (usan eventId).
 * El header {@code eventType} permite a los consumidores saber qué tipo deserializar.
 */
@Component
@ConditionalOnProperty(name = "nutriplan.outbox.relay.enabled", havingValue = "true", matchIfMissing = true)
public class OutboxRelay {

    private static final Logger log = LoggerFactory.getLogger(OutboxRelay.class);
    private static final int MAX_ATTEMPTS = 10;

    private final OutboxEventRepository outbox;
    private final KafkaTemplate<String, String> kafka;

    public OutboxRelay(OutboxEventRepository outbox, KafkaTemplate<String, String> kafka) {
        this.outbox = outbox;
        this.kafka = kafka;
    }

    @Scheduled(fixedDelayString = "${nutriplan.outbox.relay.delay:5000}")
    @Transactional
    public void relay() {
        for (OutboxEvent event : outbox.findTop50ByPublishedAtIsNullAndAttemptsLessThanOrderByCreatedAtAsc(MAX_ATTEMPTS)) {
            try {
                ProducerRecord<String, String> record = new ProducerRecord<>(event.getTopic(), event.getKey(), event.getPayload());
                record.headers().add("eventType", event.getType().getBytes());
                record.headers().add("eventId", event.getId().getBytes());
                kafka.send(record).get(10, TimeUnit.SECONDS);
                event.markPublished();
                log.info("Evento {} publicado en {}", event.getType(), event.getTopic());
            } catch (Exception e) {
                if (e instanceof InterruptedException) {
                    Thread.currentThread().interrupt();
                }
                event.registerFailedAttempt();
                log.warn("No se pudo publicar el evento {} (intento {}): {}", event.getId(), event.getAttempts(),
                        e.getMessage());
                break; // Kafka caído: no insistir con el resto en esta vuelta
            }
        }
    }
}
