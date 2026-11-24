package com.nutriplan.recipe.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;

import com.nutriplan.recipe.application.dto.ImportedRecipe;
import com.nutriplan.recipe.infrastructure.importer.RecipeExportReader;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.json.JsonMapper;

/** Verifica que la muestra pública del repo (data/sample) se importa correctamente. */
class RecipeExportReaderTest {

    @Test
    void readsPublicSampleDataset() throws Exception {
        List<ImportedRecipe> recipes = new RecipeExportReader(JsonMapper.builder().build())
                .read(Path.of("../../data/sample/recipes-sample.json"));

        assertThat(recipes).hasSize(8);
        ImportedRecipe chicken = recipes.stream().filter(r -> r.title().startsWith("Pollo al limón")).findFirst().orElseThrow();
        assertThat(chicken.categorySlug()).isEqualTo("comidas-principales");
        assertThat(chicken.servings()).isEqualTo(2);
        assertThat(chicken.ingredientLines()).hasSize(6).contains("300 g de pechuga de pollo");
        assertThat(chicken.steps()).hasSize(3);
        assertThat(chicken.nutritionPerServing().kcal()).isEqualTo(320.0);
    }
}
