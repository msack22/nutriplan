package com.nutriplan.recipe.application.usecase;

import com.nutriplan.common.error.ResourceNotFoundException;
import com.nutriplan.recipe.application.dto.CandidateRequest;
import com.nutriplan.recipe.application.dto.RecipeCandidateDto;
import com.nutriplan.recipe.application.dto.ScoredRecipeDto;
import com.nutriplan.recipe.application.mapper.RecipeMapper;
import com.nutriplan.recipe.domain.model.MealType;
import com.nutriplan.recipe.domain.model.Recipe;
import com.nutriplan.recipe.domain.model.RecipeIngredient;
import com.nutriplan.recipe.domain.port.EmbeddingGenerator;
import com.nutriplan.recipe.domain.port.RecipeVectorIndex;
import com.nutriplan.recipe.domain.port.RecipeVectorIndex.ScoredId;
import com.nutriplan.recipe.domain.repository.RecipeEmbeddingRepository;
import com.nutriplan.recipe.domain.repository.RecipeRepository;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Búsqueda semántica: el texto del usuario se convierte en vector y se buscan las recetas con
 * vectores más "cercanos" (similitud coseno). Encuentra "algo liviano con pollo" aunque la receta
 * se llame "Ensalada griega con pechuga" (no hay coincidencia literal de palabras).
 */
@Service
@Transactional(readOnly = true)
public class SemanticSearchService {

    private final EmbeddingGenerator embeddings;
    private final RecipeVectorIndex index;
    private final RecipeRepository recipes;
    private final RecipeEmbeddingRepository embeddingRepository;
    private final RecipeMapper mapper;

    public SemanticSearchService(EmbeddingGenerator embeddings, RecipeVectorIndex index,
                                 RecipeRepository recipes, RecipeEmbeddingRepository embeddingRepository,
                                 RecipeMapper mapper) {
        this.embeddings = embeddings;
        this.index = index;
        this.recipes = recipes;
        this.embeddingRepository = embeddingRepository;
        this.mapper = mapper;
    }

    public List<ScoredRecipeDto> search(String query, MealType mealType, int limit, String userId) {
        Map<Long, Recipe> catalog = visibleCatalog(userId);
        List<ScoredId> hits = index.search(embeddings.embedQuery(query), limit, id -> {
            Recipe r = catalog.get(id);
            return r != null && (mealType == null || r.isSuitableFor(mealType));
        });
        return hits.stream()
                .map(h -> new ScoredRecipeDto(mapper.toSummary(catalog.get(h.recipeId())), round(h.score())))
                .toList();
    }

    public List<ScoredRecipeDto> similarTo(long recipeId, int limit, String userId) {
        Map<Long, Recipe> catalog = visibleCatalog(userId);
        if (!catalog.containsKey(recipeId)) {
            throw new ResourceNotFoundException("Receta", recipeId);
        }
        float[] vector = embeddingRepository.findById(recipeId)
                .orElseThrow(() -> new ResourceNotFoundException("Embedding de receta", recipeId))
                .getVector();
        return index.search(vector, limit, id -> id != recipeId && catalog.containsKey(id)).stream()
                .map(h -> new ScoredRecipeDto(mapper.toSummary(catalog.get(h.recipeId())), round(h.score())))
                .toList();
    }

    /**
     * Candidatas para el generador de planes: filtra por momento del día, tags e ingredientes
     * excluidos; si hay "preferencia" ordena por similitud semántica.
     */
    public List<RecipeCandidateDto> candidates(CandidateRequest req, String userId) {
        List<String> excluded = req.excludedIngredients().stream()
                .map(s -> s.toLowerCase(Locale.ROOT).strip()).filter(s -> !s.isEmpty()).toList();
        Map<Long, Recipe> filtered = recipes.findVisibleTo(userId).stream()
                .filter(r -> req.mealTypes().isEmpty() || req.mealTypes().stream().anyMatch(r::isSuitableFor))
                .filter(r -> r.getTags().containsAll(req.requiredTags()))
                .filter(r -> excluded.isEmpty() || r.getIngredients().stream()
                        .map(RecipeIngredient::getName)
                        .noneMatch(name -> excluded.stream().anyMatch(name::contains)))
                .collect(Collectors.toMap(Recipe::getId, Function.identity()));

        Map<Long, Double> scores = req.preference() == null || req.preference().isBlank()
                ? Map.of()
                : index.search(embeddings.embedQuery(req.preference()), filtered.size(), filtered::containsKey)
                        .stream().collect(Collectors.toMap(ScoredId::recipeId, ScoredId::score));

        return filtered.values().stream()
                .map(r -> mapper.toCandidate(r, round(scores.getOrDefault(r.getId(), 0.0))))
                .sorted(Comparator.comparingDouble(RecipeCandidateDto::preferenceScore).reversed())
                .limit(req.limit())
                .toList();
    }

    /** Catálogo + recetas propias del usuario: las privadas de otros nunca aparecen en búsquedas. */
    private Map<Long, Recipe> visibleCatalog(String userId) {
        return recipes.findVisibleTo(userId).stream().collect(Collectors.toMap(Recipe::getId, Function.identity()));
    }

    private static double round(double v) {
        return Math.round(v * 1000) / 1000.0;
    }
}
