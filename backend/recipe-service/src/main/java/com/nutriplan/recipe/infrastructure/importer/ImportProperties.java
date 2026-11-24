package com.nutriplan.recipe.infrastructure.importer;

import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * {@code nutriplan.import.*}. Las propiedades tipadas (@ConfigurationProperties) son preferibles a
 * {@code @Value}: soportan listas/objetos, se validan y se documentan en un solo lugar.
 */
@ConfigurationProperties("nutriplan.import")
public record ImportProperties(boolean onStartup, List<String> paths) {

    public ImportProperties {
        paths = paths == null ? List.of() : List.copyOf(paths);
    }
}
