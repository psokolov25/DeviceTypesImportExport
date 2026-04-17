package ru.aritmos.dtt.demo.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import ru.aritmos.dtt.api.dto.MergeStrategy;

import java.util.List;

/**
 * Запрос на импорт одного или нескольких DTT-архивов в profile JSON.
 *
 * @param archivesBase64 список DTT-архивов в Base64
 * @param mergeStrategy стратегия merge при совпадении deviceTypeId
 */
@Schema(description = "Запрос на импорт набора DTT в профиль оборудования")
public record ImportDttSetToProfileRequest(
        @Schema(
                description = "Список DTT-архивов в Base64",
                example = "[\"UEsDB...\"]"
        )
        List<String> archivesBase64,
        @Schema(description = "Стратегия merge", example = "FAIL_IF_EXISTS")
        MergeStrategy mergeStrategy
) {
}
