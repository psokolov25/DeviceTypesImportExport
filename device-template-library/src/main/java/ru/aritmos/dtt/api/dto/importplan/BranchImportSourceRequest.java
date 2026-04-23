package ru.aritmos.dtt.api.dto.importplan;

import java.util.List;

/**
 * Структурированное описание одного отделения для подготовки branch-импорта.
 *
 * @param branchId идентификатор отделения
 * @param displayName отображаемое имя отделения
 * @param deviceTypes набор импортируемых типов устройств
 */
public record BranchImportSourceRequest(
        String branchId,
        String displayName,
        List<BranchDeviceTypeImportSourceRequest> deviceTypes
) {
}
