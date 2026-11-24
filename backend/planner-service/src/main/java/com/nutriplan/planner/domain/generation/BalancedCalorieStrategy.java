package com.nutriplan.planner.domain.generation;

import com.nutriplan.planner.domain.model.MealType;
import java.time.DayOfWeek;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

/**
 * Algoritmo GREEDY (goloso) con presupuesto adaptativo:
 * <ol>
 *   <li>Para cada día, reparte las kcal objetivo entre las comidas elegidas según su peso
 *       (desayuno 25%, almuerzo 35%, merienda 10%, cena 30%, re-normalizado).</li>
 *   <li>Para cada comida elige la receta de menor "costo":
 *       error calórico + penalización por repetir + bonus por proteína y por preferencia
 *       semántica + un poco de aleatoriedad (seeded) para dar variedad.</li>
 *   <li>Lo que sobra/falta del presupuesto se traslada a las comidas restantes del día.</li>
 * </ol>
 * No garantiza el óptimo global (eso sería programación lineal entera), pero es rápido,
 * explicable y da muy buenos resultados en la práctica.
 */
public class BalancedCalorieStrategy implements PlanGenerationStrategy {

    @Override
    public String name() {
        return "balanced";
    }

    @Override
    public String description() {
        return "Ajusta las calorías al objetivo diario, prioriza proteína y variedad";
    }

    @Override
    public List<MealAssignment> generate(GenerationRequest req, Map<MealType, List<RecipeCandidate>> byMeal) {
        Random random = new Random(req.seed());
        Map<Long, Integer> uses = new HashMap<>();
        Map<MealType, Long> yesterday = new HashMap<>();
        List<MealAssignment> result = new ArrayList<>();
        List<MealType> meals = req.meals().stream().sorted().toList();

        for (DayOfWeek day : DayOfWeek.values()) {
            if (!req.days().contains(day)) {
                continue;
            }
            double remainingKcal = req.dailyKcalTarget();
            double remainingShare = meals.stream().mapToDouble(MealType::calorieShare).sum();
            Set<Long> today = new HashSet<>();

            for (MealType meal : meals) {
                double slotTarget = remainingKcal * meal.calorieShare() / remainingShare;
                RecipeCandidate best = null;
                double bestCost = Double.MAX_VALUE;
                for (RecipeCandidate c : byMeal.getOrDefault(meal, List.of())) {
                    int used = uses.getOrDefault(c.id(), 0);
                    if (used >= req.maxRepeatsPerWeek() || today.contains(c.id())) {
                        continue;
                    }
                    double cost = cost(c, slotTarget, used, c.id() == yesterday.getOrDefault(meal, -1L))
                            + random.nextDouble() * 0.15;
                    if (cost < bestCost) {
                        bestCost = cost;
                        best = c;
                    }
                }
                remainingShare -= meal.calorieShare();
                if (best == null) {
                    continue;   // no hay candidatas: el hueco queda libre
                }
                result.add(new MealAssignment(day, meal, best));
                uses.merge(best.id(), 1, Integer::sum);
                today.add(best.id());
                yesterday.put(meal, best.id());
                remainingKcal -= best.hasKcal() ? best.nutrition().kcal() : slotTarget;
                remainingKcal = Math.max(remainingKcal, 0);
            }
        }
        return result;
    }

    static double cost(RecipeCandidate c, double slotTarget, int timesUsed, boolean sameAsYesterday) {
        double calorieError = c.hasKcal() && slotTarget > 0
                ? Math.abs(c.nutrition().kcal() - slotTarget) / slotTarget
                : 0.6;    // sin datos: se permite pero con desventaja
        double proteinBonus = 0;
        if (c.hasKcal() && c.nutrition().proteinG() != null) {
            double proteinCalorieRatio = c.nutrition().proteinG() * 4 / c.nutrition().kcal();
            proteinBonus = Math.min(proteinCalorieRatio, 0.5) * 0.4;
        }
        return calorieError
                + timesUsed * 0.6
                + (sameAsYesterday ? 0.3 : 0)
                - proteinBonus
                - c.preferenceScore() * 0.5;
    }
}
