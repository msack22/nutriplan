package com.nutriplan.recipe.application.usecase;

import com.nutriplan.recipe.domain.model.Recipe;
import com.nutriplan.recipe.domain.model.RecipeEmbedding;
import com.nutriplan.recipe.domain.port.EmbeddingGenerator;
import com.nutriplan.recipe.domain.port.RecipeVectorIndex;
import com.nutriplan.recipe.domain.repository.RecipeEmbeddingRepository;
import com.nutriplan.recipe.domain.repository.RecipeRepository;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Mantiene sincronizados: recetas (MySQL) -> embeddings (MySQL) -> índice vectorial (memoria).
 * Solo genera embeddings que faltan o cuyo modelo cambió (los vectores de modelos distintos
 * no son comparables entre sí).
 */
@Service
public class RecipeIndexingService {

    private static final Logger log = LoggerFactory.getLogger(RecipeIndexingService.class);

    private final RecipeRepository recipes;
    private final RecipeEmbeddingRepository embeddingRepository;
    private final EmbeddingGenerator generator;
    private final RecipeVectorIndex index;

    public RecipeIndexingService(RecipeRepository recipes, RecipeEmbeddingRepository embeddingRepository,
                                 EmbeddingGenerator generator, RecipeVectorIndex index) {
        this.recipes = recipes;
        this.embeddingRepository = embeddingRepository;
        this.generator = generator;
        this.index = index;
    }

    /** @param force si es true, regenera todos los embeddings aunque existan. */
    @Transactional
    public int reindex(boolean force) {
        Map<Long, RecipeEmbedding> existing = embeddingRepository.findAll().stream()
                .collect(Collectors.toMap(RecipeEmbedding::getRecipeId, Function.identity()));
        int generated = 0;
        for (Recipe recipe : recipes.findByActiveTrue()) {
            RecipeEmbedding embedding = existing.get(recipe.getId());
            boolean upToDate = embedding != null && embedding.getModel().equals(generator.modelName());
            if (!upToDate || force) {
                float[] vector = generator.embedDocument(recipe.toEmbeddingText());
                if (embedding == null) {
                    embedding = embeddingRepository.save(new RecipeEmbedding(recipe, generator.modelName(), vector));
                } else {
                    embedding.update(generator.modelName(), vector);
                }
                generated++;
            }
            index.upsert(recipe.getId(), embedding.getVector());
        }
        log.info("Índice vectorial listo: {} recetas ({} embeddings nuevos, modelo {})",
                index.size(), generated, generator.modelName());
        return generated;
    }
}
