package com.nutriplan.planner.domain.model;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.LocalDate;
import java.time.Period;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.Set;

/**
 * Perfil nutricional del usuario. La PK es el {@code sub} del JWT de Keycloak: el usuario
 * "vive" en Keycloak (identidad) y acá solo guardamos datos del dominio (no hay tabla users).
 */
@Entity
@Table(name = "user_profile")
public class UserProfile {

    @Id
    @Column(name = "user_id", length = 64)
    private String userId;

    @Column(name = "display_name", length = 120)
    private String displayName;

    @Enumerated(EnumType.STRING)
    @Column(length = 10)
    private Sex sex;

    @Column(name = "birth_date")
    private LocalDate birthDate;

    @Column(name = "height_cm")
    private Double heightCm;

    @Column(name = "current_weight_kg")
    private Double currentWeightKg;

    @Column(name = "target_weight_kg")
    private Double targetWeightKg;

    @Enumerated(EnumType.STRING)
    @Column(name = "activity_level", length = 20)
    private ActivityLevel activityLevel = ActivityLevel.LIGHT;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private WeightGoal goal = WeightGoal.LOSE;

    @Column(name = "default_people")
    private int defaultPeople = 1;

    private boolean vegetarian;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "user_profile_meal", joinColumns = @JoinColumn(name = "user_id"))
    @Enumerated(EnumType.STRING)
    @Column(name = "meal_type", length = 20)
    private Set<MealType> enabledMeals = EnumSet.allOf(MealType.class);

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "user_profile_excluded", joinColumns = @JoinColumn(name = "user_id"))
    @Column(name = "ingredient", length = 100)
    private Set<String> excludedIngredients = new HashSet<>();

    @Version
    private long version;

    protected UserProfile() {
    }

    public UserProfile(String userId, String displayName) {
        this.userId = userId;
        this.displayName = displayName;
    }

    public void updateBody(Sex sex, LocalDate birthDate, Double heightCm, Double currentWeightKg,
                           Double targetWeightKg, ActivityLevel activityLevel, WeightGoal goal) {
        if (heightCm != null && (heightCm < 100 || heightCm > 250)) {
            throw new IllegalArgumentException("Altura fuera de rango (100-250 cm)");
        }
        this.sex = sex;
        this.birthDate = birthDate;
        this.heightCm = heightCm;
        if (currentWeightKg != null) {
            recordWeight(currentWeightKg);
        }
        this.targetWeightKg = targetWeightKg;
        this.activityLevel = activityLevel == null ? ActivityLevel.LIGHT : activityLevel;
        this.goal = goal == null ? WeightGoal.LOSE : goal;
    }

    public void updatePreferences(int defaultPeople, boolean vegetarian, Set<MealType> meals, Set<String> excluded) {
        if (defaultPeople < 1 || defaultPeople > 12) {
            throw new IllegalArgumentException("Las personas deben estar entre 1 y 12");
        }
        if (meals == null || meals.isEmpty()) {
            throw new IllegalArgumentException("Elegí al menos una comida del día");
        }
        this.defaultPeople = defaultPeople;
        this.vegetarian = vegetarian;
        Set<MealType> mealsCopy = Set.copyOf(meals);
        this.enabledMeals.clear();
        this.enabledMeals.addAll(mealsCopy);
        Set<String> excludedCopy = excluded == null ? Set.of() : Set.copyOf(excluded);
        this.excludedIngredients.clear();
        excludedCopy.stream().map(String::strip).filter(s -> !s.isEmpty()).map(String::toLowerCase)
                .forEach(this.excludedIngredients::add);
    }

    public void recordWeight(double kg) {
        if (kg < 30 || kg > 350) {
            throw new IllegalArgumentException("Peso fuera de rango (30-350 kg)");
        }
        this.currentWeightKg = kg;
    }

    public boolean isComplete() {
        return sex != null && birthDate != null && heightCm != null && currentWeightKg != null;
    }

    public int ageAt(LocalDate date) {
        return Period.between(birthDate, date).getYears();
    }

    public String getUserId() {
        return userId;
    }

    public String getDisplayName() {
        return displayName;
    }

    public Sex getSex() {
        return sex;
    }

    public LocalDate getBirthDate() {
        return birthDate;
    }

    public Double getHeightCm() {
        return heightCm;
    }

    public Double getCurrentWeightKg() {
        return currentWeightKg;
    }

    public Double getTargetWeightKg() {
        return targetWeightKg;
    }

    public ActivityLevel getActivityLevel() {
        return activityLevel;
    }

    public WeightGoal getGoal() {
        return goal;
    }

    public int getDefaultPeople() {
        return defaultPeople;
    }

    public boolean isVegetarian() {
        return vegetarian;
    }

    public Set<MealType> getEnabledMeals() {
        return Collections.unmodifiableSet(enabledMeals);
    }

    public Set<String> getExcludedIngredients() {
        return Collections.unmodifiableSet(excludedIngredients);
    }
}
