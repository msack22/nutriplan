package com.nutriplan.pricing.domain.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

/** Ingrediente cuyo precio se actualiza cada lunes (llega por Kafka desde recipe-service). */
@Entity
@Table(name = "tracked_ingredient")
public class TrackedIngredient {

    @Id
    @Column(length = 200)
    private String name;

    @Column(name = "added_at", nullable = false)
    private Instant addedAt;

    protected TrackedIngredient() {
    }

    public TrackedIngredient(String name) {
        this.name = name;
        this.addedAt = Instant.now();
    }

    public String getName() {
        return name;
    }
}
