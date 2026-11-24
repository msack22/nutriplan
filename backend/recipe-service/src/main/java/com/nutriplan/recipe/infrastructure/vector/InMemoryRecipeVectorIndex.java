package com.nutriplan.recipe.infrastructure.vector;

import com.nutriplan.recipe.domain.port.RecipeVectorIndex;
import com.nutriplan.recipe.domain.service.VectorMath;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.LongPredicate;
import org.springframework.stereotype.Component;

/**
 * Vector store en memoria: búsqueda exacta (fuerza bruta) por similitud coseno.
 *
 * <p>Complejidad O(n·d) por consulta: con 133 recetas x 768 dimensiones son ~100K
 * multiplicaciones => microsegundos. Para millones de vectores se usaría un índice ANN
 * (HNSW) en una base vectorial. La fuente de verdad es MySQL (tabla recipe_embedding);
 * este índice se reconstruye al iniciar (ver RecipeIndexingService).
 *
 * <p>Thread-safe gracias a {@link ConcurrentHashMap}.
 */
@Component
public class InMemoryRecipeVectorIndex implements RecipeVectorIndex {

    private final Map<Long, float[]> vectors = new ConcurrentHashMap<>();

    @Override
    public void upsert(long recipeId, float[] vector) {
        vectors.put(recipeId, VectorMath.normalize(vector));
    }

    @Override
    public void remove(long recipeId) {
        vectors.remove(recipeId);
    }

    @Override
    public List<ScoredId> search(float[] query, int topK, LongPredicate filter) {
        float[] q = VectorMath.normalize(query);
        return vectors.entrySet().stream()
                .filter(e -> filter.test(e.getKey()))
                .filter(e -> e.getValue().length == q.length)
                .map(e -> new ScoredId(e.getKey(), VectorMath.cosine(q, e.getValue())))
                .sorted(Comparator.comparingDouble(ScoredId::score).reversed())
                .limit(topK)
                .toList();
    }

    @Override
    public int size() {
        return vectors.size();
    }
}
