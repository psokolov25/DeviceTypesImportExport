package ru.aritmos.dtt.json.branch;

import ru.aritmos.dtt.api.dto.DeviceTypeTemplate;

import java.util.Map;

/**
 * Модель типа устройства в отделении с шаблонными параметрами и дочерними устройствами.
 *
 * @param template шаблон типа устройства
 * @param devices экземпляры устройств
 */
public record BranchDeviceType(DeviceTypeTemplate template, Map<String, DeviceInstanceTemplate> devices) {
}
