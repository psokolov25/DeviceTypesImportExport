package ru.aritmos.dtt.api.dto.branch;

import java.util.List;

/**
 * Запрос на сборку одного отделения (branch) из набора типов устройств.
 *
 * @param branchId идентификатор отделения
 * @param displayName отображаемое имя отделения
 * @param deviceTypes список импортируемых типов устройств
 */
public record BranchImportRequest(
        String branchId,
        String displayName,
        List<BranchDeviceTypeImportRequest> deviceTypes
) {
}
