package ru.loginov.log_client;

import java.time.Instant;
import java.util.List;

/** DTO, отправляемый в log-master. Поля совпадают с LogEntryDto сервера. */
public record LogEntryRequest(
        String code,
        String applicationCode,
        Instant created,
        String exception,
        String additional,
        List<Object> args,
        String level
) {}
