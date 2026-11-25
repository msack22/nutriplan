package com.nutriplan.recipe.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.nutriplan.common.error.ResourceNotFoundException;
import com.nutriplan.recipe.application.usecase.LlmGate;
import com.nutriplan.recipe.application.usecase.RecipeDraftService;
import com.nutriplan.recipe.application.usecase.RecipeDraftService.Status;
import com.nutriplan.recipe.domain.model.MealType;
import com.nutriplan.recipe.domain.port.RecipeDraftGenerator.DraftRequest;
import com.nutriplan.recipe.infrastructure.ai.TemplateRecipeDraftGenerator;
import java.time.Duration;
import java.util.List;
import org.awaitility.Awaitility;
import org.junit.jupiter.api.Test;

class RecipeDraftServiceTest {

    private final RecipeDraftService service = new RecipeDraftService(new TemplateRecipeDraftGenerator(), new LlmGate());
    private final DraftRequest request = new DraftRequest(List.of("pollo", "zapallito"), MealType.DINNER, 2, null);

    @Test
    void generatesDraftAsynchronously() {
        var job = service.start("alice", request);
        assertThat(job.status()).isEqualTo(Status.PENDING);

        Awaitility.await().atMost(Duration.ofSeconds(5))
                .until(() -> service.get("alice", job.id()).status() == Status.DONE);

        var done = service.get("alice", job.id());
        assertThat(done.draft().title()).contains("pollo");
        assertThat(done.draft().ingredients()).hasSize(2);
    }

    @Test
    void interactiveCallsGoBeforeQueuedBackgroundWork() throws Exception {
        LlmGate gate = new LlmGate();
        java.util.List<String> order = java.util.Collections.synchronizedList(new java.util.ArrayList<>());
        java.util.concurrent.CountDownLatch busy = new java.util.concurrent.CountDownLatch(1);
        // 1) un trabajo de fondo ocupa el LLM
        Thread first = Thread.ofVirtual().start(() -> gate.background(() -> {
            busy.countDown();
            sleep(300);
            return order.add("fondo-1");
        }));
        busy.await();
        // 2) llegan otro de fondo y uno interactivo mientras está ocupado
        Thread second = Thread.ofVirtual().start(() -> gate.background(() -> order.add("fondo-2")));
        sleep(50);
        Thread interactive = Thread.ofVirtual().start(() -> gate.interactive(() -> order.add("usuario")));
        first.join();
        second.join();
        interactive.join();
        // el usuario no espera detrás de "fondo-2"
        assertThat(order).containsExactly("fondo-1", "usuario", "fondo-2");
    }

    private static void sleep(long ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    @Test
    void otherUsersCannotReadMyDraft() {
        var job = service.start("alice", request);
        assertThatThrownBy(() -> service.get("mallory", job.id())).isInstanceOf(ResourceNotFoundException.class);
    }
}
