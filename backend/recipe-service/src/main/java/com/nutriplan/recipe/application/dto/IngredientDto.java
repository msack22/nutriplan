package com.nutriplan.recipe.application.dto;

import com.nutriplan.recipe.domain.model.Unit;

/** Ingrediente ya escalado a la cantidad de porciones pedida. */
public record IngredientDto(String name, Double quantity, Unit unit, String rawText, boolean optional) {
}
