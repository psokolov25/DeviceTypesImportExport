package ru.aritmos.dtt.demo.dto;

import com.fasterxml.jackson.databind.JsonNode;
import io.swagger.v3.oas.annotations.media.Schema;
import ru.aritmos.dtt.json.profile.EquipmentProfile;

/**
 * Запрос на экспорт одного DTT-архива из profile JSON.
 *
 * @param profile модель profile JSON
 * @param profileJson profile JSON как объект (альтернатива полю profile)
 * @param deviceTypeId идентификатор экспортируемого типа устройства
 * @param dttVersion опциональная версия DTT, фиксируемая в архиве
 */
@Schema(description = "Запрос на экспорт одного DTT из profile JSON")
public record ExportSingleDttFromProfileRequest(
        @Schema(description = "Модель profile JSON", implementation = EquipmentProfile.class)
        EquipmentProfile profile,
        @Schema(description = "Profile JSON как объект (альтернатива profile)", type = "object")
        JsonNode profileJson,
        @Schema(description = "Идентификатор экспортируемого типа устройства", example = "display")
        String deviceTypeId,
        @Schema(description = "Версия DTT, которая фиксируется в архиве", example = "2.1.0")
        String dttVersion
) {
}
