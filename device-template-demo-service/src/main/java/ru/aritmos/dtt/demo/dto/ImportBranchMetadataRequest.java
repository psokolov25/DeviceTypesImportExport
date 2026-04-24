package ru.aritmos.dtt.demo.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

/**
 * Описание отделения для сценария одновременной сборки profile/branch.
 *
 * @param branchId идентификатор отделения
 * @param metadataOverrides переопределения metadata типов устройств внутри отделения
 * @param deviceTypeOverrides переопределения derived-типов и устройств внутри отделения
 */
@Schema(description = "Параметры branch c metadata override")
public record ImportBranchMetadataRequest(
        @Schema(description = "Идентификатор отделения", requiredMode = Schema.RequiredMode.REQUIRED)
        String branchId,
        @Schema(description = "Metadata override типов устройств в отделении")
        List<ImportBranchDeviceTypeMetadataOverrideRequest> metadataOverrides,
        @Schema(description = "Переопределения derived-типов устройств и наборов устройств внутри отделения")
        List<ImportBranchDerivedDeviceTypeRequest> deviceTypeOverrides
) {
}
