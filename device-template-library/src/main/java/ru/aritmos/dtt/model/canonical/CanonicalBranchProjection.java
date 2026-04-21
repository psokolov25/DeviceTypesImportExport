package ru.aritmos.dtt.model.canonical;

import java.util.Map;

/**
 * Каноническая проекция шаблона в branch-контекст.
 *
 * @param kind значение `type` для branch deviceType
 * @param deviceTypeParamValues восстановленные значения параметров типа устройства
 * @param deviceTypeParameterSchema каноническая схема параметров типа устройства
 * @param deviceParameterSchema каноническая схема параметров дочерних устройств
 */
public record CanonicalBranchProjection(String kind,
                                        Map<String, Object> deviceTypeParamValues,
                                        CanonicalParameterSchema deviceTypeParameterSchema,
                                        CanonicalParameterSchema deviceParameterSchema) {

    /**
     * Конструктор обратной совместимости.
     *
     * @param kind значение `type` для branch deviceType
     * @param deviceTypeParamValues значения параметров типа устройства
     */
    public CanonicalBranchProjection(String kind, Map<String, Object> deviceTypeParamValues) {
        this(kind, deviceTypeParamValues, null, null);
    }
}
