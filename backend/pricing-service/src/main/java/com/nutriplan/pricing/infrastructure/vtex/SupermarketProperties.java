package com.nutriplan.pricing.infrastructure.vtex;

import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;

/** {@code pricing.chains}: qué supermercados cotizar (agregar uno = una línea de YAML). */
@ConfigurationProperties("pricing")
public record SupermarketProperties(List<Chain> chains) {

    public SupermarketProperties {
        chains = chains == null ? List.of() : chains;
    }

    public record Chain(String id, String name, String host, boolean enabled) {
    }
}
