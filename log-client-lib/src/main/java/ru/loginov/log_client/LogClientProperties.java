package ru.loginov.log_client;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "log-master.client")
@Getter
@Setter
public class LogClientProperties {

    /** URL сервера log-master (без trailing slash). */
    private String serverUrl = "http://localhost:8080";

    /** Код приложения — идентификатор сервиса, отправляющего логи. */
    private String appCode = "unknown";

    /** Максимальный размер буферной очереди. При переполнении записи отбрасываются. */
    private int queueCapacity = 10000;

    /** Максимальное количество записей в одном HTTP-запросе к серверу. */
    private int batchSize = 50;

    /** Интервал между отправками батча (мс). */
    private long flushIntervalMs = 500;
}
