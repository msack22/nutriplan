package com.nutriplan.recipe.infrastructure.ai;

import com.nutriplan.recipe.domain.model.Nutrition;
import com.nutriplan.recipe.domain.port.RecipeDraftGenerator;
import java.util.List;
import java.util.Map;
import org.springframework.web.client.RestClient;
import tools.jackson.databind.ObjectMapper;

/**
 * Genera el borrador con un LLM local (Ollama) usando structured outputs (JSON Schema).
 *
 * <p>Prompt injection: el texto del usuario va en el mensaje "user", con límites de largo
 * validados antes; el resultado solo lo ve el mismo usuario y se valida/recorta acá. Nunca se
 * ejecuta nada de lo que devuelve el modelo.
 */
public class OllamaRecipeDraftGenerator implements RecipeDraftGenerator {

    private static final Map<String, Object> SCHEMA = Map.of(
            "type", "object",
            "properties", Map.of(
                    "title", Map.of("type", "string"),
                    "servings", Map.of("type", "integer"),
                    "ingredients", Map.of("type", "array", "items", Map.of("type", "string")),
                    "steps", Map.of("type", "array", "items", Map.of("type", "string")),
                    "kcal", Map.of("type", "number"),
                    "proteinG", Map.of("type", "number"),
                    "carbsG", Map.of("type", "number"),
                    "fatG", Map.of("type", "number")),
            "required", List.of("title", "servings", "ingredients", "steps", "kcal", "proteinG", "carbsG", "fatG"));

    private static final String SYSTEM = """
            Sos un chef nutricionista argentino. Creá UNA receta saludable para bajar de peso usando
            principalmente los ingredientes indicados (podés sumar básicos: sal, pimienta, aceite, especias).
            Cada ingrediente con cantidad concreta (g, ml, unidades, cucharadas). Pasos claros y cortos.
            Macros estimados POR PORCIÓN. Español rioplatense. Respondé solo el JSON pedido.""";

    private static final Map<String, String> MEAL = Map.of(
            "BREAKFAST", "desayuno", "LUNCH", "almuerzo", "SNACK", "merienda", "DINNER", "cena");

    private final RestClient client;
    private final String model;
    private final ObjectMapper json;

    public OllamaRecipeDraftGenerator(RestClient ollamaClient, String model, ObjectMapper json) {
        this.client = ollamaClient;
        this.model = model;
        this.json = json;
    }

    @Override
    public RecipeDraft generate(DraftRequest r) {
        String user = "Ingredientes: %s.%nComida: %s.%nPorciones: %d.%s".formatted(
                String.join(", ", r.ingredients()),
                r.mealType() == null ? "cualquiera" : MEAL.get(r.mealType().name()),
                r.servings(),
                r.notes() == null || r.notes().isBlank() ? "" : "%nPreferencias: " + r.notes());
        OllamaNutritionEstimator.ChatResponse response = client.post().uri("/api/chat")
                .body(Map.of("model", model, "stream", false, "format", SCHEMA,
                        "options", Map.of("temperature", 0.4),
                        "messages", List.of(Map.of("role", "system", "content", SYSTEM),
                                Map.of("role", "user", "content", user))))
                .retrieve()
                .body(OllamaNutritionEstimator.ChatResponse.class);
        if (response == null || response.message() == null) {
            throw new IllegalStateException("El modelo no respondió");
        }
        Raw raw = json.readValue(response.message().content(), Raw.class);
        Nutrition n = new Nutrition(raw.kcal(), raw.proteinG(), raw.carbsG(), raw.fatG());
        return new RecipeDraft(
                cut(raw.title(), 120),
                Math.max(1, Math.min(24, raw.servings() == null ? r.servings() : raw.servings())),
                raw.ingredients() == null ? List.of() : raw.ingredients().stream().limit(40).map(s -> cut(s, 200)).toList(),
                raw.steps() == null ? List.of() : raw.steps().stream().limit(30).map(s -> cut(s, 1000)).toList(),
                OllamaNutritionEstimator.isPlausible(n) ? n : null,
                model);
    }

    private static String cut(String s, int max) {
        String t = s == null ? "" : s.strip();
        return t.length() > max ? t.substring(0, max) : t;
    }

    record Raw(String title, Integer servings, List<String> ingredients, List<String> steps,
               Double kcal, Double proteinG, Double carbsG, Double fatG) {
    }
}
