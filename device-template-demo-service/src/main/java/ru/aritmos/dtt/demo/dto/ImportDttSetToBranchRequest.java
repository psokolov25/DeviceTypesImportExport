package ru.aritmos.dtt.demo.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import ru.aritmos.dtt.api.dto.MergeStrategy;

import java.util.List;

/**
 * Запрос на импорт одного или нескольких DTT-архивов в branch equipment JSON.
 *
 * @param archivesBase64 список DTT-архивов в Base64
 * @param branchIds идентификаторы отделений, в которые будут импортированы все шаблоны
 * @param mergeStrategy стратегия merge при совпадении deviceTypeId
 */
@Schema(description = "Запрос на импорт набора DTT в branch equipment JSON")
public record ImportDttSetToBranchRequest(
        @Schema(description = "Список DTT-архивов в Base64", example = "[\"UEsDB...\"]")
        List<String> archivesBase64,
        @Schema(description = "Список идентификаторов отделений", example = "[\"branch-1\",\"branch-2\"]")
        List<String> branchIds,
        @Schema(description = "Стратегия merge", example = "FAIL_IF_EXISTS")
        MergeStrategy mergeStrategy
) {
}
