package ru.loginov.log_master.data.dto;

import java.time.Instant;
import java.util.List;

public record LogEntryDto(String code,
                          String applicationCode,
                          Instant created,
                          String exception,
                          String additional,
                          List<Object> args,
                          String level) {
}
