package ru.aritmos.dtt.demo.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import ru.aritmos.dtt.json.profile.EquipmentProfile;

/**
 * Запрос на экспорт одного DTT-архива из profile JSON.
 *
 * @param profile модель profile JSON
 * @param profileJson строковое представление profile JSON (альтернатива полю profile)
 * @param deviceTypeId идентификатор экспортируемого типа устройства
 * @param dttVersion опциональная версия DTT, фиксируемая в архиве
 */
@Schema(description = "Запрос на экспорт одного DTT из profile JSON")
public record ExportSingleDttFromProfileRequest(
        @Schema(description = "Модель profile JSON", implementation = EquipmentProfile.class)
        EquipmentProfile profile,
        @Schema(description = "Строковое представление profile JSON (альтернатива profile)")
        String profileJson,
        @Schema(description = "Идентификатор экспортируемого типа устройства", example = "display")
        String deviceTypeId,
        @Schema(description = "Версия DTT, которая фиксируется в архиве", example = "2.1.0")
        String dttVersion
) {
}
