package ru.loginov.log_master.data.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import ru.loginov.log_master.data.enums.LogLevel;

import java.util.Map;

/**
 * OpenSearch document — индекс log-templates.
 * <p>
 * Один документ = один шаблон лога для одного приложения.
 * Мультиязычность хранится в поле messages (lang -> text).
 * _id документа = code (например, AUTH_001).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LogTemplate {

    /** OpenSearch document _id (= code). */
    private String id;

    /** Уникальный код шаблона, например AUTH_001. */
    @JsonProperty("code")
    private String code;

    /** Уровень логирования, который задаётся для шаблона. */
    @JsonProperty("level")
    private LogLevel level;

    /** Код приложения, которому принадлежит шаблон. */
    @JsonProperty("applicationCode")
    private String applicationCode;

    /**
     * Тексты шаблона по языкам.
     * Ключ — BCP-47 тег языка (ru, en, ...).
     * Значение — шаблон с позиционными плейсхолдерами {0}, {1}, ...
     */
    @JsonProperty("messages")
    private Map<String, String> messages;
}
