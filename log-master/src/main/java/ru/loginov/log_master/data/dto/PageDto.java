package ru.loginov.log_master.data.dto;

import java.util.List;

/** Универсальный постраничный ответ. */
public record PageDto<T>(
        List<T> content,
        long totalElements,
        int totalPages,
        int number,
        int size
) {}
