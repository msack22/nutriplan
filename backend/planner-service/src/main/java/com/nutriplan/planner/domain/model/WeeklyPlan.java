package com.nutriplan.planner.domain.model;

import com.nutriplan.common.error.BusinessRuleException;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.persistence.Version;
import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

/**
 * Aggregate Root: el plan semanal de un usuario. TODA modificación de comidas pasa por acá,
 * así las invariantes se respetan siempre:
 * <ul>
 *   <li>la semana empieza un lunes</li>
 *   <li>como máximo una receta por (día, momento)</li>
 *   <li>personas entre 1 y 12</li>
 * </ul>
 * {@code @Version} => si el usuario edita el plan desde dos pestañas, la segunda escritura
 * falla con OptimisticLockException (409) en vez de pisar cambios.
 */
@Entity
@Table(name = "weekly_plan", uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "week_start"}))
@EntityListeners(AuditingEntityListener.class)
public class WeeklyPlan {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false, length = 64)
    private String userId;

    @Column(name = "week_start", nullable = false)
    private LocalDate weekStart;

    @Column(nullable = false)
    private int people;

    @OneToMany(mappedBy = "plan", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<PlannedMeal> meals = new ArrayList<>();

    @Version
    private long version;

    @CreatedDate
    @Column(name = "created_at", updatable = false)
    private Instant createdAt;

    @LastModifiedDate
    @Column(name = "updated_at")
    private Instant updatedAt;

    protected WeeklyPlan() {
    }

    public WeeklyPlan(String userId, LocalDate weekStart, int people) {
        if (weekStart.getDayOfWeek() != DayOfWeek.MONDAY) {
            throw new BusinessRuleException("La semana del plan debe empezar un lunes");
        }
        this.userId = userId;
        this.weekStart = weekStart;
        changePeople(people);
    }

    /** Asigna (o reemplaza) la receta de un hueco. servings null => personas del plan. */
    public PlannedMeal assign(DayOfWeek day, MealType mealType, RecipeRef recipe, Integer servings) {
        int effectiveServings = servings == null ? people : servings;
        if (effectiveServings < 1 || effectiveServings > 24) {
            throw new BusinessRuleException("Porciones fuera de rango (1-24)");
        }
        Optional<PlannedMeal> existing = mealAt(day, mealType);
        if (existing.isPresent()) {
            existing.get().change(recipe, effectiveServings);
            return existing.get();
        }
        PlannedMeal meal = new PlannedMeal(this, day, mealType, recipe, effectiveServings);
        meals.add(meal);
        return meal;
    }

    public void remove(DayOfWeek day, MealType mealType) {
        meals.removeIf(m -> m.occupies(day, mealType));
    }

    public void clear() {
        meals.clear();
    }

    public void markEaten(DayOfWeek day, MealType mealType, boolean eaten) {
        mealAt(day, mealType).orElseThrow(() -> new BusinessRuleException("No hay comida planificada en ese horario"))
                .markEaten(eaten);
    }

    /** Cambia las personas y re-escala las porciones de las comidas que seguían el default. */
    public void changePeople(int newPeople) {
        if (newPeople < 1 || newPeople > 12) {
            throw new BusinessRuleException("Las personas deben estar entre 1 y 12");
        }
        int old = this.people;
        this.people = newPeople;
        meals.stream().filter(m -> m.getServings() == old)
                .forEach(m -> m.change(new RecipeRef(m.getRecipeId(), m.getRecipeTitle(), m.getNutritionPerServing()),
                        newPeople));
    }

    public Optional<PlannedMeal> mealAt(DayOfWeek day, MealType mealType) {
        return meals.stream().filter(m -> m.occupies(day, mealType)).findFirst();
    }

    public boolean belongsTo(String user) {
        return userId.equals(user);
    }

    public LocalDate dateOf(DayOfWeek day) {
        return weekStart.plusDays(day.getValue() - 1L);
    }

    public Long getId() {
        return id;
    }

    public String getUserId() {
        return userId;
    }

    public LocalDate getWeekStart() {
        return weekStart;
    }

    public int getPeople() {
        return people;
    }

    public List<PlannedMeal> getMeals() {
        List<PlannedMeal> sorted = new ArrayList<>(meals);
        sorted.sort(Comparator.comparing(PlannedMeal::getDay).thenComparing(PlannedMeal::getMealType));
        return Collections.unmodifiableList(sorted);
    }

    public long getVersion() {
        return version;
    }
}
