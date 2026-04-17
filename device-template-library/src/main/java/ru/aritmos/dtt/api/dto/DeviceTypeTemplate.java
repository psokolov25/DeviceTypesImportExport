package ru.aritmos.dtt.api.dto;

import java.util.Map;

/**
 * Каноническое публичное представление одного шаблона типа устройства (одного DTT).
 *
 * @param metadata метаданные типа устройства
 * @param deviceTypeParamValues значения параметров типа устройства
 */
public record DeviceTypeTemplate(DeviceTypeMetadata metadata, Map<String, Object> deviceTypeParamValues) {
}
