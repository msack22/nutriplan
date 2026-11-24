package com.nutriplan.pricing.infrastructure.messaging;

import com.nutriplan.common.events.IngredientsChangedEvent;
import com.nutriplan.common.events.Topics;
import com.nutriplan.pricing.application.usecase.IngredientTrackingService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.BackOff;
import org.springframework.kafka.annotation.DltHandler;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.annotation.RetryableTopic;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

/** Consume los cambios del catálogo de recetas: nuevos ingredientes => nuevos precios a seguir. */
@Component
public class IngredientsChangedListener {

    private static final Logger log = LoggerFactory.getLogger(IngredientsChangedListener.class);

    private final IngredientTrackingService tracking;
    private final ObjectMapper json;

    public IngredientsChangedListener(IngredientTrackingService tracking, ObjectMapper json) {
        this.tracking = tracking;
        this.json = json;
    }

    @RetryableTopic(attempts = "3", backOff = @BackOff(delay = 2000, multiplier = 2))
    @KafkaListener(topics = Topics.INGREDIENTS_CHANGED, groupId = "pricing-service")
    public void onIngredientsChanged(String payload) {
        IngredientsChangedEvent event = json.readValue(payload, IngredientsChangedEvent.class);
        int added = tracking.track(event.ingredientNames());
        log.info("Evento {}: {} ingredientes nuevos para cotizar (total recibido {})", event.eventId(), added,
                event.ingredientNames().size());
    }

    @DltHandler
    public void dlt(String payload) {
        log.error("IngredientsChangedEvent enviado al DLT: {}", payload.length() > 200 ? payload.substring(0, 200) : payload);
    }
}
