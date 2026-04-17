package ru.aritmos.dtt.demo.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Запрос на экспорт всех DTT-архивов из profile JSON.
 *
 * @param profileJson JSON карты deviceTypes
 */
@Schema(description = "Запрос на экспорт всех DTT из profile JSON")
public record ExportAllDttFromProfileRequest(
        @Schema(description = "JSON карты deviceTypes")
        String profileJson
) {
}
