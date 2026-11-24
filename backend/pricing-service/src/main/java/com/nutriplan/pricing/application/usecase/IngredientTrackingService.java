package com.nutriplan.pricing.application.usecase;

import com.nutriplan.pricing.domain.model.TrackedIngredient;
import com.nutriplan.pricing.domain.repository.TrackedIngredientRepository;
import java.util.Collection;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Mantiene la lista de ingredientes a cotizar. Agregar es naturalmente IDEMPOTENTE (si ya
 * existe no hace nada), por eso el consumidor de Kafka no necesita tabla de eventos procesados.
 */
@Service
public class IngredientTrackingService {

    private final TrackedIngredientRepository tracked;

    public IngredientTrackingService(TrackedIngredientRepository tracked) {
        this.tracked = tracked;
    }

    @Transactional
    public int track(Collection<String> names) {
        Set<String> existing = tracked.findAll().stream().map(TrackedIngredient::getName).collect(Collectors.toSet());
        int added = 0;
        for (String name : names) {
            String n = name == null ? "" : name.strip().toLowerCase();
            if (n.length() >= 3 && n.length() <= 200 && !existing.contains(n) && !n.contains("gusto")) {
                tracked.save(new TrackedIngredient(n));
                existing.add(n);
                added++;
            }
        }
        return added;
    }

    @Transactional(readOnly = true)
    public long count() {
        return tracked.count();
    }
}
