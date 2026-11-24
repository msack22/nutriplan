package com.nutriplan.planner.domain.repository;

import com.nutriplan.planner.domain.model.WeightEntry;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface WeightEntryRepository extends JpaRepository<WeightEntry, Long> {

    List<WeightEntry> findByUserIdOrderByDateAsc(String userId);

    Optional<WeightEntry> findByUserIdAndDate(String userId, LocalDate date);
}
