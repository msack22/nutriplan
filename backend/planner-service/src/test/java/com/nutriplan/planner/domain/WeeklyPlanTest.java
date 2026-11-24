package com.nutriplan.planner.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.nutriplan.common.error.BusinessRuleException;
import com.nutriplan.planner.domain.model.MealType;
import com.nutriplan.planner.domain.model.NutritionSnapshot;
import com.nutriplan.planner.domain.model.RecipeRef;
import com.nutriplan.planner.domain.model.WeeklyPlan;
import com.nutriplan.planner.domain.service.WeeklyStats;
import com.nutriplan.planner.domain.service.WeeklyStatsCalculator;
import java.time.DayOfWeek;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;

class WeeklyPlanTest {

    private static final LocalDate MONDAY = LocalDate.of(2026, 11, 23);
    private static final RecipeRef SALAD = new RecipeRef(1, "Ensalada", new NutritionSnapshot(400.0, 30.0, 20.0, 15.0));
    private static final RecipeRef SOUP = new RecipeRef(2, "Sopa", new NutritionSnapshot(300.0, 10.0, 30.0, 10.0));

    @Test
    void weekMustStartOnMonday() {
        assertThatThrownBy(() -> new WeeklyPlan("u", MONDAY.plusDays(1), 2))
                .isInstanceOf(BusinessRuleException.class);
    }

    @Test
    void oneRecipePerSlotReplacesPrevious() {
        WeeklyPlan plan = new WeeklyPlan("u", MONDAY, 2);
        plan.assign(DayOfWeek.MONDAY, MealType.LUNCH, SALAD, null);
        plan.assign(DayOfWeek.MONDAY, MealType.LUNCH, SOUP, null);

        assertThat(plan.getMeals()).singleElement().satisfies(m -> {
            assertThat(m.getRecipeTitle()).isEqualTo("Sopa");
            assertThat(m.getServings()).isEqualTo(2);
        });
    }

    @Test
    void changingPeopleRescalesDefaultServingsOnly() {
        WeeklyPlan plan = new WeeklyPlan("u", MONDAY, 2);
        plan.assign(DayOfWeek.MONDAY, MealType.LUNCH, SALAD, null);     // sigue el default (2)
        plan.assign(DayOfWeek.MONDAY, MealType.DINNER, SOUP, 5);        // personalizado

        plan.changePeople(3);

        assertThat(plan.mealAt(DayOfWeek.MONDAY, MealType.LUNCH)).get().extracting("servings").isEqualTo(3);
        assertThat(plan.mealAt(DayOfWeek.MONDAY, MealType.DINNER)).get().extracting("servings").isEqualTo(5);
    }

    @Test
    void statsArePerPersonAndAveragedOverPlannedDays() {
        WeeklyPlan plan = new WeeklyPlan("u", MONDAY, 4);
        plan.assign(DayOfWeek.MONDAY, MealType.LUNCH, SALAD, null);
        plan.assign(DayOfWeek.MONDAY, MealType.DINNER, SOUP, null);
        plan.assign(DayOfWeek.TUESDAY, MealType.LUNCH, SALAD, null);

        WeeklyStats stats = WeeklyStatsCalculator.calculate(plan, null);

        assertThat(stats.weekTotal().kcal()).isEqualTo(1100.0);       // 400+300+400 (1 porción c/u)
        assertThat(stats.dailyAverage().kcal()).isEqualTo(550.0);     // 2 días con comidas
        assertThat(stats.days().getFirst().total().proteinG()).isEqualTo(40.0);
        assertThat(stats.dataCoveragePct()).isEqualTo(100);
    }
}
