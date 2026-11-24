package com.nutriplan.common.events;

/**
 * Nombres de los tópicos de Kafka. Centralizados para que productor y consumidor
 * no diverjan. Convención: {@code <dominio>.<evento-en-pasado>}.
 */
public final class Topics {
    /** recipe-service -> pricing-service: cambió el catálogo de ingredientes. */
    public static final String INGREDIENTS_CHANGED = "nutriplan.recipes.ingredients-changed";
    /** pricing-service -> planner-service: terminó el job semanal de precios. */
    public static final String PRICES_REFRESHED = "nutriplan.pricing.prices-refreshed";
    /** planner-service -> (auditoría / analytics): un plan semanal cambió. */
    public static final String PLAN_CHANGED = "nutriplan.planner.plan-changed";

    private Topics() {
    }
}
