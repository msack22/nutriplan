package com.nutriplan.pricing.domain.model;

/** Tamaño del envase normalizado: g, ml o unit. */
public record PackageSize(double amount, String unit) {
}
