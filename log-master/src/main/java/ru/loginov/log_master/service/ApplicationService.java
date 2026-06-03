package ru.loginov.log_master.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import ru.loginov.log_master.data.dto.ApplicationResponse;
import ru.loginov.log_master.data.model.Application;
import ru.loginov.log_client.LogClient;
import ru.loginov.log_master.exception.ConflictException;
import ru.loginov.log_master.exception.NotFoundException;
import ru.loginov.log_master.repository.ApplicationRepository;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ApplicationService {

    private final ApplicationRepository repository;
    private final ApplicationCacheService cache;
    private final LogClient logClient;

    public List<ApplicationResponse> findAll() {
        return cache.getAll().stream().map(ApplicationResponse::from).toList();
    }

    public ApplicationResponse create(String code, String name) {
        if (repository.findByCode(code).isPresent()) {
            throw new ConflictException("Application already exists: " + code);
        }
        Application saved = repository.save(Application.builder().code(code).name(name).build());
        cache.put(saved);
        logClient.info("APP_CREATED", code);
        return ApplicationResponse.from(saved);
    }

    public ApplicationResponse update(String code, String name) {
        Application existing = repository.findByCode(code)
                .orElseThrow(() -> new NotFoundException("Application not found: " + code));
        existing.setName(name);
        Application saved = repository.save(existing);
        cache.put(saved);
        return ApplicationResponse.from(saved);
    }

    public ApplicationResponse getByCode(String code) {
        return cache.findByCode(code)
                .map(ApplicationResponse::from)
                .orElseThrow(() -> new NotFoundException("Application not found: " + code));
    }

    public void delete(String code) {
        repository.deleteByCode(code);
        cache.remove(code);
        logClient.info("APP_DELETED", code);
    }
}
