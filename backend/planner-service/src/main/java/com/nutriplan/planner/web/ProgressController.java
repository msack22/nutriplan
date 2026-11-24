package com.nutriplan.planner.web;

import com.nutriplan.planner.application.dto.ProgressDto;
import com.nutriplan.planner.application.dto.WeightDto;
import com.nutriplan.planner.application.usecase.StatsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import java.util.List;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/progress")
@Validated
@Tag(name = "Progreso", description = "Registro de peso e historial de semanas")
public class ProgressController {

    private final StatsService stats;

    public ProgressController(StatsService stats) {
        this.stats = stats;
    }

    @GetMapping
    @Operation(summary = "Evolución de peso + resumen de las últimas N semanas")
    public ProgressDto progress(@AuthenticationPrincipal Jwt jwt,
                                @RequestParam(defaultValue = "12") @Min(1) @Max(104) int weeks) {
        return stats.progress(CurrentJwt.userId(jwt), CurrentJwt.name(jwt), weeks);
    }

    @PostMapping("/weights")
    @Operation(summary = "Registra el peso de un día (si ya existe, lo actualiza)")
    public List<WeightDto> logWeight(@AuthenticationPrincipal Jwt jwt, @RequestBody @Valid WeightDto weight) {
        return stats.logWeight(CurrentJwt.userId(jwt), CurrentJwt.name(jwt), weight);
    }
}
