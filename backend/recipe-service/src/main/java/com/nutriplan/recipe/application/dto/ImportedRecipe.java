package com.nutriplan.recipe.application.dto;

import com.nutriplan.recipe.domain.model.Nutrition;
import java.util.List;

/**
 * Receta leída desde una fuente externa, ya parseada pero todavía sin persistir.
 * Es el "contrato" entre los readers (adapters de entrada) y el caso de uso de importación.
 */
public record ImportedRecipe(
        String externalId,
        String title,
        String categorySlug,
        String categoryName,
        String categoryEmoji,
        int categoryOrder,
        String subcategory,
        int servings,
        String yieldLabel,
        List<String> ingredientLines,
        List<String> steps,
        String notes,
        Nutrition nutritionPerServing,
        String videoUrl) {
}
