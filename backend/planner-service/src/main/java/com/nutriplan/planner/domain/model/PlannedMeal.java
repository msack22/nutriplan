package com.nutriplan.planner.domain.model;

import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.DayOfWeek;

/** Una comida dentro del plan: (día, momento) -> receta + porciones. */
@Entity
@Table(name = "planned_meal")
public class PlannedMeal {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "plan_id")
    private WeeklyPlan plan;

    @Enumerated(EnumType.STRING)
    @Column(name = "day_of_week", nullable = false, length = 10)
    private DayOfWeek day;

    @Enumerated(EnumType.STRING)
    @Column(name = "meal_type", nullable = false, length = 20)
    private MealType mealType;

    @Column(name = "recipe_id", nullable = false)
    private long recipeId;

    @Column(name = "recipe_title", nullable = false)
    private String recipeTitle;

    /** Porciones a cocinar (normalmente = personas del plan). */
    @Column(nullable = false)
    private int servings;

    @Embedded
    private NutritionSnapshot nutritionPerServing;

    private boolean eaten;

    protected PlannedMeal() {
    }

    PlannedMeal(WeeklyPlan plan, DayOfWeek day, MealType mealType, RecipeRef recipe, int servings) {
        this.plan = plan;
        this.day = day;
        this.mealType = mealType;
        change(recipe, servings);
    }

    void change(RecipeRef recipe, int servings) {
        this.recipeId = recipe.id();
        this.recipeTitle = recipe.title();
        this.nutritionPerServing = recipe.nutritionPerServing();
        this.servings = servings;
        this.eaten = false;
    }

    void markEaten(boolean eaten) {
        this.eaten = eaten;
    }

    public boolean occupies(DayOfWeek d, MealType m) {
        return day == d && mealType == m;
    }

    public Long getId() {
        return id;
    }

    public DayOfWeek getDay() {
        return day;
    }

    public MealType getMealType() {
        return mealType;
    }

    public long getRecipeId() {
        return recipeId;
    }

    public String getRecipeTitle() {
        return recipeTitle;
    }

    public int getServings() {
        return servings;
    }

    public NutritionSnapshot getNutritionPerServing() {
        return nutritionPerServing;
    }

    public boolean isEaten() {
        return eaten;
    }
}
