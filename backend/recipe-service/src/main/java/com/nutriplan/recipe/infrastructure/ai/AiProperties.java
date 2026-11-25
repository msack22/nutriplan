package com.nutriplan.recipe.infrastructure.ai;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuración de IA ({@code nutriplan.ai.*}).
 *
 * @param provider "ollama" (modelo local real) o "hashing" (sin IA, determinístico: tests/CI)
 */
@ConfigurationProperties("nutriplan.ai")
public record AiProperties(String provider, Ollama ollama) {

    public AiProperties {
        provider = provider == null ? "hashing" : provider;
        ollama = ollama == null ? new Ollama(null, null, null, null) : ollama;
    }

    public record Ollama(String baseUrl, String embeddingModel, String chatModel, Duration timeout) {
        public Ollama {
            baseUrl = baseUrl == null ? "http://localhost:11434" : baseUrl;
            embeddingModel = embeddingModel == null ? "nomic-embed-text" : embeddingModel;
            chatModel = chatModel == null ? "qwen2.5:7b" : chatModel;
            timeout = timeout == null ? Duration.ofMinutes(10) : timeout;   // CPU sin GPU: respuestas lentas
        }
    }
}
