package com.jobify.api.config;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.CacheManager;
import org.springframework.cache.support.SimpleCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class CaffeineConfig {

    @Value("${spring.cache.redis.time-to-live:1h}")
    private Duration defaultTtl;

    @Value("${app.cache.jobs.ttl:10m}")
    private Duration jobsTtl;
    @Value("${app.cache.jobs.max-size:1000}")
    private long jobsMaxSize;

    @Value("${app.cache.job-search-ids.ttl:5m}")
    private Duration jobSearchIdsTtl;
    @Value("${app.cache.job-search-ids.max-size:1000}")
    private long jobSearchIdsMaxSize;

    @Value("${app.cache.default.ttl:5m}")
    private Duration localDefaultTtl;
    @Value("${app.cache.default.max-size:100}")
    private long localDefaultMaxSize;

    @Value("${app.cache.geo-regions.ttl:24h}")
    private Duration geoRegionsTtl;
    @Value("${app.cache.geo-regions.max-size:500}")
    private long geoRegionsMaxSize;

    @Value("${app.cache.geo-cities.ttl:24h}")
    private Duration geoCitiesTtl;
    @Value("${app.cache.geo-cities.max-size:2000}")
    private long geoCitiesMaxSize;

    @Bean
    public CacheManager caffeineCacheManager() {
        SimpleCacheManager manager = new SimpleCacheManager();
        manager.setCaches(java.util.Arrays.asList(
                // LRU Cache for jobs
                buildCache("jobs", jobsTtl, jobsMaxSize),

                // LRU Cache for search results (Index)
                buildCache("job_search_ids", jobSearchIdsTtl, jobSearchIdsMaxSize),

                // Geo Reference Caches (Long-lived LRU)
                buildCache("geo_regions", geoRegionsTtl, geoRegionsMaxSize),
                buildCache("geo_cities", geoCitiesTtl, geoCitiesMaxSize),

                // Primary cache for companies (redundant here if Redis is primary, but using
                // default TTL for safety)
                buildCache("companies", defaultTtl, 1000L),

                // Default local cache
                buildCache("default", localDefaultTtl, localDefaultMaxSize)));
        return manager;
    }

    private org.springframework.cache.caffeine.CaffeineCache buildCache(String name, Duration duration, long maxSize) {
        return new org.springframework.cache.caffeine.CaffeineCache(name, Caffeine.newBuilder()
                .expireAfterWrite(duration.toMillis(), TimeUnit.MILLISECONDS)
                .maximumSize(maxSize)
                .build());
    }
}
