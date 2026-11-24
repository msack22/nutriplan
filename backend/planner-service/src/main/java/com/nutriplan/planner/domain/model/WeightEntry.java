package com.nutriplan.planner.domain.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.LocalDate;

/** Registro de peso (uno por día). Permite graficar el progreso hacia el objetivo. */
@Entity
@Table(name = "weight_entry", uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "entry_date"}))
public class WeightEntry {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false, length = 64)
    private String userId;

    @Column(name = "entry_date", nullable = false)
    private LocalDate date;

    @Column(name = "weight_kg", nullable = false)
    private double weightKg;

    @Column(length = 255)
    private String note;

    protected WeightEntry() {
    }

    public WeightEntry(String userId, LocalDate date, double weightKg, String note) {
        this.userId = userId;
        this.date = date;
        update(weightKg, note);
    }

    public void update(double weightKg, String note) {
        this.weightKg = weightKg;
        this.note = note;
    }

    public Long getId() {
        return id;
    }

    public LocalDate getDate() {
        return date;
    }

    public double getWeightKg() {
        return weightKg;
    }

    public String getNote() {
        return note;
    }
}
