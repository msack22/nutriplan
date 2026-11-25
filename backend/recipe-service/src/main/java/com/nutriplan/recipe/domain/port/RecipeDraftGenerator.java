package com.nutriplan.recipe.domain.port;

import com.nutriplan.recipe.domain.model.MealType;
import com.nutriplan.recipe.domain.model.Nutrition;
import java.util.List;

/** Puerto: genera un BORRADOR de receta a partir de ingredientes (lo implementa un LLM). */
public interface RecipeDraftGenerator {

    RecipeDraft generate(DraftRequest request);

    /** Lo que pide el usuario. */
    record DraftRequest(List<String> ingredients, MealType mealType, int servings, String notes) {
    }

    /** Borrador editable: NO se guarda hasta que el usuario lo confirma. */
    record RecipeDraft(String title, int servings, List<String> ingredients, List<String> steps,
                       Nutrition nutritionPerServing, String generatedBy) {
    }
}
