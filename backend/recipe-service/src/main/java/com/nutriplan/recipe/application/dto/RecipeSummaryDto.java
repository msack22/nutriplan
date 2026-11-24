package com.nutriplan.recipe.application.dto;

import com.nutriplan.recipe.domain.model.DietTag;
import com.nutriplan.recipe.domain.model.MealType;
import com.nutriplan.recipe.domain.model.Nutrition;
import com.nutriplan.recipe.domain.model.NutritionSource;
import java.util.Set;

/** Vista resumida para listados. Los DTO desacoplan la API del modelo JPA. */
public record RecipeSummaryDto(
        Long id,
        String title,
        String categorySlug,
        String categoryName,
        String categoryEmoji,
        int servings,
        Nutrition nutritionPerServing,
        NutritionSource nutritionSource,
        Set<MealType> mealTypes,
        Set<DietTag> tags) {
}
