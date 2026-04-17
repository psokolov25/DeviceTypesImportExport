package ru.aritmos.dtt.demo.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Результат импорта набора DTT-архивов в JSON профиля оборудования.
 *
 * @param profileJson собранный profile JSON
 * @param deviceTypesCount количество device types в собранном профиле
 */
@Schema(description = "Результат импорта DTT-архивов в профиль оборудования")
public record ImportDttSetToProfileResponse(
        @Schema(description = "Собранный JSON карты deviceTypes")
        String profileJson,
        @Schema(description = "Количество импортированных типов", example = "2")
        int deviceTypesCount
) {
}
