package com.nutriplan.recipe.domain.model;

/** De dónde sale la información nutricional: importante para la honestidad de las estadísticas. */
public enum NutritionSource {
    /** Venía en la tabla nutricional del recetario. */
    DECLARED,
    /** Estimada por un LLM local (Ollama) a partir de los ingredientes. */
    ESTIMATED,
    /** Cargada/corregida manualmente por un admin. */
    MANUAL,
    /** Sin datos todavía. */
    UNKNOWN
}
