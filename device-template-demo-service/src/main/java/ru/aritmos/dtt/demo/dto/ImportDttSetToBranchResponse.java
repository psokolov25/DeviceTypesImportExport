package ru.aritmos.dtt.demo.dto;

import com.fasterxml.jackson.databind.JsonNode;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Результат импорта набора DTT в branch equipment JSON.
 *
 * @param branchJson branch equipment JSON как объект
 * @param branchesCount количество отделений в результате сборки
 * @param metadata агрегированный список метаданных типов устройств
 */
@Schema(description = "Ответ с branch equipment JSON после импорта набора DTT")
public record ImportDttSetToBranchResponse(
        @Schema(description = "Собранный branch equipment JSON (карта branchId -> branch node) как объект")
        JsonNode branchJson,
        @Schema(description = "Количество отделений", example = "2")
        int branchesCount,
        @Schema(description = "Метаданные типов устройств")
        java.util.List<DeviceTypeBasicMetadataResponse> metadata
) {
}
