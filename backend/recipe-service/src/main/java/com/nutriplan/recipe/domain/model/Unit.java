package com.nutriplan.recipe.domain.model;

/**
 * Unidades de medida normalizadas. Permiten sumar cantidades en la lista de compras.
 * {@link #baseFactor} convierte a la unidad base de su familia (g para masa, ml para volumen).
 */
public enum Unit {
    G(Family.MASS, 1), KG(Family.MASS, 1000),
    ML(Family.VOLUME, 1), L(Family.VOLUME, 1000),
    CUP(Family.VOLUME, 240), TBSP(Family.VOLUME, 15), TSP(Family.VOLUME, 5),
    UNIT(Family.COUNT, 1), SLICE(Family.COUNT, 1), CAN(Family.COUNT, 1), CLOVE(Family.COUNT, 1),
    PINCH(Family.OTHER, 1), TO_TASTE(Family.OTHER, 1);

    public enum Family { MASS, VOLUME, COUNT, OTHER }

    private final Family family;
    private final double baseFactor;

    Unit(Family family, double baseFactor) {
        this.family = family;
        this.baseFactor = baseFactor;
    }

    public Family family() {
        return family;
    }

    public double baseFactor() {
        return baseFactor;
    }
}
