package com.nutriplan.recipe.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.nutriplan.common.error.ResourceNotFoundException;
import com.nutriplan.recipe.application.usecase.AiWorkload;
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

    private final RecipeDraftService service = new RecipeDraftService(new TemplateRecipeDraftGenerator(), new AiWorkload());
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
    void otherUsersCannotReadMyDraft() {
        var job = service.start("alice", request);
        assertThatThrownBy(() -> service.get("mallory", job.id())).isInstanceOf(ResourceNotFoundException.class);
    }
}
