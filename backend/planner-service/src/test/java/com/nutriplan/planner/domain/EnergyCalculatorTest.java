package com.nutriplan.planner.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.nutriplan.planner.domain.model.ActivityLevel;
import com.nutriplan.planner.domain.model.MealType;
import com.nutriplan.planner.domain.model.Sex;
import com.nutriplan.planner.domain.model.UserProfile;
import com.nutriplan.planner.domain.model.WeightGoal;
import com.nutriplan.planner.domain.service.EnergyCalculator;
import com.nutriplan.planner.domain.service.NutritionTargets;
import java.time.LocalDate;
import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class EnergyCalculatorTest {

    private static final LocalDate TODAY = LocalDate.of(2026, 11, 24);

    @Test
    @DisplayName("Mifflin-St Jeor: hombre 90kg, 180cm, 35 años = 1855 kcal")
    void bmrMale() {
        assertThat(EnergyCalculator.bmr(Sex.MALE, 90, 180, 35)).isEqualTo(1855.0);
    }

    @Test
    void bmrFemaleSubtracts161() {
        assertThat(EnergyCalculator.bmr(Sex.FEMALE, 70, 165, 30)).isEqualTo(1420.25);
    }

    @Test
    void targetsApplyDeficitAndProteinPerKg() {
        UserProfile p = profile(Sex.MALE, 90.0, 80.0, ActivityLevel.LIGHT, WeightGoal.LOSE);

        NutritionTargets t = EnergyCalculator.targetsFor(p, TODAY);

        assertThat(t.bmr()).isEqualTo(1855);
        assertThat(t.tdee()).isEqualTo(2551);            // 1855 * 1.375
        assertThat(t.dailyKcal()).isEqualTo(2051);       // -500
        assertThat(t.proteinG()).isEqualTo(144);         // 1.6 g/kg
        assertThat(t.weeklyLossKg()).isEqualTo(0.45);    // 500*7/7700
        assertThat(t.weeksToGoal()).isEqualTo(23);       // 10 kg / 0.45
    }

    @Test
    void neverGoesBelowSafetyFloor() {
        UserProfile p = profile(Sex.FEMALE, 50.0, 45.0, ActivityLevel.SEDENTARY, WeightGoal.LOSE_FAST);

        assertThat(EnergyCalculator.targetsFor(p, TODAY).dailyKcal()).isEqualTo(1200);
    }

    @Test
    void requiresCompleteProfile() {
        assertThatThrownBy(() -> EnergyCalculator.targetsFor(new UserProfile("u", "x"), TODAY))
                .isInstanceOf(IllegalStateException.class);
    }

    static UserProfile profile(Sex sex, double weight, double target, ActivityLevel level, WeightGoal goal) {
        UserProfile p = new UserProfile("user-1", "Test");
        p.updateBody(sex, TODAY.minusYears(35), sex == Sex.MALE ? 180.0 : 160.0, weight, target, level, goal);
        p.updatePreferences(1, false, Set.of(MealType.LUNCH, MealType.DINNER), Set.of());
        return p;
    }
}
