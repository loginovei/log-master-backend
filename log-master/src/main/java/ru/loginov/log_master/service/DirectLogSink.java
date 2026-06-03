package ru.loginov.log_master.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import ru.loginov.log_client.LogEntryRequest;
import ru.loginov.log_client.LogSink;
import ru.loginov.log_master.data.dto.LogEntryDto;

import java.util.List;

/**
 * Реализация {@link LogSink} для встроенного использования внутри log-master.
 * Вместо HTTP-запроса напрямую вызывает {@link LogService#saveBatch}.
 *
 * <p>Присутствие этого бина в контексте подавляет создание HttpLogSink
 * из авто-конфигурации log-client-lib.
 */
@Component
@RequiredArgsConstructor
public class DirectLogSink implements LogSink {

    private final LogService logService;

    @Override
    public void send(List<LogEntryRequest> batch) {
        List<LogEntryDto> dtos = batch.stream()
                .map(req -> new LogEntryDto(
                        req.code(),
                        req.applicationCode(),
                        req.created(),
                        req.exception(),
                        req.additional(),
                        req.args(),
                        req.level()))
                .toList();
        logService.saveBatch(dtos);
    }
}
