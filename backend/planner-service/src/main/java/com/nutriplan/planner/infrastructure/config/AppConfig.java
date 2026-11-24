package com.nutriplan.planner.infrastructure.config;

import com.nutriplan.planner.domain.generation.BalancedCalorieStrategy;
import com.nutriplan.planner.domain.generation.PlanGenerationStrategy;
import com.nutriplan.planner.domain.generation.PlanGeneratorRegistry;
import com.nutriplan.planner.domain.generation.RandomVarietyStrategy;
import java.time.Clock;
import java.time.ZoneId;
import java.util.List;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Beans de aplicación. El dominio (estrategias) no tiene anotaciones de Spring: se registran acá.
 * {@link Clock} inyectable => en los tests se fija la fecha ("hoy") y los resultados son estables.
 */
@Configuration
@EnableJpaAuditing
@EnableScheduling
public class AppConfig {

    @Bean
    Clock clock() {
        return Clock.system(ZoneId.of("America/Argentina/Buenos_Aires"));
    }

    @Bean
    PlanGenerationStrategy balancedStrategy() {
        return new BalancedCalorieStrategy();
    }

    @Bean
    PlanGenerationStrategy varietyStrategy() {
        return new RandomVarietyStrategy();
    }

    @Bean
    PlanGeneratorRegistry planGeneratorRegistry(List<PlanGenerationStrategy> strategies) {
        return new PlanGeneratorRegistry(strategies);
    }
}
