package com.nutriplan.recipe.application.dto;

import com.nutriplan.recipe.domain.model.DietTag;
import com.nutriplan.recipe.domain.model.MealType;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import java.util.List;
import java.util.Set;

/**
 * Pedido de recetas candidatas para armar un plan automático (lo usa planner-service).
 *
 * @param requiredTags        p.ej. VEGETARIAN
 * @param excludedIngredients ingredientes que el usuario no come ("hongo", "atun")
 * @param preference          texto libre para ranking semántico ("comida casera con pollo")
 */
public record CandidateRequest(
        Set<MealType> mealTypes,
        Set<DietTag> requiredTags,
        List<String> excludedIngredients,
        String preference,
        @Min(1) @Max(500) Integer limit) {

    public CandidateRequest {
        mealTypes = mealTypes == null ? Set.of() : mealTypes;
        requiredTags = requiredTags == null ? Set.of() : requiredTags;
        excludedIngredients = excludedIngredients == null ? List.of() : excludedIngredients;
        limit = limit == null ? 200 : limit;
    }
}
