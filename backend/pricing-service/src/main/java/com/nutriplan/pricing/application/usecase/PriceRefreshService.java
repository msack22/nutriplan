package com.nutriplan.pricing.application.usecase;

import com.nutriplan.common.events.PricesRefreshedEvent;
import com.nutriplan.pricing.application.dto.RefreshReport;
import com.nutriplan.pricing.domain.model.PriceHistory;
import com.nutriplan.pricing.domain.model.ProductOffer;
import com.nutriplan.pricing.domain.model.ProductPrice;
import com.nutriplan.pricing.domain.model.TrackedIngredient;
import com.nutriplan.pricing.domain.port.RefreshEventPublisher;
import com.nutriplan.pricing.domain.port.SupermarketCatalog;
import com.nutriplan.pricing.domain.repository.PriceHistoryRepository;
import com.nutriplan.pricing.domain.repository.ProductPriceRepository;
import com.nutriplan.pricing.domain.repository.TrackedIngredientRepository;
import com.nutriplan.pricing.domain.service.PackageSizeParser;
import com.nutriplan.pricing.domain.service.ProductMatcher;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Job de actualización de precios (lo dispara el scheduler los lunes, o EventBridge si corre
 * como AWS Lambda). Para cada ingrediente seguido y cada cadena: busca, elige el mejor producto,
 * guarda el precio y el historial. Al final publica {@code PricesRefreshedEvent} en Kafka.
 *
 * <p>Buenas prácticas de scraping responsable: pausa entre requests (throttle), pocas cadenas,
 * una vez por semana y circuit breaker si una cadena falla.
 */
@Service
public class PriceRefreshService {

    private static final Logger log = LoggerFactory.getLogger(PriceRefreshService.class);

    private final List<SupermarketCatalog> catalogs;
    private final TrackedIngredientRepository tracked;
    private final ProductPriceRepository prices;
    private final PriceHistoryRepository history;
    private final RefreshEventPublisher events;
    private final TransactionTemplate tx;
    private final Clock clock;
    private final long throttleMs;
    private final AtomicBoolean running = new AtomicBoolean(false);

    public PriceRefreshService(List<SupermarketCatalog> catalogs, TrackedIngredientRepository tracked,
                               ProductPriceRepository prices, PriceHistoryRepository history,
                               RefreshEventPublisher events, TransactionTemplate tx, Clock clock,
                               @Value("${pricing.refresh.throttle-ms:150}") long throttleMs) {
        this.catalogs = catalogs;
        this.tracked = tracked;
        this.prices = prices;
        this.history = history;
        this.events = events;
        this.tx = tx;
        this.clock = clock;
        this.throttleMs = throttleMs;
    }

    /** @param limit máximo de ingredientes a procesar (null = todos). Útil para pruebas. */
    public RefreshReport refreshAll(Integer limit) {
        if (!running.compareAndSet(false, true)) {
            throw new IllegalStateException("Ya hay una actualización de precios en curso");
        }
        Instant start = Instant.now(clock);
        int updated = 0;
        int notFound = 0;
        int errors = 0;
        try {
            List<String> names = tracked.findAll().stream().map(TrackedIngredient::getName).sorted()
                    .limit(limit == null ? Long.MAX_VALUE : limit).toList();
            log.info("Actualizando precios de {} ingredientes en {} cadenas", names.size(), catalogs.size());
            for (String ingredient : names) {
                for (SupermarketCatalog catalog : catalogs) {
                    try {
                        if (refreshOne(catalog, ingredient)) {
                            updated++;
                        } else {
                            notFound++;
                        }
                    } catch (RuntimeException e) {
                        errors++;
                        log.debug("Error cotizando {} en {}: {}", ingredient, catalog.chainId(), e.getMessage());
                    }
                    sleep();
                }
            }
            RefreshReport report = new RefreshReport(UUID.randomUUID().toString(), Instant.now(clock), names.size(),
                    updated, notFound, errors, Duration.between(start, Instant.now(clock)));
            events.publish(new PricesRefreshedEvent(report.eventId(), report.finishedAt(), updated));
            log.info("Actualización de precios terminada: {}", report);
            return report;
        } finally {
            running.set(false);
        }
    }

    /** Busca en vivo un ingrediente en una cadena y persiste el resultado. */
    public Optional<ProductPrice> lookupAndStore(SupermarketCatalog catalog, String ingredient) {
        return refreshOne(catalog, ingredient) ? prices.findByChainAndIngredient(catalog.chainId(), ingredient)
                : Optional.empty();
    }

    private boolean refreshOne(SupermarketCatalog catalog, String ingredient) {
        List<ProductOffer> offers = catalog.search(ingredient, 15);
        Optional<ProductOffer> best = ProductMatcher.bestMatch(ingredient, offers);
        if (best.isEmpty()) {
            return false;
        }
        ProductOffer offer = best.get();
        tx.executeWithoutResult(s -> {
            ProductPrice price = prices.findByChainAndIngredient(catalog.chainId(), ingredient)
                    .orElseGet(() -> new ProductPrice(catalog.chainId(), ingredient));
            price.update(offer, PackageSizeParser.parse(offer.name()).orElse(null), Instant.now(clock));
            prices.save(price);
            history.save(new PriceHistory(catalog.chainId(), ingredient, LocalDate.now(clock), offer.price(),
                    offer.name()));
        });
        return true;
    }

    private void sleep() {
        try {
            Thread.sleep(throttleMs);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
