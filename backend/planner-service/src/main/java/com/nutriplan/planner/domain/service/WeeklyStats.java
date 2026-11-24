package com.nutriplan.planner.domain.service;

import com.nutriplan.planner.domain.model.NutritionSnapshot;
import java.time.LocalDate;
import java.util.List;

/**
 * Estadísticas de una semana, POR PERSONA (cada persona come 1 porción de cada comida).
 *
 * @param kcalAdherencePct promedio diario de kcal / objetivo (100% = justo en el objetivo)
 * @param dataCoveragePct  % de comidas planificadas con datos nutricionales
 */
public record WeeklyStats(
        LocalDate weekStart,
        List<DayStats> days,
        NutritionSnapshot weekTotal,
        NutritionSnapshot dailyAverage,
        NutritionTargets targets,
        Integer kcalAdherencePct,
        Integer proteinAdherencePct,
        int mealsPlanned,
        int dataCoveragePct,
        Double projectedWeeklyChangeKg) {

    public record DayStats(LocalDate date, String dayOfWeek, NutritionSnapshot total, int meals,
                           int mealsWithoutData, Integer kcalVsTargetPct) {
    }
}
