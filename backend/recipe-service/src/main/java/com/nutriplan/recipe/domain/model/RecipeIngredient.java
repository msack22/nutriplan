package com.nutriplan.recipe.domain.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

/**
 * Línea de ingrediente de una receta. Guarda el texto ORIGINAL ({@code rawText}) y además la
 * interpretación estructurada (cantidad + unidad + nombre normalizado) usada para escalar
 * porciones y armar la lista de compras.
 *
 * <p>Lado "muchos" de la relación bidireccional Recipe 1..N RecipeIngredient: es el dueño de la
 * FK ({@code recipe_id}). {@code FetchType.LAZY} evita cargar la receta al leer un ingrediente.
 */
@Entity
@Table(name = "recipe_ingredient")
public class RecipeIngredient {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "recipe_id")
    private Recipe recipe;

    private int position;

    @Column(name = "raw_text", nullable = false, length = 500)
    private String rawText;

    @Column(nullable = false, length = 200)
    private String name;

    private Double quantity;

    @Enumerated(EnumType.STRING)   // STRING y no ORDINAL: reordenar el enum no corrompe datos
    @Column(length = 20)
    private Unit unit;

    @Column(name = "optional_item")
    private boolean optional;

    protected RecipeIngredient() {
    }

    public RecipeIngredient(String rawText, String name, Double quantity, Unit unit, boolean optional) {
        this.rawText = rawText;
        this.name = name;
        this.quantity = quantity;
        this.unit = unit;
        this.optional = optional;
    }

    void attachTo(Recipe recipe, int position) {
        this.recipe = recipe;
        this.position = position;
    }

    public Long getId() {
        return id;
    }

    public int getPosition() {
        return position;
    }

    public String getRawText() {
        return rawText;
    }

    public String getName() {
        return name;
    }

    public Double getQuantity() {
        return quantity;
    }

    public Unit getUnit() {
        return unit;
    }

    public boolean isOptional() {
        return optional;
    }
}
