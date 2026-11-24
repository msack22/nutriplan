package com.nutriplan.planner.domain.port;

import com.nutriplan.planner.domain.generation.RecipeCandidate;
import com.nutriplan.planner.domain.model.MealType;
import com.nutriplan.planner.domain.model.RecipeRef;
import java.util.Collection;
import java.util.List;
import java.util.Set;

/**
 * Puerto hacia el catálogo de recetas (implementado por un cliente HTTP a recipe-service).
 * El dominio del planner no sabe que hay HTTP, JSON ni tokens del otro lado.
 */
public interface RecipeCatalogPort {

    RecipeRef getRecipe(long recipeId);

    List<RecipeIngredients> getIngredients(Collection<Long> recipeIds);

    List<RecipeCandidate> findCandidates(CandidateQuery query);

    record CandidateQuery(Set<MealType> mealTypes, boolean vegetarian, Set<String> excludedIngredients,
                          String preference) {
    }

    record RecipeIngredients(long recipeId, String title, int originalServings, List<IngredientLine> ingredients) {
    }

    record IngredientLine(String name, Double quantity, String unit, boolean optional) {
    }
}
