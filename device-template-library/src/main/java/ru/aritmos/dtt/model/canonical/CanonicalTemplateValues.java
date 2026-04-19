package ru.aritmos.dtt.model.canonical;

import java.util.Map;

/**
 * Канонический контейнер значений шаблона (default/example/binding hints).
 *
 * @param values значения в нормализованной map-форме
 */
public record CanonicalTemplateValues(Map<String, Object> values) {
}

