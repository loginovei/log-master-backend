package ru.loginov.log_master.data.dto;

import ru.loginov.log_master.data.model.LogEntry;

import java.util.Map;
import java.util.Objects;

/** Ответ API для записи лога. */
public record LogEntryResponse(
        String id,
        String logCode,
        Map<String, Object> params,
        String timestamp,
        String service,
        String level,
        String stackTrace,
        String additional
) {
    public static LogEntryResponse from(LogEntry e) {
        return new LogEntryResponse(
                e.getId(),
                e.getCode(),
                Objects.requireNonNullElse(e.getArgs(), Map.of()),
                e.getCreated() != null ? e.getCreated().toString() : null,
                e.getApplicationCode(),
                e.getLevel() != null ? e.getLevel().name() : null,
                e.getException(),
                e.getAdditional() != null ? e.getAdditional().toPrettyString() : null
        );
    }
}
