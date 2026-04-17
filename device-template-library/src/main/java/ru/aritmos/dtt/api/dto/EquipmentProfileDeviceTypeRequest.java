package ru.aritmos.dtt.api.dto;

/**
 * Запрос на включение одного типа устройства из DTT в профиль оборудования.
 *
 * @param template шаблон типа устройства
 * @param useDefaultValues флаг использования значений по умолчанию из шаблона
 */
public record EquipmentProfileDeviceTypeRequest(DeviceTypeTemplate template, boolean useDefaultValues) {
}
