package com.nutriplan.planner.application.dto;

import com.nutriplan.planner.domain.model.MealType;
import com.nutriplan.planner.domain.model.NutritionSnapshot;
import com.nutriplan.planner.domain.model.WeeklyPlan;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.List;

public record PlanDto(Long id, LocalDate weekStart, int people, long version, List<MealDto> meals) {

    public record MealDto(DayOfWeek day, LocalDate date, MealType mealType, long recipeId, String recipeTitle,
                          int servings, NutritionSnapshot nutritionPerServing, boolean eaten) {
    }

    public static PlanDto from(WeeklyPlan p) {
        return new PlanDto(p.getId(), p.getWeekStart(), p.getPeople(), p.getVersion(), p.getMeals().stream()
                .map(m -> new MealDto(m.getDay(), p.dateOf(m.getDay()), m.getMealType(), m.getRecipeId(),
                        m.getRecipeTitle(), m.getServings(), m.getNutritionPerServing(), m.isEaten()))
                .toList());
    }
}
