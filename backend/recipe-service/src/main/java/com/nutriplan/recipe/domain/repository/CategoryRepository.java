package com.nutriplan.recipe.domain.repository;

import com.nutriplan.recipe.domain.model.Category;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

/** Spring Data genera la implementación en runtime a partir del nombre de los métodos. */
public interface CategoryRepository extends JpaRepository<Category, Long> {

    Optional<Category> findBySlug(String slug);

    List<Category> findAllByOrderBySortOrderAsc();
}
