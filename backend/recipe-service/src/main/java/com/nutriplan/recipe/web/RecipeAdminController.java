package com.nutriplan.recipe.web;

import com.nutriplan.recipe.application.dto.ImportReport;
import com.nutriplan.recipe.application.usecase.NutritionEstimationService;
import com.nutriplan.recipe.application.usecase.RecipeImportService;
import com.nutriplan.recipe.application.usecase.RecipeIndexingService;
import com.nutriplan.recipe.domain.model.Nutrition;
import com.nutriplan.recipe.infrastructure.importer.RecipeExportReader;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/**
 * Operaciones de administración. {@code @PreAuthorize} a nivel de clase = method security:
 * aunque alguien descubra la URL, sin rol ADMIN en el JWT recibe 403.
 */
@RestController
@RequestMapping("/api/admin/recipes")
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "Admin de recetas", description = "Requiere rol ADMIN")
public class RecipeAdminController {

    private final RecipeImportService importService;
    private final RecipeExportReader reader;
    private final RecipeIndexingService indexing;
    private final NutritionEstimationService nutrition;

    public RecipeAdminController(RecipeImportService importService, RecipeExportReader reader,
                                 RecipeIndexingService indexing, NutritionEstimationService nutrition) {
        this.importService = importService;
        this.reader = reader;
        this.indexing = indexing;
        this.nutrition = nutrition;
    }

    @PostMapping(value = "/import", consumes = "multipart/form-data")
    @Operation(summary = "Importa (upsert) recetas desde un export JSON")
    public ImportReport importFile(@RequestParam("file") MultipartFile file) throws IOException {
        Path tmp = Files.createTempFile("recipes-", ".json");
        try {
            file.transferTo(tmp);
            ImportReport report = importService.importRecipes(reader.read(tmp));
            indexing.reindex(false);
            return report;
        } finally {
            Files.deleteIfExists(tmp);
        }
    }

    @PostMapping("/reindex")
    @Operation(summary = "Regenera embeddings e índice vectorial")
    public Map<String, Integer> reindex(@RequestParam(defaultValue = "false") boolean force) {
        return Map.of("generated", indexing.reindex(force));
    }

    @PostMapping("/estimate-nutrition")
    @Operation(summary = "Estima con LLM la nutrición faltante (asíncrono, responde 202)")
    public ResponseEntity<Map<String, Object>> estimateNutrition() {
        nutrition.estimateMissingAsync();
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(Map.of("started", true));
    }

    @PutMapping("/{id}/nutrition")
    @Operation(summary = "Corrige manualmente la nutrición por porción")
    public ResponseEntity<Void> setNutrition(@PathVariable long id, @RequestBody Nutrition perServing) {
        nutrition.setManual(id, perServing);
        return ResponseEntity.noContent().build();
    }
}
