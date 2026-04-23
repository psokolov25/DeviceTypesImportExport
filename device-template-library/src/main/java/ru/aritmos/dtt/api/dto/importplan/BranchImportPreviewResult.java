package ru.aritmos.dtt.api.dto.importplan;

import ru.aritmos.dtt.json.branch.BranchEquipment;

import java.util.Map;

/**
 * Детальный preview branch-импорта на уровне high-level import-plan.
 *
 * <p>Содержит одновременно:
 * <ul>
 *   <li>собранную preview-модель {@link BranchEquipment};</li>
 *   <li>диагностику вычисленных defaults/overrides по каждому ключу {@code branchId:deviceTypeId}.</li>
 * </ul>
 *
 * <p>DTO полезен для прикладных служб, которым нужен один вызов фасада вместо отдельной
 * последовательности {@code assembleBranch(...)} + {@code computeBranchImportPreview(...)}.
 *
 * @param branchEquipment preview-модель оборудования отделений
 * @param computationsByTarget сводка defaults/overrides по ключу {@code branchId:deviceTypeId}
 */
public record BranchImportPreviewResult(
        BranchEquipment branchEquipment,
        Map<String, ImportPreviewComputationEntry> computationsByTarget
) {
}
