package ru.aritmos.dtt.model.canonical;

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
 * @param templateOrigin происхождение шаблона
 * @param scripts набор Groovy-скриптов
 */
public record CanonicalDeviceTypeTemplate(
        String formatVersion,
        CanonicalDeviceTypeMetadata metadata,
        CanonicalParameterSchema deviceTypeParameterSchema,
        CanonicalParameterSchema deviceParameterSchema,
        CanonicalTemplateValues bindingHints,
        CanonicalTemplateValues defaultValues,
        CanonicalTemplateValues exampleValues,
        CanonicalTemplateOrigin templateOrigin,
        CanonicalScriptSet scripts
) {
}
