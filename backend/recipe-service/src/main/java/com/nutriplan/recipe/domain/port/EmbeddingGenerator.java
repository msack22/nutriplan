package com.nutriplan.recipe.domain.port;

/**
 * Puerto de salida (hexagonal): "algo que convierte texto en un vector".
 * Implementaciones (adapters): Ollama (modelo real) o Hashing (determinístico, para tests/offline).
 * El dominio no sabe cuál se usa => patrón Strategy + inversión de dependencias (la D de SOLID).
 */
public interface EmbeddingGenerator {

    /** Embedding de un documento (una receta). */
    float[] embedDocument(String text);

    /** Embedding de una consulta del usuario ("algo con pollo y verduras, liviano"). */
    float[] embedQuery(String text);

    /** Identifica el modelo: si cambia, hay que re-indexar (los vectores no son comparables). */
    String modelName();
}
