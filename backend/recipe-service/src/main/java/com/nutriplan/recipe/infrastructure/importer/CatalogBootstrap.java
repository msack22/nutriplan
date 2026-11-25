package com.nutriplan.recipe.infrastructure.importer;

import com.nutriplan.recipe.application.usecase.RecipeImportService;
import com.nutriplan.recipe.application.usecase.RecipeIndexingService;
import com.nutriplan.recipe.domain.repository.RecipeRepository;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Al arrancar: si la base está vacía importa las recetas y luego sincroniza el índice vectorial.
 * Cada entrada de {@code nutriplan.import.paths} puede ser un archivo o una CARPETA (se importan
 * todos sus .json). Se usa la primera entrada que tenga datos: la carpeta privada (export + recetas
 * extraídas de los PDFs) o, si no existe, la muestra pública del repo.
 * Se desactiva con {@code nutriplan.import.on-startup=false} (p.ej. en tests).
 */
@Component
@EnableConfigurationProperties(ImportProperties.class)
@ConditionalOnProperty(name = "nutriplan.import.on-startup", havingValue = "true", matchIfMissing = true)
public class CatalogBootstrap implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(CatalogBootstrap.class);

    private final RecipeRepository recipes;
    private final RecipeExportReader reader;
    private final RecipeImportService importService;
    private final RecipeIndexingService indexing;
    private final List<String> paths;

    public CatalogBootstrap(RecipeRepository recipes, RecipeExportReader reader, RecipeImportService importService,
                            RecipeIndexingService indexing, ImportProperties properties) {
        this.recipes = recipes;
        this.reader = reader;
        this.importService = importService;
        this.indexing = indexing;
        this.paths = properties.paths();
    }

    /** Un archivo .json, o todos los .json de una carpeta (ordenados por nombre). */
    static List<Path> jsonFiles(Path path) {
        if (Files.isRegularFile(path)) {
            return List.of(path);
        }
        if (!Files.isDirectory(path)) {
            return List.of();
        }
        try (var stream = Files.list(path)) {
            return stream.filter(p -> p.toString().endsWith(".json")).sorted().toList();
        } catch (java.io.IOException e) {
            return List.of();
        }
    }

    @Override
    public void run(ApplicationArguments args) throws Exception {
        if (recipes.count() == 0) {
            List<Path> files = paths.stream().map(Path::of).map(CatalogBootstrap::jsonFiles)
                    .filter(list -> !list.isEmpty()).findFirst().orElse(List.of());
            if (files.isEmpty()) {
                log.warn("No hay archivos de recetas para importar. Buscado en: {}", paths);
            }
            for (Path file : files) {
                log.info("Importando recetas desde {}", file.toAbsolutePath());
                importService.importRecipes(reader.read(file));
            }
        }
        try {
            indexing.reindex(false);
        } catch (RuntimeException e) {
            log.warn("No se pudo construir el índice vectorial (¿Ollama apagado?). La búsqueda semántica "
                    + "no estará disponible hasta llamar a POST /api/admin/recipes/reindex. Causa: {}", e.getMessage());
        }
    }
}
