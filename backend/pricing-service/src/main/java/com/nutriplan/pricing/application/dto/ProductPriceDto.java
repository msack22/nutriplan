package com.nutriplan.pricing.application.dto;

import java.time.Instant;

public record ProductPriceDto(String chain, String ingredient, String productName, String brand, double price,
                              String productUrl, Instant fetchedAt) {
}
