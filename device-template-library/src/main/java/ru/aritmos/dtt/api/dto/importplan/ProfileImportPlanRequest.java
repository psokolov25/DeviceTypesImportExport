package ru.aritmos.dtt.api.dto.importplan;

import ru.aritmos.dtt.api.dto.MergeStrategy;

import java.util.List;

/**
 * Запрос на подготовку profile-импорта из DTT-архивов.
 *
 * <p>Поддерживает два режима:
 * <ul>
 *     <li>legacy-режим через {@code archivesBase64};</li>
 *     <li>структурированный режим через {@code deviceTypes}, где для каждого DTT можно задать
 *     override metadata и значений параметров.</li>
 * </ul>
 *
 * @param archivesBase64 список DTT-архивов в Base64 для legacy-режима
 * @param mergeStrategy merge-стратегия при совпадении deviceTypeId
 * @param deviceTypes структурированное описание импортируемых типов устройств
 */
public record ProfileImportPlanRequest(
        List<String> archivesBase64,
        MergeStrategy mergeStrategy,
        List<ProfileDeviceTypeImportSourceRequest> deviceTypes
) {
}
