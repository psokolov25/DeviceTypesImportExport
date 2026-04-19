package ru.aritmos.dtt.api.dto;

import java.util.Map;

/**
 * Значения по умолчанию шаблона типа устройства.
 *
 * <p>DTO разделяет default values уровня типа устройства и уровня экземпляров
 * дочерних устройств, чтобы избежать смешения схемы и конкретных override-значений.</p>
 *
 * @param deviceTypeDefaults значения по умолчанию параметров типа устройства
 * @param deviceDefaults значения по умолчанию параметров устройств (deviceId -> параметры)
 */
public record TemplateDefaultValues(
        Map<String, Object> deviceTypeDefaults,
        Map<String, Map<String, Object>> deviceDefaults
) {
}
