package com.nutriplan.pricing.application.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.List;

/** Ítems de la lista de compras a cotizar (cantidad en g, ml o unidades). */
public record EstimateRequest(@Size(max = 300) List<@Valid Item> items) {

    public record Item(@NotBlank @Size(max = 200) String name, Double quantity, String unit) {
    }
}
