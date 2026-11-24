package com.nutriplan.planner.application.dto;

import com.nutriplan.planner.domain.model.ActivityLevel;
import com.nutriplan.planner.domain.model.MealType;
import com.nutriplan.planner.domain.model.Sex;
import com.nutriplan.planner.domain.model.WeightGoal;
import com.nutriplan.planner.domain.service.NutritionTargets;
import java.time.LocalDate;
import java.util.Set;

public record ProfileDto(String userId, String displayName, Sex sex, LocalDate birthDate, Double heightCm,
                         Double currentWeightKg, Double targetWeightKg, ActivityLevel activityLevel, WeightGoal goal,
                         int defaultPeople, boolean vegetarian, Set<MealType> enabledMeals,
                         Set<String> excludedIngredients, boolean complete, NutritionTargets targets) {
}
