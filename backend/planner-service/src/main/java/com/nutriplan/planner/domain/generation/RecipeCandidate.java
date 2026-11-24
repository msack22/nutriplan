package com.nutriplan.planner.domain.generation;

import com.nutriplan.planner.domain.model.MealType;
import com.nutriplan.planner.domain.model.NutritionSnapshot;
import com.nutriplan.planner.domain.model.RecipeRef;
import java.util.Set;

/** Receta candidata para el generador, con su afinidad semántica a la preferencia del usuario. */
public record RecipeCandidate(long id, String title, Set<MealType> mealTypes, NutritionSnapshot nutrition,
                              double preferenceScore) {

    public RecipeRef toRef() {
        return new RecipeRef(id, title, nutrition);
    }

    public boolean hasKcal() {
        return nutrition != null && nutrition.hasCalories();
    }
}
