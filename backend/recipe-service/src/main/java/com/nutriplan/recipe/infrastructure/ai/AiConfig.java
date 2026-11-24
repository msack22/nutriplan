package com.nutriplan.recipe.infrastructure.ai;

import com.nutriplan.recipe.domain.port.EmbeddingGenerator;
import com.nutriplan.recipe.domain.port.NutritionEstimator;
import java.util.Optional;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;
import tools.jackson.databind.ObjectMapper;

/**
 * Elige el adapter de IA según configuración (Strategy seleccionada por propiedades).
 * Agregar OpenAI/Bedrock sería solo otro bean condicional: los casos de uso no cambian
 * (principio Open/Closed).
 */
@Configuration
@EnableConfigurationProperties(AiProperties.class)
public class AiConfig {

    @Configuration
    @ConditionalOnProperty(name = "nutriplan.ai.provider", havingValue = "ollama")
    static class OllamaConfig {

        @Bean
        RestClient ollamaRestClient(RestClient.Builder builder, AiProperties props) {
            SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
            factory.setReadTimeout(props.ollama().timeout());
            return builder.clone().baseUrl(props.ollama().baseUrl()).requestFactory(factory).build();
        }

        @Bean
        EmbeddingGenerator ollamaEmbeddingGenerator(RestClient ollamaRestClient, AiProperties props) {
            return new OllamaEmbeddingGenerator(ollamaRestClient, props.ollama().embeddingModel());
        }

        @Bean
        NutritionEstimator ollamaNutritionEstimator(RestClient ollamaRestClient, AiProperties props,
                                                    ObjectMapper objectMapper) {
            return new OllamaNutritionEstimator(ollamaRestClient, props.ollama().chatModel(), objectMapper);
        }
    }

    @Configuration
    @ConditionalOnProperty(name = "nutriplan.ai.provider", havingValue = "hashing", matchIfMissing = true)
    static class OfflineConfig {

        @Bean
        EmbeddingGenerator hashingEmbeddingGenerator() {
            return new HashingEmbeddingGenerator();
        }

        /** Sin LLM no se estima nada (Null Object pattern). */
        @Bean
        NutritionEstimator noopNutritionEstimator() {
            return (title, servings, lines) -> Optional.empty();
        }
    }
}
