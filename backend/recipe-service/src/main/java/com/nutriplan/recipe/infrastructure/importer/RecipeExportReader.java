package com.nutriplan.recipe.infrastructure.importer;

import com.nutriplan.recipe.application.dto.ImportedRecipe;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

/**
 * Adapter de ENTRADA: lee el export JSON del recetario y lo traduce al contrato
 * {@link ImportedRecipe}. Si mañana las recetas vienen de un CSV o de una API, se agrega otro
 * reader y el caso de uso de importación no cambia.
 *
 * <p>Estructura esperada: {@code catalogo.escritas[]} (categorías), {@code catalogo.recetas[]}
 * (metadatos) y {@code cuerpos{id: {cuerpo: markdown}}}.
 */
@Component
public class RecipeExportReader {

    private final ObjectMapper json;

    public RecipeExportReader(ObjectMapper json) {
        this.json = json;
    }

    public List<ImportedRecipe> read(Path file) throws IOException {
        JsonNode root = json.readTree(Files.readString(file));
        JsonNode catalog = root.path("catalogo");

        record Cat(String slug, String name, String emoji, int order) { }
        Map<String, Cat> categories = new HashMap<>();
        for (JsonNode c : catalog.path("escritas")) {
            categories.put(c.path("id").asString(), new Cat(c.path("slug").asString(), c.path("title").asString(),
                    c.path("emoji").asString(""), c.path("sort_order").asInt()));
        }

        List<ImportedRecipe> result = new ArrayList<>();
        for (JsonNode r : catalog.path("recetas")) {
            if (!r.path("active").asBoolean(true)) {
                continue;
            }
            String id = r.path("id").asString();
            Cat cat = categories.get(r.path("category_id").asString());
            if (cat == null) {
                continue;
            }
            String body = root.path("cuerpos").path(id).path("cuerpo").asString("");
            RecipeBodyParser.ParsedBody parsed = RecipeBodyParser.parse(body);
            JsonNode sub = r.path("subcategoria");
            result.add(new ImportedRecipe(id, r.path("title").asString().strip(), cat.slug(), cat.name(), cat.emoji(),
                    cat.order(), sub.isNull() || sub.isMissingNode() ? null : sub.asString(),
                    parsed.servings(), parsed.yieldLabel(), parsed.ingredients(), parsed.steps(), parsed.notes(),
                    parsed.nutrition(), null));
        }
        return result;
    }
}
