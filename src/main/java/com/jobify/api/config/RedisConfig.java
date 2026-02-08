package com.jobify.api.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.serializer.RedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;

@Configuration
@ConditionalOnProperty(prefix = "spring.cache", name = "type", havingValue = "redis", matchIfMissing = true)
public class RedisConfig {

        @Value("${spring.cache.redis.time-to-live}") // Default to 1h if property is missing
        private Duration ttl;

        @Bean
        @org.springframework.context.annotation.Primary
        public RedisCacheManager cacheManager(RedisConnectionFactory connectionFactory) {
                // 1. Define the Default Configuration
                RedisCacheConfiguration defaultConfig = RedisCacheConfiguration.defaultCacheConfig()
                                .entryTtl(Duration.ofHours(1)) // Default fallback
                                .serializeValuesWith(
                                                RedisSerializationContext.SerializationPair
                                                                .fromSerializer(RedisSerializer.json()));

                // 2. Define Specific TTLs for different cache names
                Map<String, RedisCacheConfiguration> cacheConfigurations = new HashMap<>();

                cacheConfigurations.put("companies", defaultConfig.entryTtl(ttl));

                // 3. Build the CacheManager
                return RedisCacheManager.builder(connectionFactory)
                                .cacheDefaults(defaultConfig) // Fallback for any name not in the map
                                .withInitialCacheConfigurations(cacheConfigurations) // Apply the map
                                .build();
        }
}