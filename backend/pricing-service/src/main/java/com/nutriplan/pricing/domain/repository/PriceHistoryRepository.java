package com.nutriplan.pricing.domain.repository;

import com.nutriplan.pricing.domain.model.PriceHistory;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PriceHistoryRepository extends JpaRepository<PriceHistory, Long> {

    List<PriceHistory> findByIngredientOrderByDateAsc(String ingredient);
}
