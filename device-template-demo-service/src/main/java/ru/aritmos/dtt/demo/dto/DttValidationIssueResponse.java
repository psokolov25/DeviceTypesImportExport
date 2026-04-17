package ru.aritmos.dtt.demo.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Диагностическая запись валидации DTT для demo API.
 *
 * @param code код проблемы
 * @param path путь до проблемной секции
 * @param message описание проблемы
 */
@Schema(description = "Диагностическая проблема валидации DTT")
public record DttValidationIssueResponse(
        @Schema(example = "GROOVY_SYNTAX_ERROR") String code,
        @Schema(example = "scripts/onStartEvent.groovy") String path,
        @Schema(example = "Groovy-скрипт не компилируется") String message
) {
}
