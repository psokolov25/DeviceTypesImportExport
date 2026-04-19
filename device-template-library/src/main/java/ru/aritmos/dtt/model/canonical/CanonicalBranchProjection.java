package ru.aritmos.dtt.model.canonical;

import java.util.Map;

/**
 * Каноническая проекция шаблона в branch-контекст.
 *
 * @param kind значение `type` для branch deviceType
 * @param deviceTypeParamValues восстановленные значения параметров типа устройства
 */
public record CanonicalBranchProjection(String kind, Map<String, Object> deviceTypeParamValues) {
}

