package ru.aritmos.dtt.demo.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.Map;

/**
 * Результат экспорта всех DTT-архивов из profile JSON.
 *
 * @param archivesBase64ByDeviceTypeId карта deviceTypeId -> DTT-архив в Base64
 * @param exportedCount количество экспортированных архивов
 */
@Schema(description = "Результат экспорта набора DTT из profile JSON")
public record ExportAllDttFromProfileResponse(
        @Schema(description = "Карта архивов в Base64 по deviceTypeId")
        Map<String, String> archivesBase64ByDeviceTypeId,
        @Schema(description = "Количество экспортированных архивов", example = "2")
        int exportedCount
) {
}
