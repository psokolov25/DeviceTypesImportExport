package ru.aritmos.dtt.api.dto.branch;

import ru.aritmos.dtt.api.dto.MergeStrategy;

import java.util.List;

/**
 * Запрос на сборку полного branch equipment JSON из одного или нескольких DTT.
 *
 * @param branches список отделений для сборки
 * @param mergeStrategy стратегия разрешения конфликтов типов устройств внутри отделения
 */
public record BranchEquipmentAssemblyRequest(
        List<BranchImportRequest> branches,
        MergeStrategy mergeStrategy
) {
}
