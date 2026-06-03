package ru.loginov.log_master.config;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import ru.loginov.log_master.data.model.Application;
import ru.loginov.log_master.data.model.LogTemplate;

@Configuration
public class CacheConfig {

    @Bean
    public Cache<String, Application> applicationCache(
            @Value("${cache.applications.max-size:1000}") long maxSize) {
        return Caffeine.newBuilder()
                .maximumSize(maxSize)
                .recordStats()
                .build();
    }

    @Bean
    public Cache<String, LogTemplate> logTemplateCache(
            @Value("${cache.templates.max-size:10000}") long maxSize) {
        return Caffeine.newBuilder()
                .maximumSize(maxSize)
                .recordStats()
                .build();
    }
}
