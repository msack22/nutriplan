package com.nutriplan.recipe.domain.repository;

import com.nutriplan.recipe.domain.model.Recipe;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;

/**
 * Repositorio de recetas.
 * <ul>
 *   <li>{@link JpaSpecificationExecutor}: filtros dinámicos combinables (patrón Specification).</li>
 *   <li>{@link EntityGraph}: indica qué relaciones traer con JOIN FETCH en esa consulta puntual,
 *       evitando el problema N+1 sin cambiar el mapeo LAZY por defecto.</li>
 *   <li>{@link Query} con JPQL: consultas que no se pueden expresar con el nombre del método.</li>
 * </ul>
 */
public interface RecipeRepository extends JpaRepository<Recipe, Long>, JpaSpecificationExecutor<Recipe> {

    @Override
    @EntityGraph(attributePaths = "category")
    Page<Recipe> findAll(Specification<Recipe> spec, Pageable pageable);

    @EntityGraph(attributePaths = {"category", "ingredients"})
    Optional<Recipe> findWithIngredientsById(Long id);

    @EntityGraph(attributePaths = {"category", "ingredients"})
    List<Recipe> findWithIngredientsByIdIn(Collection<Long> ids);

    @EntityGraph(attributePaths = "category")
    List<Recipe> findByActiveTrue();

    Optional<Recipe> findByExternalId(String externalId);

    @Query("select distinct i.name from RecipeIngredient i where i.recipe.active = true order by i.name")
    List<String> findDistinctIngredientNames();

    @Query("select r.id from Recipe r where r.active = true and r.nutritionSource = "
            + "com.nutriplan.recipe.domain.model.NutritionSource.UNKNOWN")
    List<Long> findIdsWithoutNutrition();
}
