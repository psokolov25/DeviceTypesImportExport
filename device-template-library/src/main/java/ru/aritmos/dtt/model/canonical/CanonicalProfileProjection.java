package ru.aritmos.dtt.model.canonical;

import java.util.Map;

/**
 * Каноническая проекция шаблона в модель profile JSON (`deviceTypeParamValues`).
 *
 * @param deviceTypeParamValues восстановленные значения параметров типа устройства с metadata
 * @param deviceTypeParameterSchema каноническая схема параметров типа устройства
 */
public record CanonicalProfileProjection(Map<String, Object> deviceTypeParamValues,
                                         CanonicalParameterSchema deviceTypeParameterSchema) {

    /**
     * Конструктор обратной совместимости.
     *
     * @param deviceTypeParamValues восстановленные значения параметров типа устройства
     */
    public CanonicalProfileProjection(Map<String, Object> deviceTypeParamValues) {
        this(deviceTypeParamValues, null);
    }
}
