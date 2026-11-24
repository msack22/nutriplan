package com.nutriplan.planner.domain.generation;

import com.nutriplan.planner.domain.model.MealType;
import java.time.DayOfWeek;

public record MealAssignment(DayOfWeek day, MealType mealType, RecipeCandidate recipe) {
}
