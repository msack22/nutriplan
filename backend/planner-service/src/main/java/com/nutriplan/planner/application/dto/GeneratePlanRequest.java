package com.nutriplan.planner.application.dto;

import com.nutriplan.planner.domain.model.MealType;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import java.time.DayOfWeek;
import java.util.Set;

/**
 * Pedido de plan automático. Todo opcional: lo que falte se toma del perfil.
 *
 * @param keepExisting   true = solo completa los huecos libres (respeta lo que el usuario eligió)
 * @param preference     texto libre: "comida casera, mucho pollo y verduras" (ranking semántico)
 *
 * <p>Los campos opcionales son wrappers ({@code Boolean}, {@code Integer}): Jackson 3 falla por
 * defecto si un primitivo no viene en el JSON (FAIL_ON_NULL_FOR_PRIMITIVES).
 */
public record GeneratePlanRequest(
        String strategy,
        Set<DayOfWeek> days,
        Set<MealType> meals,
        @Min(1) @Max(12) Integer people,
        @Size(max = 300) String preference,
        @Min(1) @Max(7) Integer maxRepeatsPerWeek,
        @Min(1000) @Max(5000) Integer dailyKcal,
        Boolean vegetarian,
        Boolean keepExisting,
        Long seed) {

    public boolean keepExistingMeals() {
        return Boolean.TRUE.equals(keepExisting);
    }
}
