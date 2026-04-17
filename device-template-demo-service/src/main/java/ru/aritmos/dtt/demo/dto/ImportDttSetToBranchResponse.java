package ru.aritmos.dtt.demo.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Результат импорта набора DTT в branch equipment JSON.
 *
 * @param branchJson сериализованный branch equipment JSON
 * @param branchesCount количество отделений в результате сборки
 */
@Schema(description = "Ответ с branch equipment JSON после импорта набора DTT")
public record ImportDttSetToBranchResponse(
        @Schema(description = "Собранный branch equipment JSON (карта branchId -> branch node)", example = "{\"branch-1\":{...}}")
        String branchJson,
        @Schema(description = "Количество отделений", example = "2")
        int branchesCount
) {
}
