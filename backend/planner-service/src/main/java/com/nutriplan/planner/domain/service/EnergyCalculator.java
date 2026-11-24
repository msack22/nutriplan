package com.nutriplan.planner.domain.service;

import com.nutriplan.planner.domain.model.Sex;
import com.nutriplan.planner.domain.model.UserProfile;
import java.time.LocalDate;

/**
 * Cálculos de energía basados en evidencia (servicio de dominio puro, 100% testeable):
 * <ul>
 *   <li><b>Mifflin-St Jeor</b> (la fórmula de BMR más precisa en adultos):
 *       10·peso + 6,25·altura − 5·edad + 5 (hombres) / −161 (mujeres)</li>
 *   <li><b>TDEE</b> = BMR × factor de actividad</li>
 *   <li><b>Objetivo</b> = TDEE + déficit, con un piso de seguridad (1200 kcal mujeres / 1500 hombres)</li>
 *   <li><b>Proteínas</b> 1,6 g/kg: preserva masa muscular en déficit y da saciedad</li>
 *   <li><b>Grasas</b> 30% de las kcal, <b>carbohidratos</b> el resto</li>
 * </ul>
 * ⚠️ Orientativo: no reemplaza a un profesional de la salud.
 */
public final class EnergyCalculator {

    public static final double KCAL_PER_KG_FAT = 7700;

    private EnergyCalculator() {
    }

    public static double bmr(Sex sex, double weightKg, double heightCm, int age) {
        double base = 10 * weightKg + 6.25 * heightCm - 5 * age;
        return sex == Sex.MALE ? base + 5 : base - 161;
    }

    public static NutritionTargets targetsFor(UserProfile p, LocalDate today) {
        if (!p.isComplete()) {
            throw new IllegalStateException("Completá sexo, fecha de nacimiento, altura y peso en tu perfil");
        }
        double weight = p.getCurrentWeightKg();
        double bmr = bmr(p.getSex(), weight, p.getHeightCm(), p.ageAt(today));
        double tdee = bmr * p.getActivityLevel().factor();
        int floor = p.getSex() == Sex.MALE ? 1500 : 1200;
        double daily = Math.max(floor, tdee + p.getGoal().dailyKcalDelta());

        int protein = (int) Math.round(1.6 * weight);
        int fat = (int) Math.round(daily * 0.30 / 9);
        int carbs = (int) Math.max(0, Math.round((daily - protein * 4 - fat * 9) / 4));

        double realDeficit = tdee - daily;
        Double weeklyLoss = realDeficit > 0 ? round1(realDeficit * 7 / KCAL_PER_KG_FAT) : null;
        Integer weeks = null;
        if (weeklyLoss != null && p.getTargetWeightKg() != null && p.getTargetWeightKg() < weight) {
            weeks = (int) Math.ceil((weight - p.getTargetWeightKg()) / weeklyLoss);
        }
        return new NutritionTargets((int) Math.round(bmr), (int) Math.round(tdee), (int) Math.round(daily),
                protein, carbs, fat, weeklyLoss, weeks);
    }

    private static double round1(double v) {
        return Math.round(v * 100) / 100.0;
    }
}
