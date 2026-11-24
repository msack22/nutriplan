package com.nutriplan.planner.application.usecase;

import com.nutriplan.planner.application.dto.ShoppingListDto;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ShoppingListService {

    private final PlanService plans;
    private final ShoppingListCalculator calculator;

    public ShoppingListService(PlanService plans, ShoppingListCalculator calculator) {
        this.plans = plans;
        this.calculator = calculator;
    }

    /** Primero se verifica que el plan sea del usuario (anti-IDOR), DESPUÉS se usa la caché. */
    @Transactional(readOnly = true)
    public ShoppingListDto forPlan(String userId, long planId) {
        return calculator.calculate(plans.owned(userId, planId));
    }
}
