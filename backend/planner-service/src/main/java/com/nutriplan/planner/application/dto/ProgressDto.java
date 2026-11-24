package com.nutriplan.planner.application.dto;

import com.nutriplan.planner.domain.service.NutritionTargets;
import java.time.LocalDate;
import java.util.List;

/** Evolución: pesos registrados + resumen de cada semana planificada. */
public record ProgressDto(List<WeightDto> weights, List<WeekSummary> weeks, NutritionTargets targets,
                          Double startWeightKg, Double currentWeightKg, Double targetWeightKg, Double lostKg) {

    public record WeekSummary(LocalDate weekStart, Double avgDailyKcal, Double avgDailyProteinG,
                              Integer kcalAdherencePct, int mealsPlanned, int mealsEaten) {
    }
}
