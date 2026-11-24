package com.nutriplan.recipe.application.usecase;

import com.nutriplan.common.events.IngredientsChangedEvent;
import com.nutriplan.common.events.Topics;
import com.nutriplan.recipe.application.dto.ImportReport;
import com.nutriplan.recipe.application.dto.ImportedRecipe;
import com.nutriplan.recipe.domain.model.Category;
import com.nutriplan.recipe.domain.model.Nutrition;
import com.nutriplan.recipe.domain.model.NutritionSource;
import com.nutriplan.recipe.domain.model.Recipe;
import com.nutriplan.recipe.domain.model.RecipeIngredient;
import com.nutriplan.recipe.domain.port.IntegrationEventPublisher;
import com.nutriplan.recipe.domain.repository.CategoryRepository;
import com.nutriplan.recipe.domain.repository.RecipeRepository;
import com.nutriplan.recipe.domain.service.IngredientLineParser;
import com.nutriplan.recipe.domain.service.ParsedIngredient;
import com.nutriplan.recipe.domain.service.RecipeClassifier;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Caso de uso: importar recetas desde una fuente externa (idempotente: upsert por externalId).
 *
 * <p>Todo ocurre en UNA transacción: categorías, recetas, ingredientes y el evento de outbox.
 * Si algo falla, rollback completo (atomicidad - la "A" de ACID).
 * Si se vuelve a importar el mismo archivo, se actualiza en lugar de duplicar.
 */
@Service
public class RecipeImportService {

    private static final Logger log = LoggerFactory.getLogger(RecipeImportService.class);

    private final RecipeRepository recipes;
    private final CategoryRepository categories;
    private final IntegrationEventPublisher events;

    public RecipeImportService(RecipeRepository recipes, CategoryRepository categories,
                               IntegrationEventPublisher events) {
        this.recipes = recipes;
        this.categories = categories;
        this.events = events;
    }

    @Transactional
    public ImportReport importRecipes(List<ImportedRecipe> imported) {
        Map<String, Category> categoryCache = new HashMap<>();
        int created = 0;
        int updated = 0;
        int withNutrition = 0;

        for (ImportedRecipe in : imported) {
            Category category = categoryCache.computeIfAbsent(in.categorySlug(), slug ->
                    categories.findBySlug(slug).orElseGet(() -> categories.save(
                            new Category(slug, in.categoryName(), in.categoryEmoji(), in.categoryOrder()))));

            Recipe recipe = recipes.findByExternalId(in.externalId()).orElse(null);
            if (recipe == null) {
                recipe = new Recipe(in.externalId(), in.title(), category);
                created++;
            } else {
                recipe.rename(in.title());
                recipe.changeCategory(category);
                updated++;
            }

            List<ParsedIngredient> parsed = in.ingredientLines().stream().map(IngredientLineParser::parse).toList();
            List<RecipeIngredient> ingredients = parsed.stream().map(ParsedIngredient::toEntity).toList();
            Nutrition nutrition = in.nutritionPerServing() == null ? Nutrition.EMPTY : in.nutritionPerServing();

            recipe.defineYield(Math.max(1, in.servings()), in.yieldLabel());
            recipe.replaceIngredients(ingredients);
            recipe.replaceSteps(in.steps());
            recipe.describe(in.notes(), in.videoUrl());
            // No pisamos una nutrición estimada/manual con "vacío" al reimportar
            if (!nutrition.hasNoValues() || recipe.getNutritionSource() == NutritionSource.UNKNOWN) {
                recipe.updateNutrition(nutrition, NutritionSource.DECLARED);
            }
            if (recipe.getNutrition().hasCalories()) {
                withNutrition++;
            }
            recipe.classify(
                    RecipeClassifier.mealTypesFor(in.categorySlug(), in.subcategory()),
                    RecipeClassifier.tagsFor(in.categorySlug(), parsed.stream().map(ParsedIngredient::name).toList(),
                            recipe.getNutrition()));
            recipes.save(recipe);
        }
        recipes.flush();

        // Evento de integración (se guarda en la tabla outbox dentro de ESTA transacción)
        List<String> names = recipes.findDistinctIngredientNames();
        events.publish(Topics.INGREDIENTS_CHANGED, "catalog",
                new IngredientsChangedEvent(UUID.randomUUID().toString(), Instant.now(), names));

        ImportReport report = new ImportReport(created, updated, categoryCache.size(), withNutrition);
        log.info("Importación terminada: {}", report);
        return report;
    }
}
