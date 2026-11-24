package com.nutriplan.planner.domain.generation;

import com.nutriplan.planner.domain.model.MealType;
import java.time.DayOfWeek;
import java.util.Set;

/**
 * Parámetros de generación.
 *
 * @param days               días a completar (p.ej. solo lunes a viernes)
 * @param maxRepeatsPerWeek  cuántas veces puede repetirse una receta (2 = cocinar doble y repetir)
 * @param seed               semilla del random => mismo pedido, mismo plan (tests reproducibles)
 */
public record GenerationRequest(Set<DayOfWeek> days, Set<MealType> meals, double dailyKcalTarget,
                                double dailyProteinTarget, int maxRepeatsPerWeek, long seed) {
}
