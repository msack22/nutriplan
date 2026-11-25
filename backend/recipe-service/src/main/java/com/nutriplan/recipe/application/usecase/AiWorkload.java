package com.nutriplan.recipe.application.usecase;

import java.util.concurrent.atomic.AtomicInteger;
import org.springframework.stereotype.Component;

/**
 * Prioridad de uso del LLM local (Ollama atiende de a un pedido en CPU).
 * Las tareas INTERACTIVAS (un usuario esperando su borrador) tienen prioridad sobre las de FONDO
 * (estimar nutrición de todo el catálogo): el job de fondo se pausa mientras haya interactivas.
 */
@Component
public class AiWorkload {

    private final AtomicInteger interactive = new AtomicInteger();

    public void beginInteractive() {
        interactive.incrementAndGet();
    }

    public void endInteractive() {
        interactive.decrementAndGet();
    }

    /** Lo llama el job de fondo entre receta y receta. */
    public void yieldToInteractive() {
        while (interactive.get() > 0) {
            try {
                Thread.sleep(2000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return;
            }
        }
    }
}
