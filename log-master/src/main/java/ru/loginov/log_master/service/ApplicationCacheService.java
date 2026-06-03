package ru.loginov.log_master.service;

import com.github.benmanes.caffeine.cache.Cache;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.DependsOn;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import ru.loginov.log_master.data.model.Application;
import ru.loginov.log_master.repository.ApplicationRepository;

import jakarta.annotation.PostConstruct;
import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
@DependsOn("indexInitializer")
public class ApplicationCacheService {

    private final ApplicationRepository repository;
    private final Cache<String, Application> applicationCache;

    @PostConstruct
    public void init() {
        refresh();
    }

    @Scheduled(fixedDelayString = "${cache.applications.refresh-interval-ms:60000}")
    public void refresh() {
        try {
            applicationCache.invalidateAll();
            repository.findAll().forEach(a -> applicationCache.put(a.getCode(), a));
            log.debug("Application cache refreshed: {} entries", applicationCache.estimatedSize());
        } catch (Exception e) {
            log.error("Failed to refresh application cache", e);
        }
    }

    public List<Application> getAll() {
        return List.copyOf(applicationCache.asMap().values());
    }

    public Optional<Application> findByCode(String code) {
        return Optional.ofNullable(applicationCache.getIfPresent(code));
    }

    public void put(Application app) {
        applicationCache.put(app.getCode(), app);
    }

    public void remove(String code) {
        applicationCache.invalidate(code);
    }
}
