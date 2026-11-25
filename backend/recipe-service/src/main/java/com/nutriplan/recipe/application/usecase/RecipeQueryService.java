package com.nutriplan.recipe.application.usecase;

import static com.nutriplan.recipe.domain.repository.RecipeSpecifications.active;
import static com.nutriplan.recipe.domain.repository.RecipeSpecifications.forMeal;
import static com.nutriplan.recipe.domain.repository.RecipeSpecifications.inCategory;
import static com.nutriplan.recipe.domain.repository.RecipeSpecifications.maxKcal;
import static com.nutriplan.recipe.domain.repository.RecipeSpecifications.minProtein;
import static com.nutriplan.recipe.domain.repository.RecipeSpecifications.onlyMine;
import static com.nutriplan.recipe.domain.repository.RecipeSpecifications.visibleTo;
import static com.nutriplan.recipe.domain.repository.RecipeSpecifications.titleContains;
import static com.nutriplan.recipe.domain.repository.RecipeSpecifications.withTag;

import com.nutriplan.common.error.ResourceNotFoundException;
import com.nutriplan.recipe.application.dto.CategoryDto;
import com.nutriplan.recipe.application.dto.IngredientDto;
import com.nutriplan.recipe.application.dto.PageResponse;
import com.nutriplan.recipe.application.dto.RecipeDetailDto;
import com.nutriplan.recipe.application.dto.RecipeSearchCriteria;
import com.nutriplan.recipe.application.dto.RecipeSummaryDto;
import com.nutriplan.recipe.application.mapper.RecipeMapper;
import com.nutriplan.recipe.domain.model.Recipe;
import com.nutriplan.recipe.domain.model.RecipeIngredient;
import com.nutriplan.recipe.domain.repository.CategoryRepository;
import com.nutriplan.recipe.domain.repository.RecipeRepository;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Casos de uso de LECTURA (lado "Query" de un CQRS liviano).
 * {@code @Transactional(readOnly = true)}: Hibernate omite el dirty-checking (más rápido) y el
 * driver puede enrutar a una réplica de lectura. Con open-in-view=false, todo el mapeo a DTO
 * debe ocurrir dentro de este método transaccional (si no: LazyInitializationException).
 */
@Service
@Transactional(readOnly = true)
public class RecipeQueryService {

    private final RecipeRepository recipes;
    private final CategoryRepository categories;
    private final RecipeMapper mapper;

    public RecipeQueryService(RecipeRepository recipes, CategoryRepository categories, RecipeMapper mapper) {
        this.recipes = recipes;
        this.categories = categories;
        this.mapper = mapper;
    }

    public PageResponse<RecipeSummaryDto> search(RecipeSearchCriteria c, Pageable pageable, String userId) {
        Specification<Recipe> spec = active()
                .and(visibleTo(userId))
                .and(onlyMine(c.mine(), userId))
                .and(titleContains(c.text()))
                .and(inCategory(c.category()))
                .and(forMeal(c.mealType()))
                .and(withTag(c.tag()))
                .and(maxKcal(c.maxKcal()))
                .and(minProtein(c.minProtein()));
        return PageResponse.of(recipes.findAll(spec, pageable), mapper::toSummary);
    }

    /** Anti-IDOR: la receta privada de otro usuario "no existe" (404). */
    public RecipeDetailDto getDetail(long id, Integer servings, String userId) {
        Recipe recipe = recipes.findWithIngredientsById(id)
                .filter(r -> r.isVisibleTo(userId))
                .orElseThrow(() -> new ResourceNotFoundException("Receta", id));
        return toDetail(recipe, servings == null ? recipe.getServings() : servings, userId);
    }

    public List<RecipeDetailDto> getDetails(Collection<Long> ids, String userId) {
        return recipes.findWithIngredientsByIdIn(ids).stream()
                .filter(r -> r.getOwnerId() == null || r.getOwnerId().equals(userId))
                .map(r -> toDetail(r, r.getServings(), userId))
                .toList();
    }

    public List<CategoryDto> categories() {
        return categories.findAllByOrderBySortOrderAsc().stream().map(mapper::toDto).toList();
    }

    public List<String> ingredientNames() {
        return recipes.findDistinctIngredientNames();
    }

    /** Escala ingredientes y macros: factor = porciones pedidas / porciones originales. */
    private RecipeDetailDto toDetail(Recipe r, int servings, String userId) {
        if (servings < 1 || servings > 50) {
            throw new IllegalArgumentException("Las porciones deben estar entre 1 y 50");
        }
        double factor = (double) servings / r.getServings();
        List<IngredientDto> ingredients = r.getIngredients().stream()
                .map(i -> scale(i, factor))
                .toList();
        return new RecipeDetailDto(r.getId(), r.getTitle(), mapper.toDto(r.getCategory()), servings,
                r.getServings(), r.getYieldLabel(), r.getNutrition(), r.getNutrition().times(servings),
                // Copiamos las colecciones LAZY dentro de la transacción: si devolviéramos la PersistentSet de
                // Hibernate, Jackson la recorrería fuera de la sesión => LazyInitializationException.
                r.getNutritionSource(), Set.copyOf(r.getMealTypes()), Set.copyOf(r.getTags()), ingredients,
                List.copyOf(r.getSteps()),
                r.getNotes(), r.getVideoUrl(), r.getVersion(), r.isCustom(), r.isOwnedBy(userId));
    }

    private static IngredientDto scale(RecipeIngredient i, double factor) {
        Double qty = i.getQuantity() == null ? null : Math.round(i.getQuantity() * factor * 100) / 100.0;
        return new IngredientDto(i.getName(), qty, i.getUnit(), i.getRawText(), i.isOptional());
    }
}
