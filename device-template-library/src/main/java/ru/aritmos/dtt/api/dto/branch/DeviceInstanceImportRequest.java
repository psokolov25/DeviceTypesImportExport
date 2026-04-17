package ru.aritmos.dtt.api.dto.branch;

import java.util.Map;

/**
 * Запрос на импорт одного экземпляра устройства в тип устройства внутри branch equipment JSON.
 *
 * @param id идентификатор устройства
 * @param name системное имя устройства
 * @param displayName отображаемое имя
 * @param description описание устройства
 * @param deviceParamValues значения параметров устройства
 */
public record DeviceInstanceImportRequest(
        String id,
        String name,
        String displayName,
        String description,
        Map<String, Object> deviceParamValues
) {
}
