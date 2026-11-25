package com.nutriplan.recipe.web;

import com.nutriplan.common.security.CurrentUser;
import com.nutriplan.recipe.application.dto.RecipeCommand;
import com.nutriplan.recipe.application.dto.RecipeDetailDto;
import com.nutriplan.recipe.application.usecase.RecipeAuthoringService;
import com.nutriplan.recipe.application.usecase.RecipeDraftService;
import com.nutriplan.recipe.application.usecase.RecipeDraftService.DraftJob;
import com.nutriplan.recipe.domain.model.MealType;
import com.nutriplan.recipe.domain.port.RecipeDraftGenerator.DraftRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.net.URI;
import java.util.List;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Recetas propias del usuario (privadas) y borradores generados con IA. */
@RestController
@RequestMapping("/api/recipes")
@Tag(name = "Mis recetas", description = "Crear, editar y borrar recetas propias; borradores con IA")
public class MyRecipesController {

    private final RecipeAuthoringService authoring;
    private final RecipeDraftService drafts;

    public MyRecipesController(RecipeAuthoringService authoring, RecipeDraftService drafts) {
        this.authoring = authoring;
        this.drafts = drafts;
    }

    @PostMapping
    @Operation(summary = "Crea una receta propia (solo la ve su dueño)")
    public ResponseEntity<RecipeDetailDto> create(@RequestBody @Valid RecipeCommand command) {
        RecipeDetailDto created = authoring.create(CurrentUser.id(), command);
        return ResponseEntity.created(URI.create("/api/recipes/" + created.id())).body(created);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Edita una receta propia")
    public RecipeDetailDto update(@PathVariable long id, @RequestBody @Valid RecipeCommand command) {
        return authoring.update(CurrentUser.id(), id, command);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Borra una receta propia")
    public ResponseEntity<Void> delete(@PathVariable long id) {
        authoring.delete(CurrentUser.id(), id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/drafts")
    @Operation(summary = "Pide un borrador de receta a la IA (asíncrono: 202 + id para consultar)")
    public ResponseEntity<DraftJob> startDraft(@RequestBody @Valid DraftBody body) {
        DraftJob job = drafts.start(CurrentUser.id(), new DraftRequest(
                body.ingredients().stream().map(String::strip).toList(), body.mealType(),
                body.servings() == null ? 2 : body.servings(), body.notes()));
        return ResponseEntity.status(HttpStatus.ACCEPTED)
                .location(URI.create("/api/recipes/drafts/" + job.id())).body(job);
    }

    @GetMapping("/drafts/{jobId}")
    @Operation(summary = "Estado del borrador (PENDING / DONE / FAILED)")
    public DraftJob draft(@PathVariable String jobId) {
        return drafts.get(CurrentUser.id(), jobId);
    }

    /** Límites de entrada: también acotan lo que llega al prompt del LLM. */
    public record DraftBody(
            @jakarta.validation.constraints.NotEmpty @Size(max = 15) List<@NotBlank @Size(max = 60) String> ingredients,
            MealType mealType,
            @Min(1) @Max(8) Integer servings,
            @Size(max = 200) String notes) {
    }
}
