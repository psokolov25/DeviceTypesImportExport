package ru.aritmos.dtt.demo.controller;

import io.micronaut.http.MediaType;
import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.Get;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import ru.aritmos.dtt.demo.dto.HealthResponse;

/**
 * Технический контроллер для быстрой проверки работоспособности demo-service.
 */
@Controller("/api/system")
@Tag(name = "System")
public class HealthController {

    /**
     * Возвращает состояние demo-service.
     *
     * @return статус работоспособности
     */
    @Get(uri = "/health", produces = MediaType.APPLICATION_JSON)
    @Operation(summary = "Проверить доступность demo-service")
    @ApiResponse(responseCode = "200", description = "Сервис доступен")
    public HealthResponse health() {
        return new HealthResponse("UP");
    }
}
