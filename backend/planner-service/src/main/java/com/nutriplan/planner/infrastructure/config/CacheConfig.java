package com.nutriplan.planner.infrastructure.config;

import com.nutriplan.planner.application.dto.ShoppingListDto;
import com.nutriplan.planner.application.usecase.ShoppingListCalculator;
import java.time.Duration;
import org.springframework.boot.cache.autoconfigure.RedisCacheManagerBuilderCustomizer;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.serializer.JacksonJsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext.SerializationPair;

/**
 * Caché distribuida en Redis (compartida entre réplicas del servicio, sobrevive reinicios).
 * Serializador JSON TIPADO por caché: evita "default typing" polimórfico de Jackson, que es un
 * vector clásico de deserialización insegura (RCE).
 */
@Configuration
@EnableCaching
public class CacheConfig {

    @Bean
    RedisCacheManagerBuilderCustomizer shoppingListCache() {
        return builder -> builder.withCacheConfiguration(ShoppingListCalculator.CACHE,
                RedisCacheConfiguration.defaultCacheConfig()
                        .prefixCacheNameWith("nutriplan:planner:")
                        .entryTtl(Duration.ofHours(12))
                        .disableCachingNullValues()
                        .serializeValuesWith(SerializationPair.fromSerializer(
                                new JacksonJsonRedisSerializer<>(ShoppingListDto.class))));
    }
}
