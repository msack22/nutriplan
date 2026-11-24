package com.nutriplan.planner.domain.service;

/**
 * Objetivos diarios calculados para el usuario.
 *
 * @param bmr  tasa metabólica basal (lo que gasta el cuerpo en reposo)
 * @param tdee gasto energético total diario (BMR x actividad)
 * @param weeksToGoal estimación de semanas para llegar al peso objetivo (null si no aplica)
 */
public record NutritionTargets(int bmr, int tdee, int dailyKcal, int proteinG, int carbsG, int fatG,
                               Double weeklyLossKg, Integer weeksToGoal) {
}
