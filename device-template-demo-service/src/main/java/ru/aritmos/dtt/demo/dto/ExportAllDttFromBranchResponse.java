package ru.aritmos.dtt.demo.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.Map;

/**
 * Ответ с экспортированными DTT-архивами из branch equipment JSON.
 *
 * @param archivesBase64ByDeviceTypeId Base64-архивы по идентификатору deviceTypeId
 * @param exportedCount количество экспортированных архивов
 */
@Schema(description = "Результат экспорта всех DTT из branch equipment JSON")
public record ExportAllDttFromBranchResponse(
        @Schema(description = "Карта архивов DTT в Base64 по deviceTypeId")
        Map<String, String> archivesBase64ByDeviceTypeId,
        @Schema(description = "Количество экспортированных DTT", example = "2")
        int exportedCount
) {
}
