package ru.aritmos.dtt.model.canonical;

import java.util.Map;

/**
 * Каноническая схема параметров, представленная типизированной картой определений.
 *
 * @param parameters определения параметров по их имени
 */
public record CanonicalParameterSchema(Map<String, CanonicalParameterDefinition> parameters) {
}

