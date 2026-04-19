package ru.aritmos.dtt.api.dto;

import java.util.Map;

/**
 * Схема параметров шаблона типа устройства.
 *
 * <p>Содержит набор определений параметров верхнего уровня и может использоваться
 * как для параметров типа устройства, так и для параметров дочерних устройств.</p>
 *
 * @param parametersMap карта параметров по их системному имени
 */
public record TemplateParameterSchema(
        Map<String, TemplateParameterDefinition> parametersMap
) {
}
