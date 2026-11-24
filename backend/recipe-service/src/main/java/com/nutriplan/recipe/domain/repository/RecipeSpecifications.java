package com.nutriplan.recipe.domain.repository;

import com.nutriplan.recipe.domain.model.DietTag;
import com.nutriplan.recipe.domain.model.MealType;
import com.nutriplan.recipe.domain.model.Recipe;
import jakarta.persistence.criteria.Join;
import java.util.Locale;
import org.springframework.data.jpa.domain.Specification;

/**
 * Patrón <b>Specification</b>: cada filtro es un objeto pequeño y combinable con and()/or().
 * Internamente usa la Criteria API de JPA (consultas type-safe construidas en runtime).
 * Cuando un filtro no viene informado devolvemos {@code unrestricted()} (no filtra nada).
 */
public final class RecipeSpecifications {

    private RecipeSpecifications() {
    }

    public static Specification<Recipe> active() {
        return (root, q, cb) -> cb.isTrue(root.get("active"));
    }

    public static Specification<Recipe> titleContains(String text) {
        if (text == null || text.isBlank()) {
            return Specification.unrestricted();
        }
        String like = "%" + text.toLowerCase(Locale.ROOT).strip() + "%";
        return (root, q, cb) -> cb.like(cb.lower(root.get("title")), like);
    }

    public static Specification<Recipe> inCategory(String slug) {
        if (slug == null || slug.isBlank()) {
            return Specification.unrestricted();
        }
        return (root, q, cb) -> cb.equal(root.get("category").get("slug"), slug);
    }

    public static Specification<Recipe> forMeal(MealType mealType) {
        if (mealType == null) {
            return Specification.unrestricted();
        }
        return (root, q, cb) -> {
            Join<Recipe, MealType> meals = root.join("mealTypes");
            q.distinct(true);
            return cb.equal(meals, mealType);
        };
    }

    public static Specification<Recipe> withTag(DietTag tag) {
        if (tag == null) {
            return Specification.unrestricted();
        }
        return (root, q, cb) -> {
            Join<Recipe, DietTag> tags = root.join("tags");
            q.distinct(true);
            return cb.equal(tags, tag);
        };
    }

    public static Specification<Recipe> maxKcal(Double max) {
        if (max == null) {
            return Specification.unrestricted();
        }
        return (root, q, cb) -> cb.lessThanOrEqualTo(root.get("nutrition").get("kcal"), max);
    }

    public static Specification<Recipe> minProtein(Double min) {
        if (min == null) {
            return Specification.unrestricted();
        }
        return (root, q, cb) -> cb.greaterThanOrEqualTo(root.get("nutrition").get("proteinG"), min);
    }
}
