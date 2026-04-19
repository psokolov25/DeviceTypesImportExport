package ru.aritmos.dtt.demo.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import ru.aritmos.dtt.api.dto.MergeStrategy;

import java.util.List;

/**
 * Запрос на импорт набора DTT в уже существующий branch equipment JSON.
 *
 * @param existingBranchJson исходный JSON уровня DeviceManager.json
 * @param archivesBase64 список DTT-архивов в формате Base64
 * @param branchIds branch, в которые нужно импортировать переданные device types
 * @param mergeStrategy стратегия merge при конфликте deviceTypeId
 */
@Schema(description = "Запрос на merge-импорт DTT набора в существующее branch equipment JSON")
public record ImportDttSetToExistingBranchRequest(
        @Schema(description = "Исходный JSON уровня DeviceManager.json", requiredMode = Schema.RequiredMode.REQUIRED)
        String existingBranchJson,
        @Schema(description = "Список DTT архивов в Base64", requiredMode = Schema.RequiredMode.REQUIRED)
        List<String> archivesBase64,
        @Schema(description = "Список branch идентификаторов для импорта", requiredMode = Schema.RequiredMode.REQUIRED)
        List<String> branchIds,
        @Schema(description = "Merge-стратегия", defaultValue = "FAIL_IF_EXISTS")
        MergeStrategy mergeStrategy
) {
}
