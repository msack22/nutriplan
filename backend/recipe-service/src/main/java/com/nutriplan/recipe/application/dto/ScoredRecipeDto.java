package com.nutriplan.recipe.application.dto;

/** Resultado de búsqueda semántica: receta + similitud coseno (0..1). */
public record ScoredRecipeDto(RecipeSummaryDto recipe, double score) {
}
