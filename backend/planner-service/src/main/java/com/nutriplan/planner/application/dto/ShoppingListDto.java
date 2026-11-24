package com.nutriplan.planner.application.dto;

import com.nutriplan.planner.domain.port.PricingPort.PriceEstimate;
import com.nutriplan.planner.domain.service.ShoppingListBuilder.ShoppingItem;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

/** Lista de compras de la semana + estimación de precio por supermercado (si está disponible). */
public record ShoppingListDto(long planId, LocalDate weekStart, int people, List<ShoppingItem> items,
                              PriceEstimate prices, Instant generatedAt) {
}
