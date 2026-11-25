package com.nutriplan.recipe.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.nutriplan.recipe.domain.model.Category;
import com.nutriplan.recipe.domain.model.Recipe;
import org.junit.jupiter.api.Test;

class RecipeVisibilityTest {

    private final Category category = new Category("comidas-principales", "Comidas", "🥗", 1);

    @Test
    void catalogRecipesAreVisibleToEveryone() {
        Recipe catalog = new Recipe("ext-1", "Ensalada", category);
        assertThat(catalog.isVisibleTo("alice")).isTrue();
        assertThat(catalog.isOwnedBy("alice")).isFalse();
        assertThat(catalog.isCustom()).isFalse();
    }

    @Test
    void customRecipesAreOnlyVisibleToTheirOwner() {
        Recipe mine = Recipe.ownedBy("alice", "Mi pollo", category);
        assertThat(mine.isVisibleTo("alice")).isTrue();
        assertThat(mine.isVisibleTo("mallory")).isFalse();
        assertThat(mine.isOwnedBy("mallory")).isFalse();
    }

    @Test
    void deletedRecipesAreNotVisible() {
        Recipe mine = Recipe.ownedBy("alice", "Mi pollo", category);
        mine.deactivate();
        assertThat(mine.isVisibleTo("alice")).isFalse();
    }

    @Test
    void customRecipeRequiresOwner() {
        assertThatThrownBy(() -> Recipe.ownedBy(" ", "x", category)).isInstanceOf(IllegalArgumentException.class);
    }
}
