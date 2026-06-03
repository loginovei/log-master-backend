package ru.loginov.log_client;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.http.MediaType;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import java.util.List;

/**
 * Реализация {@link LogSink}, отправляющая батч на удалённый log-master по HTTP.
 * Регистрируется авто-конфигурацией, если в контексте нет другого {@link LogSink}.
 */
public class HttpLogSink implements LogSink {

    private static final String BATCH_PATH = "/api/logs/batch";

    private final RestClient restClient;
    private final ObjectMapper mapper;

    public HttpLogSink(LogClientProperties props) {
        this.mapper = new ObjectMapper()
                .registerModule(new JavaTimeModule())
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        this.restClient = RestClient.builder()
                .baseUrl(props.getServerUrl())
                .requestFactory(new JdkClientHttpRequestFactory())
                .build();
    }

    @Override
    public void send(List<LogEntryRequest> batch) {
        try {
            String body = mapper.writeValueAsString(batch);
            restClient.post()
                    .uri(BATCH_PATH)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(body)
                    .retrieve()
                    .toBodilessEntity();
        } catch (JsonProcessingException e) {
            //TODO log.error("[log-client] Failed to serialize batch", e);
        } catch (Exception e) {
           //TODO log.warn("[log-client] Failed to send batch: {}", e.getMessage());
        }
    }
}
