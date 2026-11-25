package com.nutriplan.recipe.web;

import com.nutriplan.common.security.CurrentUser;
import com.nutriplan.recipe.application.dto.CandidateRequest;
import com.nutriplan.recipe.application.dto.CategoryDto;
import com.nutriplan.recipe.application.dto.PageResponse;
import com.nutriplan.recipe.application.dto.RecipeCandidateDto;
import com.nutriplan.recipe.application.dto.RecipeDetailDto;
import com.nutriplan.recipe.application.dto.RecipeSearchCriteria;
import com.nutriplan.recipe.application.dto.RecipeSummaryDto;
import com.nutriplan.recipe.application.dto.ScoredRecipeDto;
import com.nutriplan.recipe.application.usecase.RecipeQueryService;
import com.nutriplan.recipe.application.usecase.SemanticSearchService;
import com.nutriplan.recipe.domain.model.DietTag;
import com.nutriplan.recipe.domain.model.MealType;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.List;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * API REST de recetas (cualquier usuario autenticado).
 * El controller es "delgado": valida la entrada y delega en los casos de uso.
 */
@RestController
@RequestMapping("/api")
@Validated
@Tag(name = "Recetas", description = "Catálogo, búsqueda y detalle de recetas")
public class RecipeController {

    private final RecipeQueryService queries;
    private final SemanticSearchService semantic;

    public RecipeController(RecipeQueryService queries, SemanticSearchService semantic) {
        this.queries = queries;
        this.semantic = semantic;
    }

    @GetMapping("/recipes")
    @Operation(summary = "Lista recetas con filtros combinables (Specification) y paginación")
    public PageResponse<RecipeSummaryDto> search(
            @RequestParam(required = false) @Size(max = 100) String q,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) MealType mealType,
            @RequestParam(required = false) DietTag tag,
            @RequestParam(required = false) Double maxKcal,
            @RequestParam(required = false) Double minProtein,
            @RequestParam(defaultValue = "false") boolean mine,
            @PageableDefault(size = 24, sort = "title") Pageable pageable) {
        return queries.search(new RecipeSearchCriteria(q, category, mealType, tag, maxKcal, minProtein, mine), pageable,
                CurrentUser.id());
    }

    @GetMapping("/recipes/{id}")
    @Operation(summary = "Detalle de receta, escalada a N porciones")
    public RecipeDetailDto detail(@PathVariable long id,
                                  @Parameter(description = "Porciones deseadas (por defecto, las de la receta)")
                                  @RequestParam(required = false) @Min(1) @Max(50) Integer servings) {
        return queries.getDetail(id, servings, CurrentUser.id());
    }

    @PostMapping("/recipes/batch")
    @Operation(summary = "Detalle de varias recetas (lo usa planner-service para la lista de compras)")
    public List<RecipeDetailDto> batch(@RequestBody @Size(max = 200) List<Long> ids) {
        return queries.getDetails(ids, CurrentUser.id());
    }

    @GetMapping("/recipes/semantic-search")
    @Operation(summary = "Búsqueda semántica en lenguaje natural (embeddings + similitud coseno)")
    public List<ScoredRecipeDto> semanticSearch(@RequestParam @NotBlank @Size(max = 300) String query,
                                                @RequestParam(required = false) MealType mealType,
                                                @RequestParam(defaultValue = "10") @Min(1) @Max(50) int limit) {
        return semantic.search(query, mealType, limit, CurrentUser.id());
    }

    @GetMapping("/recipes/{id}/similar")
    @Operation(summary = "Recetas parecidas a una dada")
    public List<ScoredRecipeDto> similar(@PathVariable long id,
                                         @RequestParam(defaultValue = "6") @Min(1) @Max(30) int limit) {
        return semantic.similarTo(id, limit, CurrentUser.id());
    }

    @PostMapping("/recipes/candidates")
    @Operation(summary = "Recetas candidatas para el generador automático de planes")
    public List<RecipeCandidateDto> candidates(@RequestBody @Valid CandidateRequest request) {
        return semantic.candidates(request, CurrentUser.id());
    }

    @GetMapping("/categories")
    @Operation(summary = "Categorías del recetario")
    public List<CategoryDto> categories() {
        return queries.categories();
    }
}
