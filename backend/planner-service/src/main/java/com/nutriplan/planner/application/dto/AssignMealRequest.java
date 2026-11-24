package com.nutriplan.planner.application.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Positive;

public record AssignMealRequest(@Positive long recipeId, @Min(1) @Max(24) Integer servings) {
}
