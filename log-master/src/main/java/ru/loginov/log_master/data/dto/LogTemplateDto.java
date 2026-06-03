package ru.loginov.log_master.data.dto;

import ru.loginov.log_master.data.enums.LogLevel;

public record LogTemplateDto(String code,
                             LogLevel level,
                             String applicationCode,
                             String lang,
                             String message) {
}
