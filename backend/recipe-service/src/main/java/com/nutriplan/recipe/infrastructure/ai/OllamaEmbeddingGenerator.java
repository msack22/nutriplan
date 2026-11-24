package com.nutriplan.recipe.infrastructure.ai;

import com.nutriplan.recipe.domain.port.EmbeddingGenerator;
import java.util.List;
import java.util.Map;
import org.springframework.web.client.RestClient;

/**
 * Adapter que llama a Ollama (LLM local, gratis) para generar embeddings con
 * {@code nomic-embed-text} (768 dimensiones).
 *
 * <p>nomic-embed-text recomienda prefijos distintos para documentos y consultas
 * ("search_document:" / "search_query:"): mejora la calidad de la búsqueda.
 * API: POST /api/embed  {"model": "...", "input": "..."} -> {"embeddings": [[...]]}
 */
public class OllamaEmbeddingGenerator implements EmbeddingGenerator {

    private final RestClient client;
    private final String model;

    public OllamaEmbeddingGenerator(RestClient ollamaClient, String model) {
        this.client = ollamaClient;
        this.model = model;
    }

    @Override
    public float[] embedDocument(String text) {
        return embed("search_document: " + text);
    }

    @Override
    public float[] embedQuery(String text) {
        return embed("search_query: " + text);
    }

    @Override
    public String modelName() {
        return model;
    }

    private float[] embed(String input) {
        EmbedResponse response = client.post().uri("/api/embed")
                .body(Map.of("model", model, "input", input))
                .retrieve()
                .body(EmbedResponse.class);
        if (response == null || response.embeddings() == null || response.embeddings().isEmpty()) {
            throw new IllegalStateException("Ollama no devolvió embeddings");
        }
        List<Float> values = response.embeddings().getFirst();
        float[] vector = new float[values.size()];
        for (int i = 0; i < vector.length; i++) {
            vector[i] = values.get(i);
        }
        return vector;
    }

    record EmbedResponse(List<List<Float>> embeddings) {
    }
}
