package ru.aritmos.dtt.demo.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Результат сравнения версии, переданной пользователем, и версии из DTT.
 *
 * @param inputVersion версия, переданная параметром
 * @param dttVersion версия, прочитанная из DTT
 * @param greaterVersion большая версия
 * @param greaterSource источник большей версии: INPUT, DTT или EQUAL
 */
@Schema(description = "Результат сравнения версии из DTT и версии из параметра")
public record DttVersionComparisonResponse(
        @Schema(description = "Введённая версия", example = "2.1.0")
        String inputVersion,
        @Schema(description = "Версия из DTT", example = "2.0.5")
        String dttVersion,
        @Schema(description = "Большая версия", example = "2.1.0")
        String greaterVersion,
        @Schema(description = "Источник большей версии: INPUT, DTT или EQUAL", example = "INPUT")
        String greaterSource
) {
}
