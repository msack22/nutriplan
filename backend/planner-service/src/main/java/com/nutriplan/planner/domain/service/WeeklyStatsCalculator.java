package com.nutriplan.planner.domain.service;

import com.nutriplan.planner.domain.model.NutritionSnapshot;
import com.nutriplan.planner.domain.model.PlannedMeal;
import com.nutriplan.planner.domain.model.WeeklyPlan;
import java.time.DayOfWeek;
import java.util.ArrayList;
import java.util.List;

/** Calcula las estadísticas nutricionales de un plan. Puro: entra un plan, sale un reporte. */
public final class WeeklyStatsCalculator {

    private WeeklyStatsCalculator() {
    }

    /** @param targets puede ser null si el perfil está incompleto */
    public static WeeklyStats calculate(WeeklyPlan plan, NutritionTargets targets) {
        List<WeeklyStats.DayStats> days = new ArrayList<>();
        NutritionSnapshot week = NutritionSnapshot.ZERO;
        int planned = 0;
        int withData = 0;
        int daysWithMeals = 0;

        for (DayOfWeek day : DayOfWeek.values()) {
            List<PlannedMeal> meals = plan.getMeals().stream().filter(m -> m.getDay() == day).toList();
            NutritionSnapshot total = NutritionSnapshot.ZERO;
            int missing = 0;
            for (PlannedMeal m : meals) {
                if (m.getNutritionPerServing() != null && m.getNutritionPerServing().hasCalories()) {
                    total = total.plus(m.getNutritionPerServing());
                    withData++;
                } else {
                    missing++;
                }
            }
            planned += meals.size();
            if (!meals.isEmpty()) {
                daysWithMeals++;
            }
            week = week.plus(total);
            Integer pct = targets == null || meals.isEmpty() ? null : pct(total.kcal(), targets.dailyKcal());
            days.add(new WeeklyStats.DayStats(plan.dateOf(day), day.name(), total.divide(1), meals.size(), missing, pct));
        }

        NutritionSnapshot avg = week.divide(Math.max(1, daysWithMeals));
        Integer kcalAdh = targets == null || daysWithMeals == 0 ? null : pct(avg.kcal(), targets.dailyKcal());
        Integer protAdh = targets == null || daysWithMeals == 0 ? null : pct(avg.proteinG(), targets.proteinG());
        Double projected = null;
        if (targets != null && daysWithMeals > 0) {
            // balance energético: (ingesta - gasto) * 7 / 7700 kcal por kg
            projected = Math.round((avg.kcal() - targets.tdee()) * 7 / EnergyCalculator.KCAL_PER_KG_FAT * 100) / 100.0;
        }
        int coverage = planned == 0 ? 0 : (int) Math.round(100.0 * withData / planned);
        return new WeeklyStats(plan.getWeekStart(), days, week.divide(1), avg, targets, kcalAdh, protAdh, planned,
                coverage, projected);
    }

    private static Integer pct(Double value, double target) {
        return value == null || target <= 0 ? null : (int) Math.round(100 * value / target);
    }
}
