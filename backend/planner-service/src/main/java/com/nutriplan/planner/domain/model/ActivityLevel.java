package com.nutriplan.planner.domain.model;

/** Factor de actividad física (multiplica la tasa metabólica basal para obtener el TDEE). */
public enum ActivityLevel {
    SEDENTARY(1.2), LIGHT(1.375), MODERATE(1.55), ACTIVE(1.725), VERY_ACTIVE(1.9);

    private final double factor;

    ActivityLevel(double factor) {
        this.factor = factor;
    }

    public double factor() {
        return factor;
    }
}
