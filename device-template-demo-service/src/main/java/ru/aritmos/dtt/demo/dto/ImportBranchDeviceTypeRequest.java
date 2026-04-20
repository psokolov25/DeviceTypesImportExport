package ru.aritmos.dtt.demo.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;
import java.util.Map;

/**
 * Запрос на импорт одного DTT в конкретное отделение с override-значениями параметров типа устройства и устройств.
 *
 * @param archiveBase64 DTT-архив в Base64
 * @param deviceTypeParamValues override-значения параметров типа устройства
 * @param devices override-описания устройств данного типа
 * @param kind override поля {@code type} в branch JSON
 */
@Schema(description = "Импорт одного DTT в отделение с override-значениями типа устройства и устройств")
public record ImportBranchDeviceTypeRequest(
        @Schema(description = "DTT-архив в Base64", requiredMode = Schema.RequiredMode.REQUIRED)
        String archiveBase64,
        @Schema(description = "Override-значения параметров типа устройства")
        Map<String, Object> deviceTypeParamValues,
        @Schema(description = "Override-описания устройств данного типа")
        List<ImportBranchDeviceRequest> devices,
        @Schema(description = "Override поля type для branch JSON")
        String kind
) {
}
