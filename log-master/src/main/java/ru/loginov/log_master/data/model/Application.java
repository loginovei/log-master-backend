package ru.loginov.log_master.data.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * OpenSearch document — индекс applications.
 *
 * Реестр приложений, чьи логи собирает система.
 * _id документа = code (например, auth-service).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Application {

    /** OpenSearch document _id (= code). */
    private String id;

    /** Уникальный технический код приложения (например, auth-service). */
    @JsonProperty("code")
    private String code;

    /** Отображаемое название приложения (например, Сервис аутентификации). */
    @JsonProperty("name")
    private String name;
}
