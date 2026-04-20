package ru.aritmos.dtt.demo.dto;

import com.fasterxml.jackson.databind.JsonNode;
import io.swagger.v3.oas.annotations.media.Schema;
import ru.aritmos.dtt.json.profile.EquipmentProfile;

import java.util.List;

/**
 * Запрос на экспорт всех DTT-архивов из profile JSON.
 *
 * @param profile модель profile JSON (карта deviceTypes)
 * @param profileJson profile JSON как объект (альтернативно полю profile)
 * @param deviceTypeIds опциональный список deviceTypeId для выборочного экспорта
 * @param dttVersion опциональная версия, которая фиксируется в DTT и добавляется в description типа
 */
@Schema(description = "Запрос на экспорт всех DTT из profile JSON")
public record ExportAllDttFromProfileRequest(
        @Schema(description = "Модель profile JSON", implementation = EquipmentProfile.class)
        EquipmentProfile profile,
        @Schema(description = "Profile JSON как объект (альтернативно profile)", type = "object")
        JsonNode profileJson,
        @Schema(description = "Опциональный список deviceTypeId для выборочного экспорта", example = "[\"display\",\"cashbox\"]")
        List<String> deviceTypeIds,
        @Schema(description = "Версия DTT, которая фиксируется в архиве", example = "2.1.0")
        String dttVersion
) {
}
