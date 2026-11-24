package com.nutriplan.pricing.infrastructure.function;

import com.nutriplan.pricing.application.dto.RefreshReport;
import com.nutriplan.pricing.application.usecase.IngredientTrackingService;
import com.nutriplan.pricing.infrastructure.client.RecipeCatalogClient;
import java.util.function.Supplier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Job semanal: TODOS LOS LUNES a las 06:00 (hora Argentina) actualiza precios.
 * Expresión cron de Spring: segundo minuto hora díaDelMes mes díaDeSemana.
 * Para hacerlo diario: {@code pricing.refresh.cron=0 0 6 * * *}.
 *
 * <p>En AWS Lambda este bean se desactiva ({@code pricing.scheduler.enabled=false}) porque el
 * disparador es EventBridge. Con varias réplicas habría que usar ShedLock para que corra en una
 * sola (lock distribuido); acá corre una instancia.
 */
@Component
@EnableScheduling
@ConditionalOnProperty(name = "pricing.scheduler.enabled", havingValue = "true", matchIfMissing = true)
public class WeeklyPriceRefreshJob {

    private static final Logger log = LoggerFactory.getLogger(WeeklyPriceRefreshJob.class);

    private final Supplier<RefreshReport> refreshPrices;
    private final IngredientTrackingService tracking;
    private final RecipeCatalogClient recipes;

    public WeeklyPriceRefreshJob(Supplier<RefreshReport> refreshPrices, IngredientTrackingService tracking,
                                 RecipeCatalogClient recipes) {
        this.refreshPrices = refreshPrices;
        this.tracking = tracking;
        this.recipes = recipes;
    }

    @Scheduled(cron = "${pricing.refresh.cron:0 0 6 * * MON}", zone = "America/Argentina/Buenos_Aires")
    public void run() {
        log.info("Job semanal de precios iniciado");
        refreshPrices.get();
    }

    /** Al arrancar sin ingredientes (primera vez), los pide a recipe-service vía client credentials. */
    @EventListener(ApplicationReadyEvent.class)
    public void bootstrapTrackedIngredients() {
        if (tracking.count() > 0) {
            return;
        }
        try {
            int added = tracking.track(recipes.ingredientNames());
            log.info("Bootstrap: {} ingredientes obtenidos de recipe-service", added);
        } catch (RuntimeException e) {
            log.warn("No se pudo obtener ingredientes de recipe-service (se recibirán por Kafka): {}", e.getMessage());
        }
    }
}
