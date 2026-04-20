package ru.aritmos.dtt.demo.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import ru.aritmos.dtt.api.dto.MergeStrategy;

import java.util.List;

/**
 * Запрос на импорт одного или нескольких DTT-архивов в profile JSON.
 *
 * @param archivesBase64 список DTT-архивов в Base64 в legacy-формате
 * @param mergeStrategy стратегия merge при совпадении deviceTypeId
 * @param deviceTypes структурированные запросы импорта с override-значениями параметров типа устройства
 */
@Schema(description = "Запрос на импорт набора DTT в профиль оборудования")
public record ImportDttSetToProfileRequest(
        @Schema(
                description = "Список DTT-архивов в Base64 (legacy-формат без override-значений)",
                example = "[\"UEsDB...\"]"
        )
        List<String> archivesBase64,
        @Schema(description = "Стратегия merge", example = "FAIL_IF_EXISTS")
        MergeStrategy mergeStrategy,
        @Schema(description = "Структурированные запросы импорта с override-значениями параметров типа устройства")
        List<ImportProfileDeviceTypeRequest> deviceTypes
) {
    public ImportDttSetToProfileRequest(List<String> archivesBase64, MergeStrategy mergeStrategy) {
        this(archivesBase64, mergeStrategy, List.of());
    }
}
