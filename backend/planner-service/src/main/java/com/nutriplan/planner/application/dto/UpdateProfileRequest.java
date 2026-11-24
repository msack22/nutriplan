package com.nutriplan.planner.application.dto;

import com.nutriplan.planner.domain.model.ActivityLevel;
import com.nutriplan.planner.domain.model.MealType;
import com.nutriplan.planner.domain.model.Sex;
import com.nutriplan.planner.domain.model.WeightGoal;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import java.util.Set;

/** Bean Validation (Jakarta): las reglas se declaran y Spring devuelve 400 si no se cumplen. */
public record UpdateProfileRequest(
        Sex sex,
        @Past LocalDate birthDate,
        @DecimalMin("100") @DecimalMax("250") Double heightCm,
        @DecimalMin("30") @DecimalMax("350") Double currentWeightKg,
        @DecimalMin("30") @DecimalMax("350") Double targetWeightKg,
        ActivityLevel activityLevel,
        WeightGoal goal,
        @Min(1) @Max(12) int defaultPeople,
        boolean vegetarian,
        @NotEmpty Set<MealType> enabledMeals,
        @Size(max = 50) Set<@Size(max = 100) String> excludedIngredients) {
}
