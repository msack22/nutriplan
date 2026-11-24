package com.nutriplan.planner.domain.port;

import com.nutriplan.planner.domain.service.ShoppingListBuilder.ShoppingItem;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

/** Puerto hacia pricing-service: estima el costo de la lista de compras por supermercado. */
public interface PricingPort {

    Optional<PriceEstimate> estimate(List<ShoppingItem> items);

    record PriceEstimate(List<ChainTotal> chains, List<ItemPrice> items, Instant pricesUpdatedAt) {
    }

    record ChainTotal(String chain, double total, int itemsPriced, int itemsMissing) {
    }

    record ItemPrice(String ingredient, String chain, String productName, double unitPrice, int packages,
                     double subtotal, String productUrl) {
    }
}
