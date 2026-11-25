package com.nutriplan.recipe.application.usecase;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Supplier;
import org.springframework.stereotype.Component;

/**
 * Cola única hacia el LLM local, con PRIORIDAD.
 *
 * <p>Problema real que resuelve: en una PC sin GPU, Ollama atendía 3-4 pedidos en paralelo
 * (estimación de fondo + borradores) y cada uno pasaba de ~1,5 min a ~10 min.
 * <ul>
 *   <li>Un solo pedido al LLM a la vez ({@link ReentrantLock} justo/FIFO).</li>
 *   <li>Las tareas INTERACTIVAS (un usuario esperando) pasan primero: las de FONDO no toman el
 *       lock mientras haya interactivas esperando.</li>
 * </ul>
 */
@Component
public class LlmGate {

    private final ReentrantLock lock = new ReentrantLock(true);
    private final AtomicInteger interactiveWaiting = new AtomicInteger();

    /** Para pedidos de un usuario que está mirando la pantalla. */
    public <T> T interactive(Supplier<T> call) {
        interactiveWaiting.incrementAndGet();
        lock.lock();
        interactiveWaiting.decrementAndGet();
        try {
            return call.get();
        } finally {
            lock.unlock();
        }
    }

    /** Para trabajos de fondo: ceden el turno a cualquier pedido interactivo. */
    public <T> T background(Supplier<T> call) {
        while (true) {
            waitWhileInteractive();
            lock.lock();
            if (interactiveWaiting.get() == 0) {
                break;
            }
            lock.unlock();   // llegó uno interactivo mientras esperábamos: le dejamos pasar
        }
        try {
            return call.get();
        } finally {
            lock.unlock();
        }
    }

    public boolean interactiveWaiting() {
        return interactiveWaiting.get() > 0;
    }

    private void waitWhileInteractive() {
        while (interactiveWaiting.get() > 0) {
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return;
            }
        }
    }
}
