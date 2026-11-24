package com.nutriplan.planner.application.usecase;

import com.nutriplan.planner.application.dto.ProgressDto;
import com.nutriplan.planner.application.dto.WeightDto;
import com.nutriplan.planner.domain.model.PlannedMeal;
import com.nutriplan.planner.domain.model.UserProfile;
import com.nutriplan.planner.domain.model.WeeklyPlan;
import com.nutriplan.planner.domain.model.WeightEntry;
import com.nutriplan.planner.domain.repository.WeeklyPlanRepository;
import com.nutriplan.planner.domain.repository.WeightEntryRepository;
import com.nutriplan.planner.domain.service.NutritionTargets;
import com.nutriplan.planner.domain.service.WeeklyStats;
import com.nutriplan.planner.domain.service.WeeklyStatsCalculator;
import java.time.Clock;
import java.time.LocalDate;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Estadísticas semanales y progreso histórico. */
@Service
@Transactional
public class StatsService {

    private final PlanService planService;
    private final ProfileService profiles;
    private final WeeklyPlanRepository plans;
    private final WeightEntryRepository weights;
    private final Clock clock;

    public StatsService(PlanService planService, ProfileService profiles, WeeklyPlanRepository plans,
                        WeightEntryRepository weights, Clock clock) {
        this.planService = planService;
        this.profiles = profiles;
        this.plans = plans;
        this.weights = weights;
        this.clock = clock;
    }

    public WeeklyStats weekly(String userId, String name, long planId) {
        WeeklyPlan plan = planService.owned(userId, planId);
        UserProfile profile = profiles.getOrCreate(userId, name);
        return WeeklyStatsCalculator.calculate(plan, profiles.targetsOrNull(profile));
    }

    public ProgressDto progress(String userId, String name, int weeksBack) {
        UserProfile profile = profiles.getOrCreate(userId, name);
        NutritionTargets targets = profiles.targetsOrNull(profile);
        LocalDate to = LocalDate.now(clock);
        List<ProgressDto.WeekSummary> weeks = plans
                .findByUserIdAndWeekStartBetweenOrderByWeekStartAsc(userId, to.minusWeeks(weeksBack), to)
                .stream()
                .map(p -> {
                    WeeklyStats s = WeeklyStatsCalculator.calculate(p, targets);
                    int eaten = (int) p.getMeals().stream().filter(PlannedMeal::isEaten).count();
                    return new ProgressDto.WeekSummary(p.getWeekStart(), s.dailyAverage().kcal(),
                            s.dailyAverage().proteinG(), s.kcalAdherencePct(), s.mealsPlanned(), eaten);
                })
                .toList();
        List<WeightEntry> entries = weights.findByUserIdOrderByDateAsc(userId);
        List<WeightDto> weightDtos = entries.stream()
                .map(w -> new WeightDto(w.getDate(), w.getWeightKg(), w.getNote())).toList();
        Double start = entries.isEmpty() ? profile.getCurrentWeightKg() : entries.getFirst().getWeightKg();
        Double current = profile.getCurrentWeightKg();
        Double lost = start != null && current != null ? Math.round((start - current) * 10) / 10.0 : null;
        return new ProgressDto(weightDtos, weeks, targets, start, current, profile.getTargetWeightKg(), lost);
    }

    /** Registra el peso del día (upsert) y actualiza el peso actual del perfil. */
    public List<WeightDto> logWeight(String userId, String name, WeightDto dto) {
        UserProfile profile = profiles.getOrCreate(userId, name);
        LocalDate date = dto.date() == null ? LocalDate.now(clock) : dto.date();
        weights.findByUserIdAndDate(userId, date).ifPresentOrElse(
                w -> w.update(dto.weightKg(), dto.note()),
                () -> weights.save(new WeightEntry(userId, date, dto.weightKg(), dto.note())));
        List<WeightEntry> all = weights.findByUserIdOrderByDateAsc(userId);
        if (!all.isEmpty() && !all.getLast().getDate().isAfter(date)) {
            profile.recordWeight(dto.weightKg());
        }
        return all.stream().map(w -> new WeightDto(w.getDate(), w.getWeightKg(), w.getNote())).toList();
    }
}
