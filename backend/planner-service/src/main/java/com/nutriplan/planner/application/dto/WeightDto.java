package com.nutriplan.planner.application.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;

public record WeightDto(@PastOrPresent LocalDate date, @DecimalMin("30") @DecimalMax("350") double weightKg,
                        @Size(max = 255) String note) {
}
