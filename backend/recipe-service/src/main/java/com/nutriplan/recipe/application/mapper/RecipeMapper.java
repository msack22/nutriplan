package com.nutriplan.recipe.application.mapper;

import com.nutriplan.recipe.application.dto.CategoryDto;
import com.nutriplan.recipe.application.dto.RecipeCandidateDto;
import com.nutriplan.recipe.application.dto.RecipeSummaryDto;
import com.nutriplan.recipe.domain.model.Category;
import com.nutriplan.recipe.domain.model.Recipe;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

/**
 * MapStruct genera la implementación en tiempo de COMPILACIÓN (target/generated-sources):
 * sin reflexión => tan rápido como escribirlo a mano, pero sin el código repetitivo.
 * {@code componentModel = "spring"} => la implementación es un @Component inyectable.
 */
@Mapper(componentModel = "spring")
public interface RecipeMapper {

    CategoryDto toDto(Category category);

    @Mapping(target = "categorySlug", source = "category.slug")
    @Mapping(target = "categoryName", source = "category.name")
    @Mapping(target = "categoryEmoji", source = "category.emoji")
    @Mapping(target = "nutritionPerServing", source = "nutrition")
    RecipeSummaryDto toSummary(Recipe recipe);

    @Mapping(target = "nutritionPerServing", source = "recipe.nutrition")
    @Mapping(target = "preferenceScore", source = "score")
    RecipeCandidateDto toCandidate(Recipe recipe, double score);
}
