package com.nutriplan.planner.domain.generation;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/** Patrón <b>Factory/Registry</b>: resuelve la estrategia por nombre. */
public class PlanGeneratorRegistry {

    private final Map<String, PlanGenerationStrategy> strategies;

    public PlanGeneratorRegistry(List<PlanGenerationStrategy> strategies) {
        this.strategies = strategies.stream()
                .collect(Collectors.toMap(PlanGenerationStrategy::name, Function.identity()));
    }

    public PlanGenerationStrategy get(String name) {
        PlanGenerationStrategy s = strategies.get(name == null ? "balanced" : name);
        if (s == null) {
            throw new IllegalArgumentException("Estrategia desconocida: " + name + ". Disponibles: " + strategies.keySet());
        }
        return s;
    }

    public List<PlanGenerationStrategy> all() {
        return List.copyOf(strategies.values());
    }
}
