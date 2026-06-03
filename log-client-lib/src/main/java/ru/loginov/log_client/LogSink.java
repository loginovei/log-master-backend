package ru.loginov.log_client;

import java.util.List;

/**
 * Стратегия доставки батча лог-записей.
 *
 * <p>Дефолтная реализация ({@link HttpLogSink}) отправляет записи на удалённый
 * сервер log-master по HTTP. Для встраивания log-client внутрь самого log-master
 * предоставьте собственный бин этого интерфейса — авто-конфигурация обнаружит его
 * и не создаст {@link HttpLogSink}.
 */
public interface LogSink {

    void send(List<LogEntryRequest> batch);
}
