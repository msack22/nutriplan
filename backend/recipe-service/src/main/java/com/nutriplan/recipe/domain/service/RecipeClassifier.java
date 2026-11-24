package com.nutriplan.recipe.domain.service;

import com.nutriplan.recipe.domain.model.DietTag;
import com.nutriplan.recipe.domain.model.MealType;
import com.nutriplan.recipe.domain.model.Nutrition;
import java.util.Collection;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

/**
 * Infere momentos del día y etiquetas a partir de la categoría, los ingredientes y los macros.
 * Es un servicio de dominio puro: reglas de negocio sin dependencias de framework.
 */
public final class RecipeClassifier {

    private static final List<String> ANIMAL_PROTEIN = List.of(
            "pollo", "carne", "atun", "atún", "jamon", "jamón", "cerdo", "pescado", "salmon", "salmón",
            "langostino", "panceta", "bondiola", "pavita", "pavo", "pastron", "pastrón", "chorizo", "lomo",
            "pechuga", "merluza", "camaron", "camarón", "bife", "nalga", "peceto", "osobuco", "matambre",
            "anchoa", "sardina", "marisco", "bacon", "salame", "milanesa", "hamburguesa de carne");

    private RecipeClassifier() {
    }

    public static Set<MealType> mealTypesFor(String categorySlug, String subcategory) {
        return switch (categorySlug) {
            case "desayunos-meriendas", "panes" -> EnumSet.of(MealType.BREAKFAST, MealType.SNACK);
            case "recetas-crunch", "postres" -> EnumSet.of(MealType.SNACK);
            case "vegetarianas" -> "desayunos-meriendas".equals(subcategory)
                    ? EnumSet.of(MealType.BREAKFAST, MealType.SNACK)
                    : EnumSet.of(MealType.LUNCH, MealType.DINNER);
            default -> EnumSet.of(MealType.LUNCH, MealType.DINNER);
        };
    }

    public static Set<DietTag> tagsFor(String categorySlug, Collection<String> ingredientNames, Nutrition n) {
        Set<DietTag> tags = EnumSet.noneOf(DietTag.class);
        switch (categorySlug) {
            case "vegetarianas" -> tags.add(DietTag.VEGETARIAN);
            case "panes" -> tags.add(DietTag.BREAD);
            case "postres" -> tags.addAll(List.of(DietTag.DESSERT, DietTag.LOW_CARB));
            case "recetas-crunch" -> tags.add(DietTag.CRUNCHY_SNACK);
            default -> { }
        }
        if (isVegetarian(ingredientNames)) {
            tags.add(DietTag.VEGETARIAN);
        }
        if (n != null) {
            if (n.proteinG() != null && n.proteinG() >= 25) tags.add(DietTag.HIGH_PROTEIN);
            if (n.carbsG() != null && n.carbsG() <= 15) tags.add(DietTag.LOW_CARB);
            if (n.kcal() != null && n.kcal() <= 300) tags.add(DietTag.LOW_CALORIE);
        }
        return tags;
    }

    public static boolean isVegetarian(Collection<String> ingredientNames) {
        return !ingredientNames.isEmpty() && ingredientNames.stream()
                .noneMatch(name -> ANIMAL_PROTEIN.stream().anyMatch(name::contains));
    }
}
