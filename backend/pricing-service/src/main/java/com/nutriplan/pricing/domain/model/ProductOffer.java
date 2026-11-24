package com.nutriplan.pricing.domain.model;

/** Producto devuelto por la búsqueda en un supermercado (modelo neutral, no atado a VTEX). */
public record ProductOffer(String chain, String name, String brand, double price, boolean available, String url,
                           String measurementUnit, double unitMultiplier) {
}
