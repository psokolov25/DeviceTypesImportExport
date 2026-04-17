package ru.aritmos.dtt.model.canonical;

import java.util.Map;

/**
 * Каноническая внутренняя модель одного шаблона типа устройства.
 *
 * @param formatVersion версия формата DTT
 * @param metadata метаданные типа устройства
 * @param deviceTypeParameterSchema схема параметров типа устройства
 * @param deviceParameterSchema схема параметров экземпляров устройств
 * @param bindingHints подсказки биндинга
 * @param defaultValues значения по умолчанию
 * @param exampleValues примерные значения
 * @param scripts набор Groovy-скриптов
 */
public record CanonicalDeviceTypeTemplate(
        String formatVersion,
        CanonicalDeviceTypeMetadata metadata,
        Map<String, Object> deviceTypeParameterSchema,
        Map<String, Object> deviceParameterSchema,
        Map<String, Object> bindingHints,
        Map<String, Object> defaultValues,
        Map<String, Object> exampleValues,
        CanonicalScriptSet scripts
) {
}
