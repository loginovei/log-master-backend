package ru.loginov.log_master.service;

import com.github.benmanes.caffeine.cache.Cache;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.DependsOn;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import ru.loginov.log_master.data.model.LogTemplate;
import ru.loginov.log_master.repository.LogTemplateRepository;

import jakarta.annotation.PostConstruct;
import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
@DependsOn("indexInitializer")
public class LogTemplateCacheService {

    private final LogTemplateRepository repository;
    private final Cache<String, LogTemplate> logTemplateCache;

    @PostConstruct
    public void init() {
        refresh();
    }

    @Scheduled(fixedDelayString = "${cache.templates.refresh-interval-ms:60000}")
    public void refresh() {
        try {
            logTemplateCache.invalidateAll();
            repository.findAll().forEach(t -> logTemplateCache.put(t.getCode(), t));
            log.debug("Log template cache refreshed: {} entries", logTemplateCache.estimatedSize());
        } catch (Exception e) {
            log.error("Failed to refresh log template cache", e);
        }
    }

    public List<LogTemplate> getAll() {
        return List.copyOf(logTemplateCache.asMap().values());
    }

    public Optional<LogTemplate> findByCode(String code) {
        return Optional.ofNullable(logTemplateCache.getIfPresent(code));
    }

    public List<LogTemplate> getByAppCode(String appCode) {
        return logTemplateCache.asMap().values().stream()
                .filter(t -> appCode.equals(t.getApplicationCode()))
                .toList();
    }

    public void put(LogTemplate template) {
        logTemplateCache.put(template.getCode(), template);
    }

    public void remove(String code) {
        logTemplateCache.invalidate(code);
    }
}
