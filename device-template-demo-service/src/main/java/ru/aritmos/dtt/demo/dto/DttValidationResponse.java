package ru.aritmos.dtt.demo.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

/**
 * Ответ demo endpoint-а валидации DTT-архива.
 *
 * @param valid общий флаг валидности
 * @param issues найденные диагностические проблемы
 */
@Schema(description = "Результат валидации DTT")
public record DttValidationResponse(
        @Schema(example = "true") boolean valid,
        List<DttValidationIssueResponse> issues
) {
}
