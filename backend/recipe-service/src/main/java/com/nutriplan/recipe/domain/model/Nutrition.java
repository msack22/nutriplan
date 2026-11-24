package com.nutriplan.recipe.domain.model;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

/**
 * Value Object con los macronutrientes POR PORCIÓN.
 *
 * <p>Es un {@code record} (inmutable) mapeado como {@code @Embeddable}: Hibernate 6.2+ soporta
 * records como embeddables. Sus columnas viven en la tabla {@code recipe} (no hay tabla aparte).
 * Los valores son {@code Double} (nullable) porque muchas recetas no traen todos los datos.
 */
@Embeddable
public record Nutrition(
        @Column(name = "kcal") Double kcal,
        @Column(name = "protein_g") Double proteinG,
        @Column(name = "carbs_g") Double carbsG,
        @Column(name = "fat_g") Double fatG) {

    public static final Nutrition EMPTY = new Nutrition(null, null, null, null);

    public boolean hasCalories() {
        return kcal != null && kcal > 0;
    }

    public boolean hasNoValues() {
        return kcal == null && proteinG == null && carbsG == null && fatG == null;
    }

    /** Multiplica por un factor (p.ej. cantidad de porciones). Los null se mantienen null. */
    public Nutrition times(double factor) {
        return new Nutrition(mul(kcal, factor), mul(proteinG, factor), mul(carbsG, factor), mul(fatG, factor));
    }

    private static Double mul(Double v, double f) {
        return v == null ? null : Math.round(v * f * 10) / 10.0;
    }
}
