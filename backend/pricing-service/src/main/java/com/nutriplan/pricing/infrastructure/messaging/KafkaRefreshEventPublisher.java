package com.nutriplan.pricing.infrastructure.messaging;

import com.nutriplan.common.events.PricesRefreshedEvent;
import com.nutriplan.common.events.Topics;
import com.nutriplan.pricing.domain.port.RefreshEventPublisher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

/**
 * Publica directo con KafkaTemplate (sin outbox): acá el evento es solo un "aviso" de
 * invalidación de caché; si se perdiera, la caché igual expira por TTL. Comparar con el outbox
 * de recipe-service, donde el evento SÍ debe ser consistente con la transacción.
 */
@Component
public class KafkaRefreshEventPublisher implements RefreshEventPublisher {

    private static final Logger log = LoggerFactory.getLogger(KafkaRefreshEventPublisher.class);

    private final KafkaTemplate<String, String> kafka;
    private final ObjectMapper json;

    public KafkaRefreshEventPublisher(KafkaTemplate<String, String> kafka, ObjectMapper json) {
        this.kafka = kafka;
        this.json = json;
    }

    @Override
    public void publish(PricesRefreshedEvent event) {
        kafka.send(Topics.PRICES_REFRESHED, event.eventId(), json.writeValueAsString(event))
                .whenComplete((r, e) -> {
                    if (e != null) {
                        log.warn("No se pudo publicar PricesRefreshedEvent: {}", e.getMessage());
                    }
                });
    }
}
