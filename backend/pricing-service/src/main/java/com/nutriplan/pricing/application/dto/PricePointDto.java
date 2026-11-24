package com.nutriplan.pricing.application.dto;

import java.time.LocalDate;

public record PricePointDto(String chain, LocalDate date, double price, String productName) {
}
