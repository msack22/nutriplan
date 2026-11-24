package com.nutriplan.recipe.domain.service;

import com.nutriplan.recipe.domain.model.RecipeIngredient;
import com.nutriplan.recipe.domain.model.Unit;

/** Resultado inmutable de interpretar una línea de ingrediente. */
public record ParsedIngredient(String rawText, String name, Double quantity, Unit unit, boolean optional) {

    public RecipeIngredient toEntity() {
        return new RecipeIngredient(rawText, name, quantity, unit, optional);
    }
}
