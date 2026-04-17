package ru.aritmos.dtt.json.branch;

import java.util.Map;

/**
 * Модель экземпляра устройства внутри branch equipment JSON.
 *
 * @param id идентификатор устройства
 * @param name системное имя устройства
 * @param displayName отображаемое имя
 * @param description описание
 * @param deviceParamValues значения параметров устройства
 */
public record DeviceInstanceTemplate(
        String id,
        String name,
        String displayName,
        String description,
        Map<String, Object> deviceParamValues
) {
}
