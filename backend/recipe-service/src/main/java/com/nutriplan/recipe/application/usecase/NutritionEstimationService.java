package com.nutriplan.recipe.application.usecase;

import com.nutriplan.common.error.ResourceNotFoundException;
import com.nutriplan.recipe.domain.model.Nutrition;
import com.nutriplan.recipe.domain.model.NutritionSource;
import com.nutriplan.recipe.domain.model.Recipe;
import com.nutriplan.recipe.domain.model.RecipeIngredient;
import com.nutriplan.recipe.domain.port.NutritionEstimator;
import com.nutriplan.recipe.domain.repository.RecipeRepository;
import com.nutriplan.recipe.domain.service.RecipeClassifier;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Completa la nutrición faltante usando el LLM. Es LENTO (segundos por receta), por eso:
 * <ul>
 *   <li>{@code @Async}: corre en otro hilo; el endpoint responde 202 Accepted de inmediato.</li>
 *   <li>Una transacción CORTA por receta ({@link TransactionTemplate}): no mantenemos una
 *       conexión de DB abierta mientras esperamos al LLM.</li>
 *   <li>{@link AtomicBoolean}: evita dos ejecuciones simultáneas.</li>
 * </ul>
 */
@Service
public class NutritionEstimationService {

    private static final Logger log = LoggerFactory.getLogger(NutritionEstimationService.class);

    private final RecipeRepository recipes;
    private final NutritionEstimator estimator;
    private final TransactionTemplate tx;
    private final AiWorkload workload;
    private final AtomicBoolean running = new AtomicBoolean(false);

    public NutritionEstimationService(RecipeRepository recipes, NutritionEstimator estimator, TransactionTemplate tx,
                                      AiWorkload workload) {
        this.recipes = recipes;
        this.estimator = estimator;
        this.tx = tx;
        this.workload = workload;
    }

    public boolean isRunning() {
        return running.get();
    }

    @Async
    public void estimateMissingAsync() {
        if (!running.compareAndSet(false, true)) {
            log.info("La estimación ya está en curso");
            return;
        }
        try {
            List<Long> ids = recipes.findIdsWithoutNutrition();
            log.info("Estimando nutrición de {} recetas con LLM...", ids.size());
            int ok = 0;
            for (Long id : ids) {
                workload.yieldToInteractive();   // si un usuario está generando un borrador, esperamos
                record Input(String title, int servings, List<String> lines) { }
                Input input = tx.execute(s -> {
                    Recipe r = recipes.findWithIngredientsById(id).orElseThrow();
                    return new Input(r.getTitle(), r.getServings(),
                            r.getIngredients().stream().map(RecipeIngredient::getRawText).toList());
                });
                var estimate = estimator.estimatePerServing(input.title(), input.servings(), input.lines());
                if (estimate.isPresent()) {
                    tx.executeWithoutResult(s -> apply(id, estimate.get(), NutritionSource.ESTIMATED));
                    ok++;
                }
            }
            log.info("Estimación terminada: {}/{} recetas completadas", ok, ids.size());
        } finally {
            running.set(false);
        }
    }

    /** Estima UNA receta (recién creada por un usuario) en segundo plano. */
    @Async
    public void estimateOneAsync(long id) {
        workload.yieldToInteractive();
        record Input(String title, int servings, List<String> lines) { }
        Input input = tx.execute(s -> recipes.findWithIngredientsById(id)
                .map(r -> new Input(r.getTitle(), r.getServings(),
                        r.getIngredients().stream().map(RecipeIngredient::getRawText).toList()))
                .orElse(null));
        if (input == null) {
            return;
        }
        estimator.estimatePerServing(input.title(), input.servings(), input.lines())
                .ifPresent(n -> tx.executeWithoutResult(s -> apply(id, n, NutritionSource.ESTIMATED)));
    }

    /** Corrección manual de un admin. */
    public void setManual(long recipeId, Nutrition nutrition) {
        tx.executeWithoutResult(s -> apply(recipeId, nutrition, NutritionSource.MANUAL));
    }

    private void apply(long id, Nutrition nutrition, NutritionSource source) {
        Recipe r = recipes.findWithIngredientsById(id).orElseThrow(() -> new ResourceNotFoundException("Receta", id));
        r.updateNutrition(nutrition, source);
        r.classify(r.getMealTypes(), RecipeClassifier.tagsFor(r.getCategory().getSlug(),
                r.getIngredients().stream().map(RecipeIngredient::getName).toList(), nutrition));
    }
}
