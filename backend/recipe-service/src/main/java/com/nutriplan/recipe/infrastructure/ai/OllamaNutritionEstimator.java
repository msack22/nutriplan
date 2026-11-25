package com.nutriplan.recipe.infrastructure.ai;

import com.nutriplan.recipe.domain.model.Nutrition;
import com.nutriplan.recipe.domain.port.NutritionEstimator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.client.RestClient;
import tools.jackson.databind.ObjectMapper;

/**
 * Estima macros con un LLM local (Ollama, p.ej. qwen2.5:7b) usando "structured outputs":
 * se le pasa un JSON Schema en {@code format} y el modelo está obligado a responder con ese JSON.
 *
 * <p>Es una ESTIMACIÓN: se guarda con {@code NutritionSource.ESTIMATED} y la UI lo indica.
 */
public class OllamaNutritionEstimator implements NutritionEstimator {

    private static final Logger log = LoggerFactory.getLogger(OllamaNutritionEstimator.class);

    private static final Map<String, Object> SCHEMA = Map.of(
            "type", "object",
            "properties", Map.of(
                    "kcal", Map.of("type", "number"),
                    "proteinG", Map.of("type", "number"),
                    "carbsG", Map.of("type", "number"),
                    "fatG", Map.of("type", "number")),
            "required", List.of("kcal", "proteinG", "carbsG", "fatG"));

    private static final String SYSTEM_PROMPT = """
            Sos un nutricionista. Estimá los macronutrientes POR PORCIÓN de la receta usando valores
            típicos de tablas nutricionales (USDA / Argenfoods). Si un ingrediente no tiene cantidad,
            asumí una cantidad razonable para una porción. Respondé solo el JSON pedido.""";

    private final RestClient client;
    private final String model;
    private final ObjectMapper json;

    public OllamaNutritionEstimator(RestClient ollamaClient, String model, ObjectMapper json) {
        this.client = ollamaClient;
        this.model = model;
        this.json = json;
    }

    @Override
    public Optional<Nutrition> estimatePerServing(String title, int servings, List<String> ingredientLines) {
        String user = "Receta: %s%nRinde: %d porciones%nIngredientes:%n- %s".formatted(
                title, servings, String.join("\n- ", ingredientLines));
        try {
            ChatResponse response = client.post().uri("/api/chat")
                    .body(Map.of(
                            "model", model,
                            "stream", false,
                            "format", SCHEMA,
                            "options", Map.of("temperature", 0),
                            "messages", List.of(
                                    Map.of("role", "system", "content", SYSTEM_PROMPT),
                                    Map.of("role", "user", "content", user))))
                    .retrieve()
                    .body(ChatResponse.class);
            if (response == null || response.message() == null) {
                return Optional.empty();
            }
            Nutrition n = json.readValue(response.message().content(), Nutrition.class);
            return isPlausible(n) ? Optional.of(round(n)) : Optional.empty();
        } catch (RuntimeException e) {
            log.warn("No se pudo estimar la nutrición de '{}': {}", title, e.getMessage());
            return Optional.empty();
        }
    }

    /** Validación defensiva: los LLM pueden "alucinar" valores absurdos. */
    static boolean isPlausible(Nutrition n) {
        return n != null && n.kcal() != null && n.kcal() > 10 && n.kcal() < 2500
                && n.proteinG() != null && n.proteinG() >= 0 && n.proteinG() < 200;
    }

    private static Nutrition round(Nutrition n) {
        return new Nutrition((double) Math.round(n.kcal()), r(n.proteinG()), r(n.carbsG()), r(n.fatG()));
    }

    private static Double r(Double v) {
        return v == null ? null : Math.round(v * 10) / 10.0;
    }

    public record ChatResponse(Message message) {
    }

    public record Message(String role, String content) {
    }
}
