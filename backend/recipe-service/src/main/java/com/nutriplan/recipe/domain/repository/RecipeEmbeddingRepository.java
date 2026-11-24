package com.nutriplan.recipe.domain.repository;

import com.nutriplan.recipe.domain.model.RecipeEmbedding;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RecipeEmbeddingRepository extends JpaRepository<RecipeEmbedding, Long> {

    List<RecipeEmbedding> findByModel(String model);
}
