package com.nutriplan.planner.domain.model;

/**
 * Momento del día. En DDD cada bounded context define su propio modelo: no compartimos esta
 * clase con recipe-service (acoplaría los despliegues). Coinciden por nombre en el JSON.
 */
public enum MealType {
    BREAKFAST(0.25), LUNCH(0.35), SNACK(0.10), DINNER(0.30);

    /** Porción típica de las calorías del día para esta comida (se re-normaliza). */
    private final double calorieShare;

    MealType(double calorieShare) {
        this.calorieShare = calorieShare;
    }

    public double calorieShare() {
        return calorieShare;
    }
}
