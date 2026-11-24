package com.nutriplan.planner.domain.model;

/**
 * Objetivo de peso => déficit calórico diario.
 * ~7700 kcal ≈ 1 kg de grasa, así que -500 kcal/día ≈ -0,45 kg/semana (ritmo saludable).
 */
public enum WeightGoal {
    LOSE_FAST(-750), LOSE(-500), LOSE_SLOW(-250), MAINTAIN(0);

    private final int dailyKcalDelta;

    WeightGoal(int dailyKcalDelta) {
        this.dailyKcalDelta = dailyKcalDelta;
    }

    public int dailyKcalDelta() {
        return dailyKcalDelta;
    }
}
