package ru.aritmos.dtt.demo.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import ru.aritmos.dtt.json.profile.EquipmentProfile;

import java.util.List;

/**
 * Запрос на экспорт всех DTT-архивов из profile JSON.
 *
 * @param profile модель profile JSON (карта deviceTypes)
 * @param profileJson строковое представление profile JSON (альтернативно полю profile)
 * @param deviceTypeIds опциональный список deviceTypeId для выборочного экспорта
 */
@Schema(description = "Запрос на экспорт всех DTT из profile JSON")
public record ExportAllDttFromProfileRequest(
        @Schema(description = "Модель profile JSON", implementation = EquipmentProfile.class)
        EquipmentProfile profile,
        @Schema(description = "Строковое представление profile JSON (альтернативно полю profile)")
        String profileJson,
        @Schema(description = "Опциональный список deviceTypeId для выборочного экспорта", example = "[\"display\",\"cashbox\"]")
        List<String> deviceTypeIds
) {
}
