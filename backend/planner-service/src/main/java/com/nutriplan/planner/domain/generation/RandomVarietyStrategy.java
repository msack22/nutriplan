package com.nutriplan.planner.domain.generation;

import com.nutriplan.planner.domain.model.MealType;
import java.time.DayOfWeek;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

/** Estrategia simple: máxima variedad, ignora calorías ("sorprendeme"). */
public class RandomVarietyStrategy implements PlanGenerationStrategy {

    @Override
    public String name() {
        return "variety";
    }

    @Override
    public String description() {
        return "Máxima variedad, sin optimizar calorías";
    }

    @Override
    public List<MealAssignment> generate(GenerationRequest req, Map<MealType, List<RecipeCandidate>> byMeal) {
        Random random = new Random(req.seed());
        Map<MealType, List<RecipeCandidate>> pools = new HashMap<>();
        byMeal.forEach((meal, list) -> {
            List<RecipeCandidate> copy = new ArrayList<>(list);
            Collections.shuffle(copy, random);
            pools.put(meal, copy);
        });
        List<MealAssignment> result = new ArrayList<>();
        Map<MealType, Integer> cursor = new HashMap<>();
        for (DayOfWeek day : DayOfWeek.values()) {
            if (!req.days().contains(day)) {
                continue;
            }
            for (MealType meal : req.meals().stream().sorted().toList()) {
                List<RecipeCandidate> pool = pools.getOrDefault(meal, List.of());
                if (pool.isEmpty()) {
                    continue;
                }
                int i = cursor.merge(meal, 1, Integer::sum) - 1;
                result.add(new MealAssignment(day, meal, pool.get(i % pool.size())));
            }
        }
        return result;
    }
}
