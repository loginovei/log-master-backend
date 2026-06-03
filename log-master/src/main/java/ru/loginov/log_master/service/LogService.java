package ru.loginov.log_master.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import com.fasterxml.jackson.databind.node.TextNode;
import ru.loginov.log_master.data.dto.AppStatsDto;
import ru.loginov.log_master.data.dto.LogEntryDto;
import ru.loginov.log_master.data.dto.LogEntryResponse;
import ru.loginov.log_master.data.dto.PageDto;
import ru.loginov.log_master.data.enums.LogLevel;
import ru.loginov.log_master.data.model.LogEntry;
import ru.loginov.log_master.data.model.LogTemplate;
import ru.loginov.log_master.repository.LogEntryRepository;
import ru.loginov.log_master.repository.LogTemplateRepository;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class LogService {

    private final LogEntryRepository entryRepository;
    private final LogTemplateRepository templateRepository;
    private final LogTemplateCacheService templateCache;

    /** Постраничный поиск записей логов с фильтрами. */
    public PageDto<LogEntryResponse> search(String appCode, List<String> logCodes, String level,
                                            String from, String to, String argsQuery, int page, int size) {
        PageDto<LogEntry> raw = entryRepository.search(appCode, logCodes, level, from, to, argsQuery, page, size);
        List<LogEntryResponse> content = raw.content().stream().map(LogEntryResponse::from).toList();
        return new PageDto<>(content, raw.totalElements(), raw.totalPages(), raw.number(), raw.size());
    }

    /** Сохраняет новую запись лога, денормализуя level из шаблона. */
    public LogEntryResponse save(LogEntryDto dto) {
        return LogEntryResponse.from(entryRepository.save(toEntry(dto)));
    }

    /** Сохраняет пакет записей логов одним bulk-запросом. */
    public List<LogEntryResponse> saveBatch(List<LogEntryDto> dtos) {
        List<LogEntry> entries = dtos.stream().map(this::toEntry).toList();
        return entryRepository.saveBatch(entries).stream().map(LogEntryResponse::from).toList();
    }

    private LogEntry toEntry(LogEntryDto dto) {
        Map<String, Object> argsMap = new LinkedHashMap<>();
        if (dto.args() != null) {
            for (int i = 0; i < dto.args().size(); i++) {
                argsMap.put(String.valueOf(i), dto.args().get(i));
            }
        }
        LogEntry entry = LogEntry.builder()
                .code(dto.code())
                .applicationCode(dto.applicationCode())
                .created(dto.created())
                .exception(dto.exception())
                .additional(dto.additional() != null ? TextNode.valueOf(dto.additional()) : null)
                .args(argsMap)
                .build();
        if (dto.level() != null && !dto.level().isBlank()) {
            try { entry.setLevel(LogLevel.valueOf(dto.level().toUpperCase())); } catch (IllegalArgumentException ignored) {}
        }
        if (entry.getLevel() == null) {
            templateCache.findByCode(dto.code()).map(LogTemplate::getLevel).ifPresent(entry::setLevel);
        }
        return entry;
    }

    /** Возвращает агрегированную статистику по логам. */
    public AppStatsDto getStats(String appCode) {
        long totalTemplates = templateRepository.count(appCode);
        long totalEntries   = entryRepository.count(appCode);
        LogEntryRepository.StatsAggResult agg = entryRepository.aggregateStats(appCode);

        return new AppStatsDto(totalTemplates, totalEntries,
                agg.byLevel(), agg.byService(), agg.recentActivity());
    }
}
