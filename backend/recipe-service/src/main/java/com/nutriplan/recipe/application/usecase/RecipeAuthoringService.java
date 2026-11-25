package com.nutriplan.recipe.application.usecase;

import com.nutriplan.common.error.ResourceNotFoundException;
import com.nutriplan.common.events.IngredientsChangedEvent;
import com.nutriplan.common.events.Topics;
import com.nutriplan.recipe.application.dto.RecipeCommand;
import com.nutriplan.recipe.application.dto.RecipeDetailDto;
import com.nutriplan.recipe.domain.model.Category;
import com.nutriplan.recipe.domain.model.Nutrition;
import com.nutriplan.recipe.domain.model.NutritionSource;
import com.nutriplan.recipe.domain.model.Recipe;
import com.nutriplan.recipe.domain.port.IntegrationEventPublisher;
import com.nutriplan.recipe.domain.repository.CategoryRepository;
import com.nutriplan.recipe.domain.repository.RecipeRepository;
import com.nutriplan.recipe.domain.service.IngredientLineParser;
import com.nutriplan.recipe.domain.service.ParsedIngredient;
import com.nutriplan.recipe.domain.service.RecipeClassifier;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Casos de uso para que cada usuario cree, edite y borre SUS recetas.
 *
 * <p>Flujo: (1) transacción corta que guarda la receta + evento outbox; (2) FUERA de la
 * transacción se genera el embedding (llama a Ollama: no queremos una conexión de DB abierta
 * mientras tanto); (3) si no cargó macros, se estiman con el LLM en segundo plano.
 */
@Service
public class RecipeAuthoringService {

    private static final Logger log = LoggerFactory.getLogger(RecipeAuthoringService.class);

    private final RecipeRepository recipes;
    private final CategoryRepository categories;
    private final IntegrationEventPublisher events;
    private final RecipeIndexingService indexing;
    private final NutritionEstimationService nutrition;
    private final RecipeQueryService queries;
    private final TransactionTemplate tx;

    public RecipeAuthoringService(RecipeRepository recipes, CategoryRepository categories,
                                  IntegrationEventPublisher events, RecipeIndexingService indexing,
                                  NutritionEstimationService nutrition, RecipeQueryService queries,
                                  TransactionTemplate tx) {
        this.recipes = recipes;
        this.categories = categories;
        this.events = events;
        this.indexing = indexing;
        this.nutrition = nutrition;
        this.queries = queries;
        this.tx = tx;
    }

    public RecipeDetailDto create(String userId, RecipeCommand cmd) {
        Long id = tx.execute(s -> {
            Recipe recipe = Recipe.ownedBy(userId, cmd.title(), category(cmd.categorySlug()));
            apply(recipe, cmd);
            return recipes.saveAndFlush(recipe).getId();
        });
        afterSave(id, cmd);
        return queries.getDetail(id, null, userId);
    }

    public RecipeDetailDto update(String userId, long id, RecipeCommand cmd) {
        tx.executeWithoutResult(s -> {
            Recipe recipe = owned(userId, id);
            recipe.rename(cmd.title());
            recipe.changeCategory(category(cmd.categorySlug()));
            apply(recipe, cmd);
        });
        afterSave(id, cmd);
        return queries.getDetail(id, null, userId);
    }

    /** Borrado lógico: si estaba en un plan, el planner conserva su snapshot. */
    public void delete(String userId, long id) {
        tx.executeWithoutResult(s -> owned(userId, id).deactivate());
        indexing.unindex(id);
    }

    // ----------------------------------------------------------------- helpers

    private void apply(Recipe recipe, RecipeCommand cmd) {
        List<ParsedIngredient> parsed = cmd.ingredients().stream().map(IngredientLineParser::parse).toList();
        recipe.defineYield(cmd.servings(), "Rinde " + cmd.servings() + (cmd.servings() == 1 ? " porción" : " porciones"));
        recipe.replaceIngredients(parsed.stream().map(ParsedIngredient::toEntity).toList());
        recipe.replaceSteps(cmd.steps().stream().map(String::strip).filter(st -> !st.isEmpty()).toList());
        recipe.describe(cmd.notes(), null);
        Nutrition n = cmd.nutritionPerServing();
        if (n != null && !n.hasNoValues()) {
            recipe.updateNutrition(n, NutritionSource.MANUAL);
        } else if (recipe.getNutritionSource() == NutritionSource.MANUAL || recipe.getId() == null) {
            recipe.updateNutrition(Nutrition.EMPTY, NutritionSource.UNKNOWN);
        }
        var mealTypes = cmd.mealTypes().isEmpty()
                ? RecipeClassifier.mealTypesFor(cmd.categorySlug(), null) : cmd.mealTypes();
        recipe.classify(mealTypes, RecipeClassifier.tagsFor(cmd.categorySlug(),
                parsed.stream().map(ParsedIngredient::name).toList(), recipe.getNutrition()));
        // pricing-service se entera de los ingredientes nuevos (outbox => misma transacción)
        events.publish(Topics.INGREDIENTS_CHANGED, "user-recipe", new IngredientsChangedEvent(
                UUID.randomUUID().toString(), Instant.now(), parsed.stream().map(ParsedIngredient::name).toList()));
    }

    private void afterSave(long id, RecipeCommand cmd) {
        try {
            indexing.indexOne(id);
        } catch (RuntimeException e) {
            log.warn("No se pudo generar el embedding de la receta {} (¿Ollama apagado?): {}", id, e.getMessage());
        }
        if (cmd.nutritionPerServing() == null || cmd.nutritionPerServing().hasNoValues()) {
            nutrition.estimateOneAsync(id);
        }
    }

    private Category category(String slug) {
        return categories.findBySlug(slug).orElseThrow(() -> new IllegalArgumentException("Categoría inexistente: " + slug));
    }

    /** Anti-IDOR: solo el dueño puede editar/borrar; para el resto la receta "no existe". */
    private Recipe owned(String userId, long id) {
        return recipes.findWithIngredientsById(id)
                .filter(r -> r.isOwnedBy(userId) && r.isActive())
                .orElseThrow(() -> new ResourceNotFoundException("Receta", id));
    }
}
