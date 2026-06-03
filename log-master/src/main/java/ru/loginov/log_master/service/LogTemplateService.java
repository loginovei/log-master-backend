package ru.loginov.log_master.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import ru.loginov.log_master.data.dto.LogTemplateRequest;
import ru.loginov.log_master.data.dto.LogTemplateResponse;
import ru.loginov.log_master.data.dto.PageDto;
import ru.loginov.log_master.data.model.LogTemplate;
import ru.loginov.log_client.LogClient;
import ru.loginov.log_master.exception.ConflictException;
import ru.loginov.log_master.exception.NotFoundException;
import ru.loginov.log_master.repository.LogTemplateRepository;

import java.util.List;

@Service
@RequiredArgsConstructor
public class LogTemplateService {

    private final LogTemplateRepository repository;
    private final LogTemplateCacheService cache;
    private final LogClient logClient;

    /** Возвращает постраничный список шаблонов с опциональным фильтром по приложению. */
    public PageDto<LogTemplateResponse> findAll(String appCode, int page, int size) {
        PageDto<LogTemplate> raw = repository.findAll(appCode, page, size);
        List<LogTemplateResponse> content = raw.content().stream().map(LogTemplateResponse::from).toList();
        return new PageDto<>(content, raw.totalElements(), raw.totalPages(), raw.number(), raw.size());
    }

    /** Полнотекстовый поиск по коду и текстам шаблонов на указанном языке. */
    public List<LogTemplateResponse> search(String appCode, String q, String lang) {
        return repository.search(appCode, q, lang).stream().map(LogTemplateResponse::from).toList();
    }

    /** Создаёт новый шаблон. */
    public LogTemplateResponse create(LogTemplateRequest req) {
        if (repository.findByCode(req.logCode()).isPresent()) {
            throw new ConflictException("Template already exists: " + req.logCode());
        }
        LogTemplate template = LogTemplate.builder()
                .code(req.logCode())
                .applicationCode(req.appCode())
                .level(req.level())
                .messages(req.messages())
                .build();
        LogTemplate saved = repository.save(template);
        cache.put(saved);
        logClient.info("TEMPLATE_CREATED", req.logCode());
        return LogTemplateResponse.from(saved);
    }

    /** Обновляет существующий шаблон (сообщения, appCode и level). */
    public LogTemplateResponse update(String logCode, LogTemplateRequest req) {
        LogTemplate existing = repository.findByCode(logCode)
                .orElseThrow(() -> new NotFoundException("Template not found: " + logCode));
        existing.setApplicationCode(req.appCode());
        existing.setLevel(req.level());
        existing.setMessages(req.messages());
        LogTemplate saved = repository.save(existing);
        cache.put(saved);
        return LogTemplateResponse.from(saved);
    }

    /** Удаляет шаблон по логКоду. */
    public void delete(String logCode) {
        repository.deleteByCode(logCode);
        cache.remove(logCode);
        logClient.info("TEMPLATE_DELETED", logCode);
    }
}
