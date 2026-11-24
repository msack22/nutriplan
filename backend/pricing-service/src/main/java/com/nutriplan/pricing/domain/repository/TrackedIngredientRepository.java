package com.nutriplan.pricing.domain.repository;

import com.nutriplan.pricing.domain.model.TrackedIngredient;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TrackedIngredientRepository extends JpaRepository<TrackedIngredient, String> {
}
