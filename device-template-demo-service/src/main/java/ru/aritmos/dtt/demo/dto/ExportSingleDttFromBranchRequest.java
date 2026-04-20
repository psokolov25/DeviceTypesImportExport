package ru.aritmos.dtt.demo.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import ru.aritmos.dtt.api.dto.MergeStrategy;
import ru.aritmos.dtt.json.branch.BranchEquipment;

import java.util.List;

/**
 * Запрос на экспорт одного DTT-архива из branch equipment JSON.
 *
 * @param branchEquipment модель branch equipment JSON
 * @param branchJson строковое представление branch equipment JSON (альтернатива branchEquipment)
 * @param branchIds опциональный фильтр branchId
 * @param deviceTypeId идентификатор экспортируемого типа устройства
 * @param mergeStrategy стратегия merge при конфликте одинакового deviceTypeId между branch
 * @param dttVersion опциональная версия DTT, фиксируемая в архиве
 */
@Schema(description = "Запрос на экспорт одного DTT из branch equipment JSON")
public record ExportSingleDttFromBranchRequest(
        @Schema(description = "Модель branch equipment JSON", implementation = BranchEquipment.class)
        BranchEquipment branchEquipment,
        @Schema(description = "Строковое представление branch equipment JSON (альтернатива branchEquipment)")
        String branchJson,
        @Schema(description = "Опциональный фильтр branchId", example = "[\"branch-1\"]")
        List<String> branchIds,
        @Schema(description = "Идентификатор экспортируемого типа устройства", example = "display")
        String deviceTypeId,
        @Schema(description = "Стратегия merge при конфликте deviceTypeId", example = "FAIL_IF_EXISTS")
        MergeStrategy mergeStrategy,
        @Schema(description = "Версия DTT, которая фиксируется в архиве", example = "2.1.0")
        String dttVersion
) {
}
