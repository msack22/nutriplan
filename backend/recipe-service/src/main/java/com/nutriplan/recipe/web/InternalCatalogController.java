package com.nutriplan.recipe.web;

import com.nutriplan.recipe.application.usecase.RecipeQueryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Endpoints para OTROS microservicios (no para usuarios). Protegidos por la regla
 * {@code /internal/** -> hasRole('SERVICE')} del SecurityFilterChain: solo tokens obtenidos con
 * Client Credentials por un service account con rol SERVICE. El Gateway NO enruta /internal.
 */
@RestController
@RequestMapping("/internal")
@Tag(name = "Interno (service-to-service)")
public class InternalCatalogController {

    private final RecipeQueryService queries;

    public InternalCatalogController(RecipeQueryService queries) {
        this.queries = queries;
    }

    @GetMapping("/ingredients/names")
    @Operation(summary = "Nombres normalizados de todos los ingredientes (para cotizar precios)")
    public List<String> ingredientNames() {
        return queries.ingredientNames();
    }
}
