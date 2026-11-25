package com.nutriplan.recipe.application.usecase;

import com.nutriplan.common.error.ResourceNotFoundException;
import com.nutriplan.recipe.domain.port.RecipeDraftGenerator;
import com.nutriplan.recipe.domain.port.RecipeDraftGenerator.DraftRequest;
import com.nutriplan.recipe.domain.port.RecipeDraftGenerator.RecipeDraft;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * Borradores de recetas con IA como <b>trabajo asíncrono</b>: generar tarda minutos en CPU, así que
 * {@code start()} devuelve enseguida un id (HTTP 202) y el frontend consulta el estado (polling).
 *
 * <ul>
 *   <li>Corre en un <b>virtual thread</b> (Java 21): no bloquea hilos de Tomcat.</li>
 *   <li>{@link LlmGate}: un solo pedido al LLM a la vez y los borradores tienen prioridad.</li>
 *   <li>Un usuario no puede lanzar otro mientras tiene uno en curso (evita abuso).</li>
 *   <li>Los trabajos viven en memoria 1 h (son descartables: el borrador no se guarda).</li>
 * </ul>
 */
@Service
public class RecipeDraftService {

    private static final Logger log = LoggerFactory.getLogger(RecipeDraftService.class);
    private static final Duration TTL = Duration.ofHours(1);

    public enum Status { PENDING, DONE, FAILED }

    public record DraftJob(String id, String userId, Status status, RecipeDraft draft, String error,
                           Instant createdAt, Instant finishedAt) {
    }

    private final RecipeDraftGenerator generator;
    private final LlmGate gate;
    private final Map<String, DraftJob> jobs = new ConcurrentHashMap<>();

    public RecipeDraftService(RecipeDraftGenerator generator, LlmGate gate) {
        this.generator = generator;
        this.gate = gate;
    }

    public DraftJob start(String userId, DraftRequest request) {
        purgeOld();
        var running = jobs.values().stream()
                .filter(j -> j.userId().equals(userId) && j.status() == Status.PENDING).findFirst();
        if (running.isPresent()) {
            return running.get();
        }
        DraftJob job = new DraftJob(UUID.randomUUID().toString(), userId, Status.PENDING, null, null, Instant.now(), null);
        jobs.put(job.id(), job);
        Thread.ofVirtual().name("recipe-draft-", 0).start(() -> run(job, request));
        return job;
    }

    public DraftJob get(String userId, String jobId) {
        DraftJob job = jobs.get(jobId);
        if (job == null || !job.userId().equals(userId)) {
            throw new ResourceNotFoundException("Borrador", jobId);
        }
        return job;
    }

    private void run(DraftJob job, DraftRequest request) {
        try {
            RecipeDraft draft = gate.interactive(() -> generator.generate(request));
            jobs.put(job.id(), new DraftJob(job.id(), job.userId(), Status.DONE, draft, null, job.createdAt(), Instant.now()));
            log.info("Borrador {} generado en {} s", job.id(), Duration.between(job.createdAt(), Instant.now()).toSeconds());
        } catch (RuntimeException e) {
            log.warn("Falló el borrador {}: {}", job.id(), e.getMessage());
            jobs.put(job.id(), new DraftJob(job.id(), job.userId(), Status.FAILED, null,
                    "No se pudo generar la receta. Probá de nuevo o cargala a mano.", job.createdAt(), Instant.now()));
        }
    }

    private void purgeOld() {
        Instant limit = Instant.now().minus(TTL);
        jobs.values().removeIf(j -> j.createdAt().isBefore(limit) && j.status() != Status.PENDING);
    }
}
