package com.nutriplan.planner.web;

import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.nutriplan.common.error.ResourceNotFoundException;
import com.nutriplan.planner.application.dto.PlanDto;
import com.nutriplan.planner.application.usecase.PlanService;
import com.nutriplan.planner.application.usecase.ShoppingListService;
import com.nutriplan.planner.application.usecase.StatsService;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Test de "slice" web: levanta SOLO la capa MVC + seguridad (sin DB, sin Kafka).
 * {@code jwt()} de spring-security-test simula un token ya validado (no necesita Keycloak).
 */
@WebMvcTest(PlanController.class)
@TestPropertySource(properties = {
        "nutriplan.security.issuer=http://test/realms/nutriplan",
        "nutriplan.security.jwk-set-uri=http://test/certs"})
class PlanControllerSecurityTest {

    @Autowired
    MockMvc mvc;

    @MockitoBean
    PlanService plans;
    @MockitoBean
    StatsService stats;
    @MockitoBean
    ShoppingListService shopping;

    @Test
    void rejectsAnonymousRequestsWith401() throws Exception {
        mvc.perform(get("/api/plans/week")).andExpect(status().isUnauthorized());
    }

    @Test
    void returnsTheCallersOwnPlan() throws Exception {
        given(plans.getWeek(eq("alice"), eq(null), eq(null)))
                .willReturn(new PlanDto(10L, LocalDate.of(2026, 11, 23), 2, 0, List.of()));

        mvc.perform(get("/api/plans/week").with(jwt().jwt(j -> j.subject("alice"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(10))
                .andExpect(jsonPath("$.people").value(2));
    }

    @Test
    void anotherUsersPlanLooksLikeItDoesNotExist() throws Exception {
        given(plans.get(eq("mallory"), anyLong())).willThrow(new ResourceNotFoundException("Plan", 10));

        mvc.perform(get("/api/plans/10").with(jwt().jwt(j -> j.subject("mallory"))))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.title").value("Recurso no encontrado"));
    }
}
