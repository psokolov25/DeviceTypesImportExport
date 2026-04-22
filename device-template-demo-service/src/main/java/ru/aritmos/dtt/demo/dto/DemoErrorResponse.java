package ru.aritmos.dtt.demo.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Структурированный ответ об ошибке demo API.
 *
 * @param code машинно-читаемый код ошибки
 * @param message человеко-читаемое сообщение
 */
@Schema(description = "Структурированная ошибка demo API")
public record DemoErrorResponse(
        @Schema(example = "INVALID_ARGUMENT")
        String code,
        @Schema(example = "archivesBase64 must contain at least one DTT archive")
        String message
) {
}
