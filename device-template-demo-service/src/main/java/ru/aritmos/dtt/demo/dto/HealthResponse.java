package ru.aritmos.dtt.demo.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * DTO ответа health-проверки demo-service.
 *
 * @param status итоговый статус доступности сервиса
 */
@Schema(description = "Результат проверки доступности demo-service")
public record HealthResponse(
        @Schema(description = "Статус сервиса", example = "UP")
        String status
) {
}
