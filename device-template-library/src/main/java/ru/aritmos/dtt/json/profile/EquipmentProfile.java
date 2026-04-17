package ru.aritmos.dtt.json.profile;

import ru.aritmos.dtt.api.dto.DeviceTypeTemplate;

import java.util.Map;

/**
 * Модель JSON профиля оборудования — только карта {@code deviceTypes} без branch-обёртки.
 *
 * @param deviceTypes собранные типы устройств
 */
public record EquipmentProfile(Map<String, DeviceTypeTemplate> deviceTypes) {
}
