package ru.aritmos.dtt.demo.dto;

import com.fasterxml.jackson.databind.JsonNode;
import io.swagger.v3.oas.annotations.media.Schema;
import ru.aritmos.dtt.api.dto.MergeStrategy;
import ru.aritmos.dtt.json.branch.BranchEquipment;

import java.util.List;

/**
 * Запрос на экспорт всех DTT-архивов из branch equipment JSON.
 *
 * @param branchEquipment branch equipment JSON-модель
 * @param branchJson branch equipment JSON как объект (альтернативно полю branchEquipment)
 * @param branchIds опциональный список branchId для выборочного экспорта
 * @param deviceTypeIds опциональный список deviceTypeId для выборочного экспорта
 * @param mergeStrategy стратегия разрешения конфликтов deviceTypeId между отделениями
 * @param dttVersion опциональная версия, которая фиксируется в DTT и добавляется в description типа
 */
@Schema(description = "Запрос на экспорт набора DTT из branch equipment JSON")
public record ExportAllDttFromBranchRequest(
        @Schema(description = "Модель branch equipment JSON", implementation = BranchEquipment.class)
        BranchEquipment branchEquipment,
        @Schema(description = "Branch equipment JSON как объект (альтернативно branchEquipment)", type = "object")
        JsonNode branchJson,
        @Schema(description = "Опциональный список branchId для выборочного экспорта", example = "[\"branch-1\",\"branch-2\"]")
        List<String> branchIds,
        @Schema(description = "Опциональный список deviceTypeId для выборочного экспорта", example = "[\"display\"]")
        List<String> deviceTypeIds,
        @Schema(description = "Стратегия merge при повторении deviceTypeId", example = "FAIL_IF_EXISTS")
        MergeStrategy mergeStrategy,
        @Schema(description = "Версия DTT, которая фиксируется в архиве", example = "2.1.0")
        String dttVersion
) {
}
