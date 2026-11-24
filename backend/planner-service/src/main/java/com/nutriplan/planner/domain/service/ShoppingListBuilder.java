package com.nutriplan.planner.domain.service;

import com.nutriplan.planner.domain.port.RecipeCatalogPort.IngredientLine;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;

/**
 * Arma la lista de compras: suma las cantidades del mismo ingrediente de distintas recetas.
 *
 * <p>Para poder sumar "1 taza de leche" + "200 ml de leche" convierte a una unidad base por
 * familia: masa -> g, volumen -> ml, unidades contables -> se suman por tipo (unidad, lata...).
 * Patrón <b>Builder</b>: se van agregando recetas con {@link #add} y al final {@link #build}.
 */
public final class ShoppingListBuilder {

    public record ShoppingItem(String name, Aisle aisle, String aisleLabel, Double quantity, String unit,
                               boolean toTaste, List<String> recipes) {
    }

    private record Key(String name, String unit) {
    }

    private static final class Acc {
        double quantity;
        boolean toTaste;
        final TreeSet<String> recipes = new TreeSet<>();
    }

    private static final Map<String, Double> MASS = Map.of("G", 1.0, "KG", 1000.0);
    private static final Map<String, Double> VOLUME = Map.of("ML", 1.0, "L", 1000.0, "CUP", 240.0, "TBSP", 15.0,
            "TSP", 5.0);

    private final Map<Key, Acc> items = new LinkedHashMap<>();

    /** @param factor porciones a cocinar / porciones que rinde la receta */
    public ShoppingListBuilder add(String recipeTitle, List<IngredientLine> ingredients, double factor) {
        for (IngredientLine i : ingredients) {
            if (i.optional() || i.name() == null || i.name().isBlank()) {
                continue;
            }
            String unit = i.unit() == null ? "TO_TASTE" : i.unit();
            double qty = i.quantity() == null ? 0 : i.quantity() * factor;
            String baseUnit;
            if (MASS.containsKey(unit)) {
                baseUnit = "g";
                qty *= MASS.get(unit);
            } else if (VOLUME.containsKey(unit)) {
                baseUnit = "ml";
                qty *= VOLUME.get(unit);
            } else if (unit.equals("PINCH") || unit.equals("TO_TASTE") || i.quantity() == null) {
                baseUnit = "a gusto";
            } else {
                baseUnit = unit.toLowerCase();   // unit, slice, can, clove
            }
            Acc acc = items.computeIfAbsent(new Key(i.name(), baseUnit), k -> new Acc());
            acc.quantity += qty;
            acc.toTaste |= baseUnit.equals("a gusto");
            acc.recipes.add(recipeTitle);
        }
        return this;
    }

    public List<ShoppingItem> build() {
        return items.entrySet().stream()
                .map(e -> {
                    Aisle aisle = Aisle.classify(e.getKey().name());
                    Acc a = e.getValue();
                    Double q = a.toTaste ? null : roundUp(e.getKey().unit(), a.quantity);
                    return new ShoppingItem(e.getKey().name(), aisle, aisle.label(), q, e.getKey().unit(), a.toTaste,
                            List.copyOf(a.recipes));
                })
                .sorted(Comparator.comparing(ShoppingItem::aisle).thenComparing(ShoppingItem::name))
                .toList();
    }

    /** Redondeo "de supermercado": unidades hacia arriba (no se compra 1,3 huevos). */
    private static double roundUp(String unit, double q) {
        if (unit.equals("g") || unit.equals("ml")) {
            return Math.ceil(q / 5) * 5;
        }
        return Math.ceil(q * 2) / 2;
    }
}
