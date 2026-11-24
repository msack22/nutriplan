package com.nutriplan.planner.infrastructure.messaging;

import com.nutriplan.common.events.Topics;
import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

@Configuration
public class KafkaTopicsConfig {

    @Bean
    NewTopic planChangedTopic() {
        return TopicBuilder.name(Topics.PLAN_CHANGED).partitions(3).replicas(1).build();
    }

    @Bean
    NewTopic pricesRefreshedTopic() {
        return TopicBuilder.name(Topics.PRICES_REFRESHED).partitions(1).replicas(1).build();
    }
}
