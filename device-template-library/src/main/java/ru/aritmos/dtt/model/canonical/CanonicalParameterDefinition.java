package ru.aritmos.dtt.model.canonical;

import java.util.Map;

/**
 * Каноническое определение одного параметра с поддержкой вложенной схемы и metadata.
 *
 * @param name имя параметра
 * @param type тип параметра (`String`, `Number`, `Boolean`, `Object`, `Array`, `nullable` и т.д.)
 * @param metadata дополнительные поля metadata (`displayName`, `description`, `exampleValue` и пр.)
 * @param parametersMap вложенная схема для `Object`
 * @param items схема элемента для `Array`
 */
public record CanonicalParameterDefinition(
        String name,
        String type,
        Map<String, Object> metadata,
        Map<String, CanonicalParameterDefinition> parametersMap,
        CanonicalParameterDefinition items
) {
}

