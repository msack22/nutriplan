package com.nutriplan.planner.application.usecase;

import com.nutriplan.common.error.BusinessRuleException;
import com.nutriplan.common.error.ResourceNotFoundException;
import com.nutriplan.common.events.PlanChangedEvent;
import com.nutriplan.common.events.Topics;
import com.nutriplan.planner.application.dto.AssignMealRequest;
import com.nutriplan.planner.application.dto.GeneratePlanRequest;
import com.nutriplan.planner.application.dto.PlanDto;
import com.nutriplan.planner.application.dto.StrategyDto;
import com.nutriplan.planner.domain.generation.GenerationRequest;
import com.nutriplan.planner.domain.generation.MealAssignment;
import com.nutriplan.planner.domain.generation.PlanGeneratorRegistry;
import com.nutriplan.planner.domain.generation.RecipeCandidate;
import com.nutriplan.planner.domain.model.MealType;
import com.nutriplan.planner.domain.model.PlannedMeal;
import com.nutriplan.planner.domain.model.UserProfile;
import com.nutriplan.planner.domain.model.WeeklyPlan;
import com.nutriplan.planner.domain.port.IntegrationEventPublisher;
import com.nutriplan.planner.domain.port.RecipeCatalogPort;
import com.nutriplan.planner.domain.port.RecipeCatalogPort.CandidateQuery;
import com.nutriplan.planner.domain.repository.WeeklyPlanRepository;
import com.nutriplan.planner.domain.service.NutritionTargets;
import java.time.Clock;
import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Casos de uso del plan semanal.
 *
 * <p><b>Autorización a nivel de objeto (anti-IDOR)</b>: además de estar autenticado, cada
 * operación verifica que el plan pertenezca al usuario del token. Si no, respondemos 404 (no 403)
 * para no revelar que ese id existe. Es el riesgo #1 del OWASP API Top 10 (BOLA).
 */
@Service
@Transactional
public class PlanService {

    private final WeeklyPlanRepository plans;
    private final ProfileService profiles;
    private final RecipeCatalogPort catalog;
    private final PlanGeneratorRegistry generators;
    private final IntegrationEventPublisher events;
    private final Clock clock;

    public PlanService(WeeklyPlanRepository plans, ProfileService profiles, RecipeCatalogPort catalog,
                       PlanGeneratorRegistry generators, IntegrationEventPublisher events, Clock clock) {
        this.plans = plans;
        this.profiles = profiles;
        this.catalog = catalog;
        this.generators = generators;
        this.events = events;
        this.clock = clock;
    }

    /** Devuelve (o crea vacío) el plan de la semana que contiene {@code anyDate}. */
    public PlanDto getWeek(String userId, String name, LocalDate anyDate) {
        return PlanDto.from(weekPlan(userId, name, anyDate == null ? LocalDate.now(clock) : anyDate));
    }

    @Transactional(readOnly = true)
    public PlanDto get(String userId, long planId) {
        return PlanDto.from(owned(userId, planId));
    }

    public PlanDto assignMeal(String userId, long planId, DayOfWeek day, MealType meal, AssignMealRequest req) {
        WeeklyPlan plan = owned(userId, planId);
        plan.assign(day, meal, catalog.getRecipe(req.recipeId()), req.servings());
        return changed(plan, "ASSIGN " + day + " " + meal);
    }

    public PlanDto removeMeal(String userId, long planId, DayOfWeek day, MealType meal) {
        WeeklyPlan plan = owned(userId, planId);
        plan.remove(day, meal);
        return changed(plan, "REMOVE " + day + " " + meal);
    }

    public PlanDto markEaten(String userId, long planId, DayOfWeek day, MealType meal, boolean eaten) {
        WeeklyPlan plan = owned(userId, planId);
        plan.markEaten(day, meal, eaten);
        return changed(plan, "EATEN " + day + " " + meal + "=" + eaten);
    }

    public PlanDto changePeople(String userId, long planId, int people) {
        WeeklyPlan plan = owned(userId, planId);
        plan.changePeople(people);
        return changed(plan, "PEOPLE " + people);
    }

    public PlanDto clear(String userId, long planId) {
        WeeklyPlan plan = owned(userId, planId);
        plan.clear();
        return changed(plan, "CLEAR");
    }

    /** Copia las comidas de la semana anterior ("repetir semana"). */
    public PlanDto copyPreviousWeek(String userId, long planId) {
        WeeklyPlan plan = owned(userId, planId);
        WeeklyPlan previous = plans.findByUserIdAndWeekStart(userId, plan.getWeekStart().minusWeeks(1))
                .orElseThrow(() -> new BusinessRuleException("No hay un plan para la semana anterior"));
        plan.clear();
        for (PlannedMeal m : previous.getMeals()) {
            plan.assign(m.getDay(), m.getMealType(),
                    new com.nutriplan.planner.domain.model.RecipeRef(m.getRecipeId(), m.getRecipeTitle(),
                            m.getNutritionPerServing()),
                    m.getServings() == previous.getPeople() ? null : m.getServings());
        }
        return changed(plan, "COPY_PREVIOUS");
    }

    /**
     * Genera un plan automático: pide candidatas a recipe-service (filtradas y rankeadas por
     * búsqueda semántica) y delega la elección en la estrategia elegida.
     */
    public PlanDto generate(String userId, String name, LocalDate anyDate, GeneratePlanRequest req) {
        UserProfile profile = profiles.getOrCreate(userId, name);
        WeeklyPlan plan = weekPlan(userId, name, anyDate == null ? LocalDate.now(clock) : anyDate);
        if (req.people() != null) {
            plan.changePeople(req.people());
        }
        Set<MealType> meals = req.meals() == null || req.meals().isEmpty()
                ? EnumSet.copyOf(profile.getEnabledMeals()) : EnumSet.copyOf(req.meals());
        Set<DayOfWeek> days = req.days() == null || req.days().isEmpty()
                ? EnumSet.allOf(DayOfWeek.class) : EnumSet.copyOf(req.days());
        NutritionTargets targets = profiles.targetsOrNull(profile);
        double kcal = req.dailyKcal() != null ? req.dailyKcal() : targets != null ? targets.dailyKcal() : 1800;
        double protein = targets != null ? targets.proteinG() : 100;
        boolean vegetarian = req.vegetarian() != null ? req.vegetarian() : profile.isVegetarian();

        List<RecipeCandidate> candidates = catalog.findCandidates(new CandidateQuery(meals, vegetarian,
                Set.copyOf(profile.getExcludedIngredients()), req.preference()));
        Map<MealType, List<RecipeCandidate>> byMeal = new EnumMap<>(MealType.class);
        for (MealType m : meals) {
            byMeal.put(m, candidates.stream().filter(c -> c.mealTypes().contains(m)).toList());
        }

        long seed = req.seed() != null ? req.seed() : System.nanoTime();
        int maxRepeats = req.maxRepeatsPerWeek() != null ? req.maxRepeatsPerWeek() : 2;
        List<MealAssignment> assignments = generators.get(req.strategy())
                .generate(new GenerationRequest(days, meals, kcal, protein, maxRepeats, seed), byMeal);

        if (!req.keepExistingMeals()) {
            for (DayOfWeek d : days) {
                for (MealType m : meals) {
                    plan.remove(d, m);
                }
            }
        }
        for (MealAssignment a : assignments) {
            if (req.keepExistingMeals() && plan.mealAt(a.day(), a.mealType()).isPresent()) {
                continue;
            }
            plan.assign(a.day(), a.mealType(), a.recipe().toRef(), null);
        }
        return changed(plan, "GENERATE " + req.strategy());
    }

    @Transactional(readOnly = true)
    public List<StrategyDto> strategies() {
        return generators.all().stream().map(s -> new StrategyDto(s.name(), s.description())).toList();
    }

    // ------------------------------------------------------------------ helpers

    WeeklyPlan owned(String userId, long planId) {
        return plans.findWithMealsById(planId)
                .filter(p -> p.belongsTo(userId))
                .orElseThrow(() -> new ResourceNotFoundException("Plan", planId));
    }

    private WeeklyPlan weekPlan(String userId, String name, LocalDate anyDate) {
        LocalDate monday = anyDate.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        return plans.findByUserIdAndWeekStart(userId, monday).orElseGet(() -> {
            int people = profiles.getOrCreate(userId, name).getDefaultPeople();
            return plans.save(new WeeklyPlan(userId, monday, people));
        });
    }

    private PlanDto changed(WeeklyPlan plan, String change) {
        plans.saveAndFlush(plan);   // flush => incrementa @Version antes de armar el DTO
        events.publish(Topics.PLAN_CHANGED, plan.getUserId(), new PlanChangedEvent(UUID.randomUUID().toString(),
                Instant.now(clock), plan.getId(), plan.getUserId(), plan.getWeekStart(), change));
        return PlanDto.from(plan);
    }
}
