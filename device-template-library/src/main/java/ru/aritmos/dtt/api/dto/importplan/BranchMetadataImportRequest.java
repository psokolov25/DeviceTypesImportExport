package ru.aritmos.dtt.api.dto.importplan;

import java.util.List;

/**
 * Описание branch для сценария одновременной сборки profile и branch
 * с наследованием metadata.
 *
 * @param branchId идентификатор branch
 * @param displayName отображаемое имя branch; если не задано, может быть вычислено из {@code branchId}
 * @param metadataOverrides branch-специфичные переопределения metadata по типам устройств
 */
public record BranchMetadataImportRequest(
        String branchId,
        String displayName,
        List<BranchDeviceTypeMetadataOverrideImportRequest> metadataOverrides
) {
}
