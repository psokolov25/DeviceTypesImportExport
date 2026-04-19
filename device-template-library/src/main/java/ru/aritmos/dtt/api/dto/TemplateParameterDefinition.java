package ru.aritmos.dtt.api.dto;

import java.util.Map;

/**
 * Определение одного параметра шаблона типа устройства.
 *
 * <p>DTO используется как универсальное описание параметра в публичном API библиотеки,
 * не привязанное к конкретному формату хранения (`.dtt`, profile JSON или branch JSON).</p>
 *
 * @param name системное имя параметра
 * @param displayName человекочитаемое имя параметра
 * @param type тип параметра (например: String, Number, Boolean, Object, Array)
 * @param description описание назначения параметра
 * @param exampleValue пример значения параметра
 * @param parametersMap дополнительные метаданные параметра
 * @param nullable допускается ли {@code null} как валидное значение
 * @param nestedSchema вложенная схема (для Object/Array-параметров)
 */
public record TemplateParameterDefinition(
        String name,
        String displayName,
        String type,
        String description,
        Object exampleValue,
        Map<String, Object> parametersMap,
        boolean nullable,
        TemplateParameterSchema nestedSchema
) {
}
