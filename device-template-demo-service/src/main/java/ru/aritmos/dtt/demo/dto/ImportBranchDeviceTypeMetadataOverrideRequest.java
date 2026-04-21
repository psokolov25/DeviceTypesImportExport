package ru.aritmos.dtt.demo.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Metadata override типа устройства для конкретного отделения.
 *
 * @param deviceTypeId id типа устройства
 * @param metadata metadata override
 */
@Schema(description = "Metadata override для типа устройства в конкретном branch")
public record ImportBranchDeviceTypeMetadataOverrideRequest(
        @Schema(description = "Идентификатор типа устройства", requiredMode = Schema.RequiredMode.REQUIRED)
        String deviceTypeId,
        @Schema(description = "Значения metadata override", requiredMode = Schema.RequiredMode.REQUIRED)
        ImportDeviceTypeMetadataOverrideRequest metadata
) {
}
