package ru.aritmos.dtt.model.canonical;

import java.util.Map;

/**
 * Каноническая проекция шаблона в модель profile JSON (`deviceTypeParamValues`).
 *
 * @param deviceTypeParamValues восстановленные значения параметров типа устройства с metadata
 */
public record CanonicalProfileProjection(Map<String, Object> deviceTypeParamValues) {
}

