package com.nutriplan.planner.application.usecase;

import com.nutriplan.planner.application.dto.ShoppingListDto;
import com.nutriplan.planner.domain.model.PlannedMeal;
import com.nutriplan.planner.domain.model.WeeklyPlan;
import com.nutriplan.planner.domain.port.PricingPort;
import com.nutriplan.planner.domain.port.RecipeCatalogPort;
import com.nutriplan.planner.domain.port.RecipeCatalogPort.RecipeIngredients;
import com.nutriplan.planner.domain.service.ShoppingListBuilder;
import com.nutriplan.planner.domain.service.ShoppingListBuilder.ShoppingItem;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Component;

/**
 * Cálculo (caro: 2 llamadas HTTP) de la lista de compras, cacheado en Redis.
 *
 * <p>Clave de caché = {@code planId:version}. Como cada cambio del plan incrementa la versión
 * (@Version), un plan modificado genera una clave nueva => no hace falta invalidar a mano.
 * Cuando cambian los precios (evento Kafka) se vacía toda la caché.
 *
 * <p>Está en un bean SEPARADO de {@link ShoppingListService} a propósito: {@code @Cacheable}
 * funciona con un proxy de Spring AOP; si un método de la misma clase lo llamara
 * ("self-invocation"), no pasaría por el proxy y la caché se ignoraría.
 */
@Component
public class ShoppingListCalculator {

    public static final String CACHE = "shopping-list";

    private final RecipeCatalogPort catalog;
    private final PricingPort pricing;
    private final Clock clock;

    public ShoppingListCalculator(RecipeCatalogPort catalog, PricingPort pricing, Clock clock) {
        this.catalog = catalog;
        this.pricing = pricing;
        this.clock = clock;
    }

    @Cacheable(cacheNames = CACHE, key = "#plan.id + ':' + #plan.version")
    public ShoppingListDto calculate(WeeklyPlan plan) {
        List<PlannedMeal> meals = plan.getMeals();
        Map<Long, RecipeIngredients> recipes = catalog.getIngredients(
                        meals.stream().map(PlannedMeal::getRecipeId).distinct().toList())
                .stream().collect(Collectors.toMap(RecipeIngredients::recipeId, Function.identity()));

        ShoppingListBuilder builder = new ShoppingListBuilder();
        for (PlannedMeal meal : meals) {
            RecipeIngredients r = recipes.get(meal.getRecipeId());
            if (r != null) {
                builder.add(r.title(), r.ingredients(), (double) meal.getServings() / Math.max(1, r.originalServings()));
            }
        }
        List<ShoppingItem> items = builder.build();
        return new ShoppingListDto(plan.getId(), plan.getWeekStart(), plan.getPeople(), items,
                pricing.estimate(items).orElse(null), Instant.now(clock));
    }

    @CacheEvict(cacheNames = CACHE, allEntries = true)
    public void evictAll() {
        // el trabajo lo hace la anotación
    }
}
