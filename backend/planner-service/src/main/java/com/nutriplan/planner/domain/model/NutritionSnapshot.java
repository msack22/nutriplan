package com.nutriplan.planner.domain.model;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

/**
 * Copia (snapshot) de los macros por porción en el momento de planificar.
 * Patrón de microservicios: guardamos los datos que necesitamos de otro servicio en vez de
 * consultarlo siempre => las estadísticas funcionan aunque recipe-service esté caído.
 */
@Embeddable
public record NutritionSnapshot(
        @Column(name = "kcal") Double kcal,
        @Column(name = "protein_g") Double proteinG,
        @Column(name = "carbs_g") Double carbsG,
        @Column(name = "fat_g") Double fatG) {

    public static final NutritionSnapshot ZERO = new NutritionSnapshot(0.0, 0.0, 0.0, 0.0);

    public boolean hasCalories() {
        return kcal != null && kcal > 0;
    }

    public NutritionSnapshot plus(NutritionSnapshot o) {
        if (o == null) {
            return this;
        }
        return new NutritionSnapshot(add(kcal, o.kcal), add(proteinG, o.proteinG), add(carbsG, o.carbsG),
                add(fatG, o.fatG));
    }

    public NutritionSnapshot divide(double d) {
        return new NutritionSnapshot(div(kcal, d), div(proteinG, d), div(carbsG, d), div(fatG, d));
    }

    private static Double add(Double a, Double b) {
        return (a == null ? 0 : a) + (b == null ? 0 : b);
    }

    private static Double div(Double a, double d) {
        return a == null || d == 0 ? 0.0 : Math.round(a / d * 10) / 10.0;
    }
}
