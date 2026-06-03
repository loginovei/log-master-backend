package ru.loginov.log_master.data.dto;

import jakarta.validation.constraints.NotBlank;

/** Тело запроса для создания приложения. */
public record ApplicationRequest(
        @NotBlank String code,
        @NotBlank String name
) {}
