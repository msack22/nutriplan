package com.nutriplan.recipe.application.dto;

import com.nutriplan.recipe.domain.model.DietTag;
import com.nutriplan.recipe.domain.model.MealType;

/** Filtros del listado clásico (no semántico). Todos opcionales. */
public record RecipeSearchCriteria(String text, String category, MealType mealType, DietTag tag,
                                   Double maxKcal, Double minProtein, boolean mine) {
}
