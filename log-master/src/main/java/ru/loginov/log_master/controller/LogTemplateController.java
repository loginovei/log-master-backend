package ru.loginov.log_master.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import ru.loginov.log_master.data.dto.LogTemplateRequest;
import ru.loginov.log_master.data.dto.LogTemplateResponse;
import ru.loginov.log_master.data.dto.PageDto;
import ru.loginov.log_master.service.LogTemplateService;

import java.util.List;

@RestController
@RequestMapping("/api/templates")
@RequiredArgsConstructor
@Tag(name = "Log Templates", description = "Управление шаблонами лог-сообщений")
public class LogTemplateController {

    private final LogTemplateService service;

    @GetMapping
    @Operation(summary = "Постраничный список шаблонов",
               description = "Возвращает шаблоны с опциональной фильтрацией по приложению")
    public ResponseEntity<PageDto<LogTemplateResponse>> findAll(
            @Parameter(description = "Код приложения для фильтрации")
            @RequestParam(required = false) String appCode,
            @Parameter(description = "Номер страницы (с 0)")
            @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Размер страницы")
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(service.findAll(appCode, page, size));
    }

    @GetMapping("/search")
    @Operation(summary = "Полнотекстовый поиск шаблонов",
               description = "Ищет по коду шаблона и текстам сообщений на указанном языке (phrase_prefix)")
    public ResponseEntity<List<LogTemplateResponse>> search(
            @Parameter(description = "Код приложения для фильтрации")
            @RequestParam(required = false) String appCode,
            @Parameter(description = "Поисковый запрос (часть кода или текста сообщения)")
            @RequestParam(required = false) String q,
            @Parameter(description = "Язык поиска (ru / en / zh). Если не указан — ищет по всем языкам")
            @RequestParam(required = false) String lang) {
        return ResponseEntity.ok(service.search(appCode, q, lang));
    }

    @PostMapping
    @Operation(summary = "Создать шаблон")
    public ResponseEntity<LogTemplateResponse> create(@Valid @RequestBody LogTemplateRequest req) {
        return ResponseEntity.ok(service.create(req));
    }

    @PutMapping("/{logCode}")
    @Operation(summary = "Обновить шаблон")
    public ResponseEntity<LogTemplateResponse> update(
            @Parameter(description = "Код шаблона лога", example = "AUTH_001")
            @PathVariable String logCode,
            @Valid @RequestBody LogTemplateRequest req) {
        return ResponseEntity.ok(service.update(logCode, req));
    }

    @DeleteMapping("/{logCode}")
    @Operation(summary = "Удалить шаблон")
    public ResponseEntity<Void> delete(
            @Parameter(description = "Код шаблона лога", example = "AUTH_001")
            @PathVariable String logCode) {
        service.delete(logCode);
        return ResponseEntity.noContent().build();
    }
}
