package com.nutriplan.planner.domain.service;

import java.util.List;

/** Góndola / sección del súper: ordena la lista de compras como se recorre el supermercado. */
public enum Aisle {
    VERDULERIA("Verdulería", List.of("tomate", "palta", "lechuga", "cebolla", "ajo", "zanahoria", "zapallito",
            "espinaca", "brocoli", "brócoli", "coliflor", "pepino", "morron", "morrón", "pimiento", "limon", "limón",
            "manzana", "banana", "frutilla", "fruto rojo", "berenjena", "champiñon", "champiñón", "hongo", "kale",
            "apio", "rucula", "rúcula", "perejil", "cilantro", "albahaca", "papa", "batata", "calabaza", "zapallo",
            "repollo", "puerro", "jengibre", "naranja", "arandano", "arándano", "esparrago", "espárrago", "col ",
            "verdura", "fruta", "lima", "menta", "rabanito", "remolacha", "choclo", "chaucha")),
    CARNICERIA("Carnicería y pescadería", List.of("pollo", "carne", "cerdo", "pescado", "salmon", "salmón", "merluza",
            "langostino", "pechuga", "bife", "chuleta", "lomo", "nalga", "peceto", "camaron", "camarón")),
    LACTEOS("Lácteos, huevos y fiambres", List.of("huevo", "queso", "leche", "yogur", "manteca", "crema", "ricota",
            "jamon", "jamón", "pavita", "pastron", "pastrón", "tofu")),
    PANADERIA("Panadería", List.of("pan ", "pan integral", "tortilla", "wrap")),
    ESPECIAS("Especias y condimentos", List.of("sal", "pimienta", "comino", "curcuma", "cúrcuma", "oregano", "orégano",
            "pimenton", "pimentón", "canela", "edulcorante", "vainilla", "nuez moscada", "aji", "ají", "condimento",
            "mostaza", "vinagre", "salsa de soja")),
    ALMACEN("Almacén", List.of());

    private final String label;
    private final List<String> keywords;

    Aisle(String label, List<String> keywords) {
        this.label = label;
        this.keywords = keywords;
    }

    public String label() {
        return label;
    }

    public static Aisle classify(String ingredientName) {
        String n = " " + ingredientName.toLowerCase() + " ";
        // orden importa: "leche de coco" es almacén, "pan rallado" también
        if (n.contains("leche de coco") || n.contains("pan rallado") || n.contains("harina") || n.contains("lata")
                || n.contains("caldo") || n.contains("aceite") || n.contains("salsa de tomate") || n.contains("almendra")) {
            return ALMACEN;
        }
        for (Aisle aisle : values()) {
            if (aisle.keywords.stream().anyMatch(k -> n.contains(k.endsWith(" ") ? " " + k : k))) {
                return aisle;
            }
        }
        return ALMACEN;
    }
}
