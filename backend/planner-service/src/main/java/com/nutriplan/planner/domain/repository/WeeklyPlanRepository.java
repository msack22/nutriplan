package com.nutriplan.planner.domain.repository;

import com.nutriplan.planner.domain.model.WeeklyPlan;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface WeeklyPlanRepository extends JpaRepository<WeeklyPlan, Long> {

    @EntityGraph(attributePaths = "meals")
    Optional<WeeklyPlan> findByUserIdAndWeekStart(String userId, LocalDate weekStart);

    @EntityGraph(attributePaths = "meals")
    Optional<WeeklyPlan> findWithMealsById(Long id);

    @EntityGraph(attributePaths = "meals")
    List<WeeklyPlan> findByUserIdAndWeekStartBetweenOrderByWeekStartAsc(String userId, LocalDate from, LocalDate to);
}
