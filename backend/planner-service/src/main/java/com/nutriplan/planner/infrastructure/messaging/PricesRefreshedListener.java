package com.nutriplan.planner.infrastructure.messaging;

import com.nutriplan.common.events.PricesRefreshedEvent;
import com.nutriplan.common.events.Topics;
import com.nutriplan.planner.application.usecase.ShoppingListCalculator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.DltHandler;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.annotation.RetryableTopic;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.kafka.annotation.BackOff;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.ObjectMapper;

/**
 * Consumidor Kafka: cuando pricing-service termina de actualizar precios (job de los lunes),
 * vaciamos la caché de listas de compras para que se recalculen con precios nuevos.
 *
 * <ul>
 *   <li>{@code groupId}: todas las réplicas de planner forman un "consumer group" => cada evento
 *       lo procesa UNA sola réplica.</li>
 *   <li>{@code @RetryableTopic}: si falla, reintenta en tópicos de retry con backoff
 *       (sin bloquear la partición) y al final lo manda al DLT (Dead Letter Topic).</li>
 *   <li>Idempotencia con la tabla processed_event.</li>
 * </ul>
 */
@Component
public class PricesRefreshedListener {

    private static final Logger log = LoggerFactory.getLogger(PricesRefreshedListener.class);

    private final ShoppingListCalculator shoppingLists;
    private final ProcessedEventRepository processed;
    private final ObjectMapper json;

    public PricesRefreshedListener(ShoppingListCalculator shoppingLists, ProcessedEventRepository processed,
                                   ObjectMapper json) {
        this.shoppingLists = shoppingLists;
        this.processed = processed;
        this.json = json;
    }

    @RetryableTopic(attempts = "4", backOff = @BackOff(delay = 1000, multiplier = 2))
    @KafkaListener(topics = Topics.PRICES_REFRESHED, groupId = "planner-service")
    @Transactional
    public void onPricesRefreshed(String payload) {
        PricesRefreshedEvent event = json.readValue(payload, PricesRefreshedEvent.class);
        if (processed.existsById(event.eventId())) {
            log.info("Evento {} ya procesado, se ignora (idempotencia)", event.eventId());
            return;
        }
        shoppingLists.evictAll();
        processed.save(new ProcessedEvent(event.eventId()));
        log.info("Precios actualizados ({} productos): caché de listas de compras invalidada", event.productsUpdated());
    }

    @DltHandler
    public void onDeadLetter(String payload, @Header(KafkaHeaders.RECEIVED_TOPIC) String topic) {
        log.error("Evento enviado al DLT {} tras agotar reintentos: {}", topic, payload);
    }
}
