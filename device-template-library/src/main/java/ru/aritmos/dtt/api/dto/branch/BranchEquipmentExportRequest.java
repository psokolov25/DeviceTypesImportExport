package ru.aritmos.dtt.api.dto.branch;

import ru.aritmos.dtt.api.dto.MergeStrategy;
import ru.aritmos.dtt.json.branch.BranchEquipment;

import java.util.List;

/**
 * Запрос на экспорт набора DTT-архивов из branch equipment JSON модели.
 *
 * @param branchEquipment исходная модель branch equipment
 * @param branchIds опциональный список branchId для выборочного экспорта; если null/пустой — все branch
 * @param deviceTypeIds опциональный список deviceTypeId для выборочного экспорта; если null/пустой — все типы
 * @param mergeStrategy стратегия разрешения конфликтов deviceTypeId между branch
 * @param dttVersion опциональная версия шаблона, которая фиксируется в DTT и добавляется в описание типа
 */
public record BranchEquipmentExportRequest(
        BranchEquipment branchEquipment,
        List<String> branchIds,
        List<String> deviceTypeIds,
        MergeStrategy mergeStrategy,
        String dttVersion
) {
}
