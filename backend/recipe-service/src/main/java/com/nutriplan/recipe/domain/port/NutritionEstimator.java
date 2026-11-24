package com.nutriplan.recipe.domain.port;

import com.nutriplan.recipe.domain.model.Nutrition;
import java.util.List;
import java.util.Optional;

/** Puerto: estima macros por porción para recetas que no los traen (implementado con un LLM). */
public interface NutritionEstimator {

    Optional<Nutrition> estimatePerServing(String title, int servings, List<String> ingredientLines);
}
