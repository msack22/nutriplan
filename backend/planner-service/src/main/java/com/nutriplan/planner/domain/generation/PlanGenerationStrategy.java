package com.nutriplan.planner.domain.generation;

import com.nutriplan.planner.domain.model.MealType;
import java.util.List;
import java.util.Map;

/**
 * Patrón <b>Strategy</b>: distintas formas de armar un plan automático con la misma interfaz.
 * El caso de uso elige la estrategia por nombre (ver {@link PlanGeneratorRegistry}), así agregar
 * una nueva (p.ej. "high-protein" o una basada en programación lineal) no toca el código existente.
 */
public interface PlanGenerationStrategy {

    String name();

    String description();

    List<MealAssignment> generate(GenerationRequest request, Map<MealType, List<RecipeCandidate>> candidatesByMeal);
}
