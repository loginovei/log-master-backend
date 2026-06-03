package ru.loginov.log_master.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import ru.loginov.log_master.data.dto.AppStatsDto;
import ru.loginov.log_master.data.dto.LogEntryDto;
import ru.loginov.log_master.data.dto.LogEntryResponse;
import ru.loginov.log_master.data.dto.PageDto;
import ru.loginov.log_master.service.LogService;

import java.util.List;

@RestController
@RequestMapping("/api/logs")
@RequiredArgsConstructor
@Tag(name = "Logs", description = "Поиск записей логов и получение статистики")
public class LogController {

    private final LogService service;

    @GetMapping
    @Operation(summary = "Поиск записей логов",
               description = "Постраничный поиск с фильтрами по приложению, кодам логов, уровню, диапазону и параметрам")
    public ResponseEntity<PageDto<LogEntryResponse>> search(
            @Parameter(description = "Код приложения")
            @RequestParam(required = false) String appCode,
            @Parameter(description = "Коды шаблонов логов (можно несколько)", example = "AUTH_001")
            @RequestParam(required = false) List<String> logCodes,
            @Parameter(description = "Уровень лога", example = "ERROR")
            @RequestParam(required = false) String level,
            @Parameter(description = "Начало периода (ISO 8601)", example = "2026-05-10T00:00:00Z")
            @RequestParam(required = false) String from,
            @Parameter(description = "Конец периода (ISO 8601)", example = "2026-05-10T23:59:59Z")
            @RequestParam(required = false) String to,
            @Parameter(description = "Поиск по значениям параметров лога", example = "john.doe")
            @RequestParam(required = false) String argsQuery,
            @Parameter(description = "Номер страницы (с 0)")
            @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Размер страницы")
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(service.search(appCode, logCodes, level, from, to, argsQuery, page, size));
    }

    @PostMapping
    @Operation(summary = "Сохранить запись лога")
    public ResponseEntity<LogEntryResponse> save(@Valid @RequestBody LogEntryDto dto) {
        return ResponseEntity.ok(service.save(dto));
    }

    @PostMapping("/batch")
    @Operation(summary = "Сохранить пакет записей логов")
    public ResponseEntity<List<LogEntryResponse>> saveBatch(@RequestBody List<LogEntryDto> dtos) {
        return ResponseEntity.ok(service.saveBatch(dtos));
    }

    @GetMapping("/stats")
    @Operation(summary = "Статистика по логам",
               description = "Возвращает счётчики по уровням, сервисам и активность за последние дни")
    public ResponseEntity<AppStatsDto> getStats(
            @Parameter(description = "Код приложения (если не указан — по всем)")
            @RequestParam(required = false) String appCode) {
        return ResponseEntity.ok(service.getStats(appCode));
    }
}
