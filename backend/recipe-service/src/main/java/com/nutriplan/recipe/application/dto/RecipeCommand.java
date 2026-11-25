package com.nutriplan.recipe.application.dto;

import com.nutriplan.recipe.domain.model.MealType;
import com.nutriplan.recipe.domain.model.Nutrition;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import java.util.List;
import java.util.Set;

/**
 * Alta/edición de una receta propia. Los ingredientes se escriben como texto libre
 * ("200 g de pollo", "1/2 palta"): el mismo parser del importador los interpreta.
 *
 * @param nutritionPerServing opcional; si no viene, la estima el LLM en segundo plano
 */
public record RecipeCommand(
        @NotBlank @Size(max = 120) String title,
        @NotBlank String categorySlug,
        @Min(1) @Max(24) int servings,
        Set<MealType> mealTypes,
        @NotEmpty @Size(max = 40) List<@NotBlank @Size(max = 200) String> ingredients,
        @Size(max = 30) List<@Size(max = 1000) String> steps,
        @Size(max = 2000) String notes,
        Nutrition nutritionPerServing) {

    public RecipeCommand {
        mealTypes = mealTypes == null ? Set.of() : mealTypes;
        steps = steps == null ? List.of() : steps;
    }
}
