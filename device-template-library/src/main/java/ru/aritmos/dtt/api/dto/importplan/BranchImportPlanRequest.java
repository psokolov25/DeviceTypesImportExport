package ru.aritmos.dtt.api.dto.importplan;

import ru.aritmos.dtt.api.dto.MergeStrategy;

import java.util.List;

/**
 * Запрос на подготовку branch-импорта из DTT-архивов.
 *
 * <p>Поддерживает два режима:
 * <ul>
 *     <li>legacy-режим через {@code archivesBase64 + branchIds}, когда все DTT импортируются во все branch;</li>
 *     <li>структурированный режим через {@code branches}, где каждое отделение содержит собственный
 *     набор DTT и override-значения.</li>
 * </ul>
 *
 * @param archivesBase64 список DTT-архивов в Base64 для legacy-режима
 * @param branchIds список branchId для legacy-режима
 * @param mergeStrategy merge-стратегия при совпадении deviceTypeId
 * @param branches структурированное описание отделений и используемых типов устройств
 */
public record BranchImportPlanRequest(
        List<String> archivesBase64,
        List<String> branchIds,
        MergeStrategy mergeStrategy,
        List<BranchImportSourceRequest> branches
) {
}
