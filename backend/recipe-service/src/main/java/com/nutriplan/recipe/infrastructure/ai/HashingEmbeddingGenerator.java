package com.nutriplan.recipe.infrastructure.ai;

import com.nutriplan.recipe.domain.port.EmbeddingGenerator;
import com.nutriplan.recipe.domain.service.VectorMath;
import java.text.Normalizer;
import java.util.Locale;

/**
 * Embedding "de juguete" por feature hashing (bag of words -> vector de 512 dimensiones).
 * No entiende sinónimos como un modelo real, pero:
 *  - no necesita red ni GPU => ideal para tests y CI
 *  - es determinístico => tests reproducibles
 * Es el adapter por defecto si {@code nutriplan.ai.provider=hashing}.
 */
public class HashingEmbeddingGenerator implements EmbeddingGenerator {

    private static final int DIMENSIONS = 512;

    @Override
    public float[] embedDocument(String text) {
        return embed(text);
    }

    @Override
    public float[] embedQuery(String text) {
        return embed(text);
    }

    @Override
    public String modelName() {
        return "hashing-512";
    }

    private static float[] embed(String text) {
        float[] v = new float[DIMENSIONS];
        String clean = Normalizer.normalize(text.toLowerCase(Locale.ROOT), Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "").replaceAll("[^a-z0-9 ]", " ");
        for (String token : clean.split("\\s+")) {
            if (token.length() < 3) {
                continue;
            }
            String stem = token.length() > 5 ? token.substring(0, 5) : token; // "stemming" naive
            v[Math.floorMod(stem.hashCode(), DIMENSIONS)] += 1f;
        }
        return VectorMath.normalize(v);
    }
}
