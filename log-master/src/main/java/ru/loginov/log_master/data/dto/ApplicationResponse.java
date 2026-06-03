package ru.loginov.log_master.data.dto;

import ru.loginov.log_master.data.model.Application;

/** Ответ API для зарегистрированного приложения. */
public record ApplicationResponse(
        String id,
        String code,
        String name
) {
    public static ApplicationResponse from(Application app) {
        return new ApplicationResponse(app.getId(), app.getCode(), app.getName());
    }
}
