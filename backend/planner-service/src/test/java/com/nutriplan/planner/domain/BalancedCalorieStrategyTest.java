package com.nutriplan.planner.domain;

import static org.assertj.core.api.Assertions.assertThat;

import com.nutriplan.planner.domain.generation.BalancedCalorieStrategy;
import com.nutriplan.planner.domain.generation.GenerationRequest;
import com.nutriplan.planner.domain.generation.MealAssignment;
import com.nutriplan.planner.domain.generation.RecipeCandidate;
import com.nutriplan.planner.domain.model.MealType;
import com.nutriplan.planner.domain.model.NutritionSnapshot;
import java.time.DayOfWeek;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import org.junit.jupiter.api.Test;

class BalancedCalorieStrategyTest {

    private final BalancedCalorieStrategy strategy = new BalancedCalorieStrategy();

    private static List<RecipeCandidate> candidates(MealType meal, int count, double baseKcal) {
        return IntStream.range(0, count)
                .mapToObj(i -> new RecipeCandidate(meal.ordinal() * 100L + i, meal + "-" + i, Set.of(meal),
                        new NutritionSnapshot(baseKcal + i * 50, 25.0, 30.0, 15.0), 0))
                .toList();
    }

    private final Map<MealType, List<RecipeCandidate>> pool = Map.of(
            MealType.LUNCH, candidates(MealType.LUNCH, 20, 300),     // 300..1250 kcal
            MealType.DINNER, candidates(MealType.DINNER, 20, 300));

    private GenerationRequest request(int maxRepeats, long seed) {
        return new GenerationRequest(EnumSet.allOf(DayOfWeek.class), EnumSet.of(MealType.LUNCH, MealType.DINNER),
                1500, 100, maxRepeats, seed);
    }

    @Test
    void fillsEveryRequestedSlot() {
        assertThat(strategy.generate(request(2, 42), pool)).hasSize(14);
    }

    @Test
    void respectsMaxRepeatsPerWeek() {
        Map<Long, Long> uses = strategy.generate(request(2, 42), pool).stream()
                .collect(Collectors.groupingBy(a -> a.recipe().id(), Collectors.counting()));
        assertThat(uses.values()).allMatch(n -> n <= 2);
    }

    @Test
    void sameSeedProducesSamePlan() {
        assertThat(strategy.generate(request(2, 7), pool)).isEqualTo(strategy.generate(request(2, 7), pool));
    }

    @Test
    void dailyCaloriesStayCloseToTarget() {
        List<MealAssignment> plan = strategy.generate(request(2, 1), pool);
        Map<DayOfWeek, Double> perDay = plan.stream().collect(Collectors.groupingBy(MealAssignment::day,
                Collectors.summingDouble(a -> a.recipe().nutrition().kcal())));
        // solo almuerzo+cena habilitados => entre los dos deben cubrir las 1500 kcal (±15%)
        assertThat(perDay.values()).allMatch(kcal -> Math.abs(kcal - 1500) <= 225);
    }
}
