package com.nutriplan.recipe.domain.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.MapsId;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import java.time.Instant;

/**
 * Vector de embedding de una receta (búsqueda semántica). Se guarda en una tabla separada
 * para no cargar ~3KB de floats cada vez que se lee una receta.
 *
 * <p>{@code @MapsId}: la PK de esta tabla ES la FK a recipe (relación 1-1 compartiendo id).
 * El {@code float[]} se guarda como BLOB gracias a {@code FloatArrayConverter} (autoApply = true,
 * por eso el dominio no necesita importar la infraestructura).
 */
@Entity
@Table(name = "recipe_embedding")
public class RecipeEmbedding {

    @Id
    @Column(name = "recipe_id")
    private Long recipeId;

    @MapsId
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "recipe_id")
    private Recipe recipe;

    @Column(nullable = false, length = 80)
    private String model;

    @Column(nullable = false)
    private int dimensions;

    @Column(name = "vector", nullable = false, columnDefinition = "MEDIUMBLOB")
    private float[] vector;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected RecipeEmbedding() {
    }

    public RecipeEmbedding(Recipe recipe, String model, float[] vector) {
        this.recipe = recipe;
        update(model, vector);
    }

    public void update(String model, float[] vector) {
        this.model = model;
        this.vector = vector.clone();
        this.dimensions = vector.length;
        this.updatedAt = Instant.now();
    }

    public Long getRecipeId() {
        return recipeId;
    }

    public String getModel() {
        return model;
    }

    public float[] getVector() {
        return vector.clone();
    }
}
