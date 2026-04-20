package ru.aritmos.dtt.demo.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Ответ с одним экспортированным DTT-архивом.
 *
 * @param deviceTypeId идентификатор экспортированного типа устройства
 * @param archiveBase64 DTT-архив в Base64
 */
@Schema(description = "Результат экспорта одного DTT-архива")
public record ExportSingleDttResponse(
        @Schema(description = "Идентификатор экспортированного типа устройства", example = "display")
        String deviceTypeId,
        @Schema(description = "DTT-архив в Base64")
        String archiveBase64
) {
}
