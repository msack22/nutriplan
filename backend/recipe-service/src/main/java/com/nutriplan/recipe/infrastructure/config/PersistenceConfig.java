package com.nutriplan.recipe.infrastructure.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * {@code @EnableJpaAuditing}: completa @CreatedDate/@LastModifiedDate.
 * {@code @EnableAsync}: habilita @Async (con virtual threads, ver application.yml).
 * {@code @EnableScheduling}: habilita @Scheduled (relay del outbox).
 */
@Configuration
@EnableJpaAuditing
@EnableAsync
@EnableScheduling
public class PersistenceConfig {
}
