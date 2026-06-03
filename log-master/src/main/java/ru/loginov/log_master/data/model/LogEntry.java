package ru.loginov.log_master.data.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import ru.loginov.log_master.data.enums.LogLevel;

import java.time.Instant;
import java.util.Map;

/**
 * OpenSearch document — индекс log-entries.
 * <p>
 * Один документ = одно событие логирования.
 * level денормализован из шаблона для эффективной фильтрации без join-а.
 * _id генерируется OpenSearch автоматически (UUID).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class LogEntry {

    /** OpenSearch document _id. */
    private String id;

    /** Код шаблона лога (ссылка на LogTemplate.code). */
    @JsonProperty("code")
    private String code;

    /** Код приложения/сервиса, сгенерировавшего запись. */
    @JsonProperty("applicationCode")
    private String applicationCode;

    /**
     * Уровень лога — денормализован из LogTemplate для фильтрации.
     * Заполняется сервисом при сохранении на основе level шаблона.
     */
    @JsonProperty("level")
    private LogLevel level;

    /** Момент возникновения события. */
    @JsonProperty("created")
    private Instant created;

    /** Stack trace исключения (если есть). */
    @JsonProperty("exception")
    private String exception;

    /** Дополнительный контекст в произвольном формате. */
    @JsonProperty("additional")
    private JsonNode additional;

    /**
     * Позиционные аргументы для подстановки в шаблон.
     * Ключ — строковый индекс ("0", "1", ...), значение — аргумент.
     */
    @JsonProperty("args")
    private Map<String, Object> args;
}
