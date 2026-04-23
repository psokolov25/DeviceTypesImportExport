package ru.aritmos.dtt.api.dto.importplan;

import ru.aritmos.dtt.api.dto.DeviceTypeMetadata;

/**
 * Переопределение metadata одного типа устройства для конкретного branch
 * в сценарии одновременной сборки profile и branch.
 *
 * @param deviceTypeId идентификатор типа устройства в исходном profile
 * @param metadata metadata override, применяемый только внутри указанного branch
 */
public record BranchDeviceTypeMetadataOverrideImportRequest(
        String deviceTypeId,
        DeviceTypeMetadata metadata
) {
}
