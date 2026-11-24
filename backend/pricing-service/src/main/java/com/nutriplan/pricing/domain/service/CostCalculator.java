package com.nutriplan.pricing.domain.service;

import com.nutriplan.pricing.domain.model.PackageSize;

/**
 * ¿Cuántos envases hay que comprar? Si las unidades son compatibles (g con g, ml con ml,
 * unidades con unidades) se redondea hacia arriba; si no se puede saber, 1 envase.
 * Ej: necesito 680 ml de leche y el envase es de 1000 ml => 1 envase.
 */
public final class CostCalculator {

    private CostCalculator() {
    }

    public static int packagesNeeded(Double quantity, String unit, PackageSize pkg) {
        if (quantity == null || quantity <= 0 || pkg == null || pkg.amount() <= 0) {
            return 1;
        }
        String u = normalizeUnit(unit);
        if (!u.equals(pkg.unit())) {
            return 1;
        }
        return (int) Math.max(1, Math.ceil(quantity / pkg.amount() - 0.05)); // 5% de tolerancia
    }

    static String normalizeUnit(String unit) {
        if (unit == null) {
            return "unit";
        }
        return switch (unit.toLowerCase()) {
            case "g" -> "g";
            case "ml" -> "ml";
            default -> "unit";
        };
    }
}
