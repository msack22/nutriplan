package com.nutriplan.pricing.application.dto;

import java.time.Duration;
import java.time.Instant;

public record RefreshReport(String eventId, Instant finishedAt, int ingredients, int pricesUpdated, int notFound,
                            int errors, Duration duration) {
}
