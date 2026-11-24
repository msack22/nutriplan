package com.nutriplan.pricing.infrastructure.config;

import com.nutriplan.pricing.domain.port.SupermarketCatalog;
import com.nutriplan.pricing.infrastructure.vtex.SupermarketProperties;
import com.nutriplan.pricing.infrastructure.vtex.VtexCatalogAdapter;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import java.time.Clock;
import java.time.Duration;
import java.time.ZoneId;
import java.util.List;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.http.HttpHeaders;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

@Configuration
@EnableJpaAuditing
@EnableConfigurationProperties(SupermarketProperties.class)
public class PricingConfig {

    @Bean
    Clock clock() {
        return Clock.system(ZoneId.of("America/Argentina/Buenos_Aires"));
    }

    /** Un adapter por cadena habilitada (Factory a partir de configuración). */
    @Bean
    List<SupermarketCatalog> supermarketCatalogs(SupermarketProperties props, RestClient.Builder builder) {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(Duration.ofSeconds(5));
        factory.setReadTimeout(Duration.ofSeconds(10));
        RestClient http = builder.clone()
                .requestFactory(factory)
                .defaultHeader(HttpHeaders.USER_AGENT, "NutriPlan/0.1 (proyecto educativo; consulta semanal)")
                .build();
        CircuitBreakerConfig cb = CircuitBreakerConfig.custom()
                .slidingWindowSize(10)
                .failureRateThreshold(50)                       // 50% de fallos en 10 llamadas...
                .waitDurationInOpenState(Duration.ofMinutes(2)) // ...=> abierto 2 minutos
                .build();
        return props.chains().stream()
                .filter(SupermarketProperties.Chain::enabled)
                .map(c -> (SupermarketCatalog) new VtexCatalogAdapter(c.id(), c.name(), c.host(), http,
                        CircuitBreaker.of("vtex-" + c.id(), cb)))
                .toList();
    }
}
