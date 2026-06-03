package ru.loginov.log_master.data.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import ru.loginov.log_master.data.enums.LogLevel;

import java.util.Map;

/** Тело запроса для создания/обновления шаблона через REST API. */
public record LogTemplateRequest(
        @NotBlank String logCode,
        @NotBlank String appCode,
        @NotNull LogLevel level,
        @NotEmpty Map<String, String> messages
) {}
