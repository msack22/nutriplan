package com.nutriplan.pricing.domain.repository;

import com.nutriplan.pricing.domain.model.ProductPrice;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface ProductPriceRepository extends JpaRepository<ProductPrice, Long> {

    Optional<ProductPrice> findByChainAndIngredient(String chain, String ingredient);

    List<ProductPrice> findByIngredientIn(Collection<String> ingredients);

    List<ProductPrice> findByIngredientOrderByPriceAsc(String ingredient);

    @Query("select max(p.fetchedAt) from ProductPrice p")
    Optional<Instant> lastUpdate();
}
