package com.nutriplan.pricing.application.dto;

import java.time.Instant;
import java.util.List;

/** Costo estimado de la lista por cadena. Mismo contrato JSON que espera planner-service. */
public record PriceEstimate(List<ChainTotal> chains, List<ItemPrice> items, Instant pricesUpdatedAt) {

    public record ChainTotal(String chain, double total, int itemsPriced, int itemsMissing) {
    }

    public record ItemPrice(String ingredient, String chain, String productName, double unitPrice, int packages,
                            double subtotal, String productUrl) {
    }
}
