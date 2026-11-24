package com.nutriplan.recipe.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

import com.nutriplan.recipe.domain.service.VectorMath;
import com.nutriplan.recipe.infrastructure.ai.HashingEmbeddingGenerator;
import org.junit.jupiter.api.Test;

class VectorMathTest {

    @Test
    void cosineOfIdenticalVectorsIsOne() {
        assertThat(VectorMath.cosine(new float[]{1, 2, 3}, new float[]{2, 4, 6})).isCloseTo(1.0, within(1e-9));
    }

    @Test
    void cosineOfOrthogonalVectorsIsZero() {
        assertThat(VectorMath.cosine(new float[]{1, 0}, new float[]{0, 1})).isZero();
    }

    @Test
    void similarTextsAreCloserThanUnrelatedOnes() {
        var gen = new HashingEmbeddingGenerator();
        float[] query = gen.embedQuery("ensalada de pollo con tomate");
        double related = VectorMath.cosine(query, gen.embedDocument("Ensalada griega con pollo, tomate y pepino"));
        double unrelated = VectorMath.cosine(query, gen.embedDocument("Brownie de chocolate y nueces"));
        assertThat(related).isGreaterThan(unrelated);
    }
}
