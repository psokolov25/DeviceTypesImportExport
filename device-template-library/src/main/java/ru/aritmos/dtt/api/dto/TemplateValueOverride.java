package ru.aritmos.dtt.api.dto;

import java.util.Map;

/**
 * Явные override-значения параметров шаблона для конкретного типа устройства.
 *
 * @param deviceTypeId идентификатор типа устройства
 * @param values карта override-значений
 */
public record TemplateValueOverride(String deviceTypeId, Map<String, Object> values) {
}
