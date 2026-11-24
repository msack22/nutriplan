package com.nutriplan.recipe.application.dto;

import com.nutriplan.recipe.domain.model.DietTag;
import com.nutriplan.recipe.domain.model.MealType;
import com.nutriplan.recipe.domain.model.Nutrition;
import java.util.Set;

public record RecipeCandidateDto(Long id, String title, Set<MealType> mealTypes, Set<DietTag> tags,
                                 Nutrition nutritionPerServing, double preferenceScore) {
}
