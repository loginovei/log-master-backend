package ru.loginov.log_master.data.dto;

import ru.loginov.log_master.data.model.LogTemplate;

import java.util.Map;

/** Ответ API для шаблона лога (поля соответствуют ожиданиям фронтенда). */
public record LogTemplateResponse(
        String id,
        String logCode,
        String appCode,
        String level,
        Map<String, String> messages
) {
    public static LogTemplateResponse from(LogTemplate t) {
        return new LogTemplateResponse(
                t.getId(), t.getCode(), t.getApplicationCode(),
                t.getLevel() != null ? t.getLevel().name() : null,
                t.getMessages());
    }
}
