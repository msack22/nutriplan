package com.nutriplan.planner.domain.model;

/** Referencia mínima a una receta de recipe-service (lo que el plan necesita guardar). */
public record RecipeRef(long id, String title, NutritionSnapshot nutritionPerServing) {
}
