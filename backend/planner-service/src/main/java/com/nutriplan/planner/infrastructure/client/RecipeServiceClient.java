package com.nutriplan.planner.infrastructure.client;

import com.nutriplan.common.error.ResourceNotFoundException;
import com.nutriplan.planner.domain.generation.RecipeCandidate;
import com.nutriplan.planner.domain.model.MealType;
import com.nutriplan.planner.domain.model.NutritionSnapshot;
import com.nutriplan.planner.domain.model.RecipeRef;
import com.nutriplan.planner.domain.port.RecipeCatalogPort;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpStatus;
import org.springframework.resilience.annotation.Retryable;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;

/**
 * Adapter HTTP hacia recipe-service usando {@link RestClient} (cliente síncrono moderno de Spring).
 *
 * <p><b>Token propagation</b>: el RestClient "recipeRestClient" tiene un interceptor que copia el
 * JWT del usuario actual al header Authorization (ver ClientsConfig). recipe-service ve al MISMO
 * usuario => autorización end-to-end.
 *
 * <p><b>Resiliencia</b>: {@code @Retryable} (nuevo en Spring Framework 7) reintenta ante fallos
 * transitorios con backoff exponencial. No reintentamos 4xx (error del cliente, no transitorio).
 */
@Component
public class RecipeServiceClient implements RecipeCatalogPort {

    private final RestClient http;

    public RecipeServiceClient(RestClient recipeRestClient) {
        this.http = recipeRestClient;
    }

    @Override
    @Retryable(maxRetries = 2, delay = 200, multiplier = 2, excludes = HttpClientErrorException.class)
    public RecipeRef getRecipe(long recipeId) {
        try {
            RecipeJson r = http.get().uri("/api/recipes/{id}", recipeId).retrieve().body(RecipeJson.class);
            return new RecipeRef(r.id(), r.title(), r.nutritionPerServing());
        } catch (HttpClientErrorException e) {
            if (e.getStatusCode() == HttpStatus.NOT_FOUND) {
                throw new ResourceNotFoundException("Receta", recipeId);
            }
            throw e;
        }
    }

    @Override
    @Retryable(maxRetries = 2, delay = 200, multiplier = 2, excludes = HttpClientErrorException.class)
    public List<RecipeIngredients> getIngredients(Collection<Long> recipeIds) {
        if (recipeIds.isEmpty()) {
            return List.of();
        }
        List<RecipeJson> body = http.post().uri("/api/recipes/batch").body(List.copyOf(recipeIds))
                .retrieve().body(new ParameterizedTypeReference<>() { });
        return body == null ? List.of() : body.stream()
                .map(r -> new RecipeIngredients(r.id(), r.title(), r.originalServings(), r.ingredients().stream()
                        .map(i -> new IngredientLine(i.name(), i.quantity(), i.unit(), i.optional())).toList()))
                .toList();
    }

    @Override
    @Retryable(maxRetries = 2, delay = 200, multiplier = 2, excludes = HttpClientErrorException.class)
    public List<RecipeCandidate> findCandidates(CandidateQuery q) {
        Map<String, Object> request = Map.of(
                "mealTypes", q.mealTypes(),
                "requiredTags", q.vegetarian() ? Set.of("VEGETARIAN") : Set.of(),
                "excludedIngredients", q.excludedIngredients(),
                "preference", q.preference() == null ? "" : q.preference(),
                "limit", 500);
        List<CandidateJson> body = http.post().uri("/api/recipes/candidates").body(request)
                .retrieve().body(new ParameterizedTypeReference<>() { });
        return body == null ? List.of() : body.stream()
                .map(c -> new RecipeCandidate(c.id(), c.title(), c.mealTypes(), c.nutritionPerServing(),
                        c.preferenceScore()))
                .toList();
    }

    // Solo los campos que nos interesan del JSON de recipe-service (los demás se ignoran).
    record RecipeJson(long id, String title, int originalServings, NutritionSnapshot nutritionPerServing,
                      List<IngredientJson> ingredients) {
    }

    record IngredientJson(String name, Double quantity, String unit, boolean optional) {
    }

    record CandidateJson(long id, String title, Set<MealType> mealTypes, NutritionSnapshot nutritionPerServing,
                         double preferenceScore) {
    }
}
