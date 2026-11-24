package com.nutriplan.planner.web;

import com.nutriplan.planner.application.dto.AssignMealRequest;
import com.nutriplan.planner.application.dto.GeneratePlanRequest;
import com.nutriplan.planner.application.dto.PlanDto;
import com.nutriplan.planner.application.dto.ShoppingListDto;
import com.nutriplan.planner.application.dto.StrategyDto;
import com.nutriplan.planner.application.usecase.PlanService;
import com.nutriplan.planner.application.usecase.ShoppingListService;
import com.nutriplan.planner.application.usecase.StatsService;
import com.nutriplan.planner.domain.model.MealType;
import com.nutriplan.planner.domain.service.WeeklyStats;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.List;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/plans")
@Validated
@Tag(name = "Planes semanales", description = "Armado del plan, generación automática, stats y compras")
public class PlanController {

    private final PlanService plans;
    private final StatsService stats;
    private final ShoppingListService shopping;

    public PlanController(PlanService plans, StatsService stats, ShoppingListService shopping) {
        this.plans = plans;
        this.stats = stats;
        this.shopping = shopping;
    }

    @GetMapping("/week")
    @Operation(summary = "Plan de la semana que contiene 'date' (lo crea vacío si no existe)")
    public PlanDto week(@AuthenticationPrincipal Jwt jwt,
                        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return plans.getWeek(CurrentJwt.userId(jwt), CurrentJwt.name(jwt), date);
    }

    @GetMapping("/{id}")
    public PlanDto get(@AuthenticationPrincipal Jwt jwt, @PathVariable long id) {
        return plans.get(CurrentJwt.userId(jwt), id);
    }

    @PutMapping("/{id}/meals/{day}/{meal}")
    @Operation(summary = "Asigna una receta a un día y momento (reemplaza si ya había)")
    public PlanDto assign(@AuthenticationPrincipal Jwt jwt, @PathVariable long id, @PathVariable DayOfWeek day,
                          @PathVariable MealType meal, @RequestBody @Valid AssignMealRequest request) {
        return plans.assignMeal(CurrentJwt.userId(jwt), id, day, meal, request);
    }

    @DeleteMapping("/{id}/meals/{day}/{meal}")
    public PlanDto remove(@AuthenticationPrincipal Jwt jwt, @PathVariable long id, @PathVariable DayOfWeek day,
                          @PathVariable MealType meal) {
        return plans.removeMeal(CurrentJwt.userId(jwt), id, day, meal);
    }

    @PatchMapping("/{id}/meals/{day}/{meal}/eaten")
    @Operation(summary = "Marca una comida como consumida (seguimiento de adherencia)")
    public PlanDto eaten(@AuthenticationPrincipal Jwt jwt, @PathVariable long id, @PathVariable DayOfWeek day,
                         @PathVariable MealType meal, @RequestParam boolean value) {
        return plans.markEaten(CurrentJwt.userId(jwt), id, day, meal, value);
    }

    @PatchMapping("/{id}/people")
    @Operation(summary = "Cambia para cuántas personas se cocina (re-escala porciones)")
    public PlanDto people(@AuthenticationPrincipal Jwt jwt, @PathVariable long id,
                          @RequestParam @Min(1) @Max(12) int value) {
        return plans.changePeople(CurrentJwt.userId(jwt), id, value);
    }

    @PostMapping("/{id}/clear")
    public PlanDto clear(@AuthenticationPrincipal Jwt jwt, @PathVariable long id) {
        return plans.clear(CurrentJwt.userId(jwt), id);
    }

    @PostMapping("/{id}/copy-previous")
    @Operation(summary = "Copia las comidas de la semana anterior")
    public PlanDto copyPrevious(@AuthenticationPrincipal Jwt jwt, @PathVariable long id) {
        return plans.copyPreviousWeek(CurrentJwt.userId(jwt), id);
    }

    @PostMapping("/generate")
    @Operation(summary = "Genera automáticamente el plan de la semana (Strategy + búsqueda semántica)")
    public PlanDto generate(@AuthenticationPrincipal Jwt jwt,
                            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
                            @RequestBody @Valid GeneratePlanRequest request) {
        return plans.generate(CurrentJwt.userId(jwt), CurrentJwt.name(jwt), date, request);
    }

    @GetMapping("/strategies")
    @Operation(summary = "Estrategias de generación disponibles")
    public List<StrategyDto> strategies() {
        return plans.strategies();
    }

    @GetMapping("/{id}/stats")
    @Operation(summary = "Estadísticas nutricionales de la semana (por persona) vs. objetivo")
    public WeeklyStats stats(@AuthenticationPrincipal Jwt jwt, @PathVariable long id) {
        return stats.weekly(CurrentJwt.userId(jwt), CurrentJwt.name(jwt), id);
    }

    @GetMapping("/{id}/shopping-list")
    @Operation(summary = "Lista de compras agregada por góndola + estimación de precios")
    public ShoppingListDto shoppingList(@AuthenticationPrincipal Jwt jwt, @PathVariable long id) {
        return shopping.forPlan(CurrentJwt.userId(jwt), id);
    }
}
