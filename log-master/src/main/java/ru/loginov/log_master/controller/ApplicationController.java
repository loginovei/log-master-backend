package ru.loginov.log_master.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import ru.loginov.log_master.data.dto.ApplicationRequest;
import ru.loginov.log_master.data.dto.ApplicationResponse;
import ru.loginov.log_master.service.ApplicationService;

import java.util.List;

@RestController
@RequestMapping("/api/applications")
@RequiredArgsConstructor
@Tag(name = "Applications", description = "Реестр приложений, чьи логи собирает система")
public class ApplicationController {

    private final ApplicationService service;

    @GetMapping
    @Operation(summary = "Список всех приложений")
    public ResponseEntity<List<ApplicationResponse>> findAll() {
        return ResponseEntity.ok(service.findAll());
    }

    @PostMapping
    @Operation(summary = "Зарегистрировать приложение")
    public ResponseEntity<ApplicationResponse> create(@Valid @RequestBody ApplicationRequest request) {
        return ResponseEntity.ok(service.create(request.code(), request.name()));
    }

    @GetMapping("/{code}")
    @Operation(summary = "Получить приложение по коду")
    public ResponseEntity<ApplicationResponse> getByCode(@PathVariable String code) {
        return ResponseEntity.ok(service.getByCode(code));
    }

    @PutMapping("/{code}")
    @Operation(summary = "Обновить имя приложения")
    public ResponseEntity<ApplicationResponse> update(@PathVariable String code,
                                                      @Valid @RequestBody ApplicationRequest request) {
        return ResponseEntity.ok(service.update(code, request.name()));
    }

    @DeleteMapping("/{code}")
    @Operation(summary = "Удалить приложение по коду")
    public ResponseEntity<Void> delete(@PathVariable String code) {
        service.delete(code);
        return ResponseEntity.noContent().build();
    }
}
