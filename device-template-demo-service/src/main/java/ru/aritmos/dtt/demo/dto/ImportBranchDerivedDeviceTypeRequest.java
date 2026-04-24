package ru.aritmos.dtt.demo.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;
import java.util.Map;

/**
 * Переопределение derived-типа устройства внутри branch для сценария
 * одновременной сборки profile+branch.
 *
 * @param profileDeviceTypeId идентификатор типа устройства, ранее сформированного в profile
 * @param deviceTypeParamValues branch-уровневые override-значения параметров типа устройства
 * @param metadataOverride branch-уровневый override metadata типа устройства
 * @param devices branch-уровневые override-описания устройств данного типа
 * @param kind override поля {@code type} в branch JSON
 */
@Schema(description = "Переопределение derived-типа устройства для branch в profile+branch сценарии")
public record ImportBranchDerivedDeviceTypeRequest(
        @Schema(description = "Идентификатор типа устройства из profile", requiredMode = Schema.RequiredMode.REQUIRED)
        String profileDeviceTypeId,
        @Schema(description = "Override-значения параметров типа устройства")
        Map<String, Object> deviceTypeParamValues,
        @Schema(description = "Override metadata типа устройства на уровне branch")
        ImportDeviceTypeMetadataOverrideRequest metadataOverride,
        @Schema(description = "Override-описания устройств данного типа")
        List<ImportBranchDeviceRequest> devices,
        @Schema(description = "Override поля type для branch JSON")
        String kind
) {
}
