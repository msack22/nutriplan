package com.nutriplan.recipe.domain.port;

import java.util.List;
import java.util.function.LongPredicate;

/**
 * Puerto: índice vectorial (vector store). Implementación actual: en memoria con similitud
 * coseno (alcanza y sobra para cientos/miles de recetas). Se podría reemplazar por pgvector,
 * Qdrant, OpenSearch o MySQL HeatWave sin tocar los casos de uso.
 */
public interface RecipeVectorIndex {

    void upsert(long recipeId, float[] vector);

    void remove(long recipeId);

    /** Top-K por similitud, filtrando por los ids que cumplan {@code filter}. */
    List<ScoredId> search(float[] query, int topK, LongPredicate filter);

    int size();

    record ScoredId(long recipeId, double score) {
    }
}
