package com.nutriplan.recipe.domain.service;

/** Operaciones de álgebra lineal para búsqueda semántica. */
public final class VectorMath {

    private VectorMath() {
    }

    /**
     * Similitud coseno: cos(θ) = (a·b) / (|a|·|b|), en [-1, 1].
     * 1 = mismo "significado", 0 = no relacionados. Es la métrica estándar para embeddings de texto.
     */
    public static double cosine(float[] a, float[] b) {
        if (a.length != b.length) {
            throw new IllegalArgumentException("Dimensiones distintas: %d vs %d".formatted(a.length, b.length));
        }
        double dot = 0, normA = 0, normB = 0;
        for (int i = 0; i < a.length; i++) {
            dot += a[i] * b[i];
            normA += a[i] * a[i];
            normB += b[i] * b[i];
        }
        return (normA == 0 || normB == 0) ? 0 : dot / (Math.sqrt(normA) * Math.sqrt(normB));
    }

    public static float[] normalize(float[] v) {
        double norm = 0;
        for (float x : v) {
            norm += x * x;
        }
        norm = Math.sqrt(norm);
        float[] out = new float[v.length];
        for (int i = 0; i < v.length; i++) {
            out[i] = norm == 0 ? 0 : (float) (v[i] / norm);
        }
        return out;
    }
}
