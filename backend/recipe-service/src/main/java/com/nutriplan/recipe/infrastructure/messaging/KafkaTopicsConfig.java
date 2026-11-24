package com.nutriplan.recipe.infrastructure.messaging;

import com.nutriplan.common.events.Topics;
import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

/** Spring Kafka crea los tópicos al iniciar si no existen (KafkaAdmin). */
@Configuration
public class KafkaTopicsConfig {

    @Bean
    NewTopic ingredientsChangedTopic() {
        return TopicBuilder.name(Topics.INGREDIENTS_CHANGED).partitions(3).replicas(1).build();
    }
}
