package com.nutriplan.recipe.infrastructure.ai;

import com.nutriplan.recipe.domain.port.RecipeDraftGenerator;
import java.util.List;

/** Modo sin IA (tests/CI): arma un borrador mínimo para que el usuario lo complete a mano. */
public class TemplateRecipeDraftGenerator implements RecipeDraftGenerator {

    @Override
    public RecipeDraft generate(DraftRequest r) {
        String main = String.join(", ", r.ingredients().stream().limit(3).toList());
        return new RecipeDraft("Receta con " + main, r.servings(),
                r.ingredients().stream().map(i -> "cantidad a gusto de " + i).toList(),
                List.of("Preparar los ingredientes.", "Cocinar a gusto.", "Servir."), null, "plantilla");
    }
}
