package ru.aritmos.dtt.model.mapping;

import ru.aritmos.dtt.model.canonical.CanonicalBranchProjection;
import ru.aritmos.dtt.model.canonical.CanonicalDeviceTypeTemplate;
import ru.aritmos.dtt.model.canonical.CanonicalParameterDefinition;
import ru.aritmos.dtt.model.canonical.CanonicalProfileProjection;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Базовый маппер канонической модели в profile/branch проекции.
 */
public class DefaultCanonicalProjectionMapper implements CanonicalProjectionMapper {

    @Override
    public CanonicalProfileProjection toProfileProjection(CanonicalDeviceTypeTemplate template) {
        Objects.requireNonNull(template, "template is required");
        final Map<String, Object> defaults = template.defaultValues() == null ? Map.of() : template.defaultValues().values();
        final Map<String, Object> examples = template.exampleValues() == null ? Map.of() : template.exampleValues().values();
        final Map<String, Object> values = new LinkedHashMap<>();
        if (template.deviceTypeParameterSchema() == null || template.deviceTypeParameterSchema().parameters() == null) {
            return new CanonicalProfileProjection(defaults, template.deviceTypeParameterSchema());
        }
        template.deviceTypeParameterSchema().parameters().forEach((name, definition) -> {
            final Object value = defaults.containsKey(name) ? defaults.get(name) : null;
            final Object example = examples.containsKey(name) ? examples.get(name) : null;
            values.put(name, mergeDefinitionWithValues(name, definition, value, example));
        });
        defaults.forEach(values::putIfAbsent);
        return new CanonicalProfileProjection(values, template.deviceTypeParameterSchema());
    }

    @Override
    public CanonicalBranchProjection toBranchProjection(CanonicalDeviceTypeTemplate template, String kind) {
        final CanonicalProfileProjection profileProjection = toProfileProjection(template);
        return new CanonicalBranchProjection(
                kind,
                profileProjection.deviceTypeParamValues(),
                profileProjection.deviceTypeParameterSchema(),
                template.deviceParameterSchema()
        );
    }

    private Object mergeDefinitionWithValues(String name,
                                             CanonicalParameterDefinition definition,
                                             Object value,
                                             Object exampleValue) {
        if (definition == null) {
            return value;
        }
        final Map<String, Object> merged = new LinkedHashMap<>();
        merged.put("name", definition.name() == null ? name : definition.name());
        merged.put("type", definition.type());
        if (definition.metadata() != null && !definition.metadata().isEmpty()) {
            merged.putAll(definition.metadata());
        }
        if (exampleValue != null && !merged.containsKey("exampleValue")) {
            merged.put("exampleValue", exampleValue);
        }

        if (definition.parametersMap() != null && !definition.parametersMap().isEmpty()) {
            final Map<String, Object> nestedValueMap = value instanceof Map<?, ?> map ? castToStringObjectMap(map) : Map.of();
            final Map<String, Object> nestedExampleMap = exampleValue instanceof Map<?, ?> map ? castToStringObjectMap(map) : Map.of();
            final Map<String, Object> nestedValues = new LinkedHashMap<>();
            definition.parametersMap().forEach((nestedName, nestedDefinition) -> {
                final Object nestedValue = nestedValueMap.containsKey(nestedName) ? nestedValueMap.get(nestedName) : null;
                final Object nestedExample = nestedExampleMap.containsKey(nestedName) ? nestedExampleMap.get(nestedName) : null;
                nestedValues.put(nestedName, mergeDefinitionWithValues(nestedName, nestedDefinition, nestedValue, nestedExample));
            });
            merged.put("value", nestedValues);
            return merged;
        }

        if (definition.items() != null && value instanceof List<?> valueList) {
            merged.put("items", toProjectionSchema(definition.items(), name + "_item"));
            final List<?> exampleList = exampleValue instanceof List<?> list ? list : List.of();
            final List<Object> mergedValues = new java.util.ArrayList<>();
            for (int i = 0; i < valueList.size(); i++) {
                final Object itemValue = valueList.get(i);
                final Object itemExample = i < exampleList.size() ? exampleList.get(i) : null;
                if (itemValue instanceof Map<?, ?>) {
                    mergedValues.add(mergeDefinitionWithValues(name + "_item", definition.items(), itemValue, itemExample));
                } else {
                    mergedValues.add(itemValue);
                }
            }
            merged.put("value", mergedValues);
            return merged;
        }
        if (definition.items() != null) {
            merged.put("items", toProjectionSchema(definition.items(), name + "_item"));
            merged.put("value", value);
            return merged;
        }
        merged.put("value", value);
        return merged;
    }

    private Map<String, Object> toProjectionSchema(CanonicalParameterDefinition definition, String fallbackName) {
        final Map<String, Object> schema = new LinkedHashMap<>();
        final String name = definition.name() == null ? fallbackName : definition.name();
        schema.put("name", name);
        schema.put("type", definition.type());
        if (definition.metadata() != null && !definition.metadata().isEmpty()) {
            schema.putAll(definition.metadata());
        }
        if (definition.parametersMap() != null && !definition.parametersMap().isEmpty()) {
            final Map<String, Object> nested = new LinkedHashMap<>();
            definition.parametersMap().forEach((nestedName, nestedDefinition) ->
                    nested.put(nestedName, toProjectionSchema(nestedDefinition, nestedName)));
            schema.put("parametersMap", nested);
        }
        if (definition.items() != null) {
            schema.put("items", toProjectionSchema(definition.items(), name + "_item"));
        }
        return schema;
    }

    private Map<String, Object> castToStringObjectMap(Map<?, ?> source) {
        final Map<String, Object> casted = new LinkedHashMap<>();
        source.forEach((key, value) -> casted.put(String.valueOf(key), value));
        return casted;
    }
}
