package ru.aritmos.dtt.demo.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.Map;

/**
 * Запрос на импорт одного DTT в профиль оборудования с возможностью переопределить
 * значения параметров и metadata типа устройства.
 *
 * @param archiveBase64 DTT-архив в Base64
 * @param deviceTypeParamValues override-значения параметров типа устройства
 * @param metadataOverride override metadata типа устройства на уровне profile
 */
@Schema(description = "Импорт одного DTT в профиль оборудования с override-значениями")
public record ImportProfileDeviceTypeRequest(
        @Schema(description = "DTT-архив в Base64", requiredMode = Schema.RequiredMode.REQUIRED)
        String archiveBase64,
        @Schema(description = "Override-значения параметров типа устройства")
        Map<String, Object> deviceTypeParamValues,
        @Schema(description = "Override метаданных типа устройства на уровне profile")
        ImportDeviceTypeMetadataOverrideRequest metadataOverride
) {
}
