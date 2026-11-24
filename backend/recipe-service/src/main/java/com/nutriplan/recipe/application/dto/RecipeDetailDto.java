package com.nutriplan.recipe.application.dto;

import com.nutriplan.recipe.domain.model.DietTag;
import com.nutriplan.recipe.domain.model.MealType;
import com.nutriplan.recipe.domain.model.Nutrition;
import com.nutriplan.recipe.domain.model.NutritionSource;
import java.util.List;
import java.util.Set;

/**
 * Detalle de receta escalado a {@code servings} porciones.
 *
 * @param originalServings porciones que rinde la receta original
 * @param nutritionTotal   macros totales para {@code servings} porciones
 */
public record RecipeDetailDto(
        Long id,
        String title,
        CategoryDto category,
        int servings,
        int originalServings,
        String yieldLabel,
        Nutrition nutritionPerServing,
        Nutrition nutritionTotal,
        NutritionSource nutritionSource,
        Set<MealType> mealTypes,
        Set<DietTag> tags,
        List<IngredientDto> ingredients,
        List<String> steps,
        String notes,
        String videoUrl,
        long version) {
}
