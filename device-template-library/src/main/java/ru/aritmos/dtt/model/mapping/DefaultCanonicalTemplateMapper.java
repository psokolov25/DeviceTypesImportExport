package ru.aritmos.dtt.model.mapping;

import ru.aritmos.dtt.api.dto.DeviceTypeMetadata;
import ru.aritmos.dtt.archive.model.DttArchiveDescriptor;
import ru.aritmos.dtt.archive.model.DttArchiveTemplate;
import ru.aritmos.dtt.model.canonical.CanonicalDeviceTypeMetadata;
import ru.aritmos.dtt.model.canonical.CanonicalParameterDefinition;
import ru.aritmos.dtt.model.canonical.CanonicalParameterSchema;
import ru.aritmos.dtt.model.canonical.CanonicalDeviceTypeTemplate;
import ru.aritmos.dtt.model.canonical.CanonicalScriptSet;
import ru.aritmos.dtt.model.canonical.CanonicalTemplateOrigin;
import ru.aritmos.dtt.model.canonical.CanonicalTemplateValues;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Базовый маппер archive DTO <-> canonical model без потери Groovy-кода и metadata полей.
 */
public class DefaultCanonicalTemplateMapper implements CanonicalTemplateMapper {

    @Override
    public CanonicalDeviceTypeTemplate toCanonical(DttArchiveTemplate archiveTemplate) {
        Objects.requireNonNull(archiveTemplate, "archiveTemplate is required");

        return new CanonicalDeviceTypeTemplate(
                archiveTemplate.descriptor().formatVersion(),
                new CanonicalDeviceTypeMetadata(
                        archiveTemplate.metadata().id(),
                        archiveTemplate.metadata().name(),
                        archiveTemplate.metadata().displayName(),
                        archiveTemplate.metadata().description()
                ),
                toParameterSchema(archiveTemplate.deviceTypeParametersSchema()),
                toParameterSchema(archiveTemplate.deviceParametersSchema()),
                new CanonicalTemplateValues(safeMap(archiveTemplate.bindingHints())),
                new CanonicalTemplateValues(safeMap(archiveTemplate.defaultValues())),
                new CanonicalTemplateValues(safeMap(archiveTemplate.exampleValues())),
                toTemplateOrigin(archiveTemplate.templateOrigin()),
                new CanonicalScriptSet(
                        archiveTemplate.onStartEvent(),
                        archiveTemplate.onStopEvent(),
                        archiveTemplate.onPublicStartEvent(),
                        archiveTemplate.onPublicFinishEvent(),
                        archiveTemplate.deviceTypeFunctions(),
                        archiveTemplate.eventHandlers() == null ? Map.of() : archiveTemplate.eventHandlers(),
                        archiveTemplate.commands() == null ? Map.of() : archiveTemplate.commands()
                )
        );
    }

    @Override
    public DttArchiveTemplate toArchive(CanonicalDeviceTypeTemplate canonicalTemplate) {
        Objects.requireNonNull(canonicalTemplate, "canonicalTemplate is required");

        final CanonicalDeviceTypeMetadata metadata = canonicalTemplate.metadata();
        final CanonicalScriptSet scripts = canonicalTemplate.scripts();

        return new DttArchiveTemplate(
                new DttArchiveDescriptor("DTT", canonicalTemplate.formatVersion(), metadata.id(), null),
                new DeviceTypeMetadata(metadata.id(), metadata.name(), metadata.displayName(), metadata.description(), null, null),
                toSchemaMap(canonicalTemplate.deviceTypeParameterSchema()),
                toSchemaMap(canonicalTemplate.deviceParameterSchema()),
                valueMap(canonicalTemplate.bindingHints()),
                valueMap(canonicalTemplate.defaultValues()),
                valueMap(canonicalTemplate.exampleValues()),
                toOriginMap(canonicalTemplate.templateOrigin()),
                scripts.onStartEvent(),
                scripts.onStopEvent(),
                scripts.onPublicStartEvent(),
                scripts.onPublicFinishEvent(),
                scripts.deviceTypeFunctions(),
                scripts.eventHandlers() == null ? Map.of() : scripts.eventHandlers(),
                scripts.commands() == null ? Map.of() : scripts.commands()
        );
    }

    private Map<String, Object> safeMap(Map<String, Object> source) {
        return source == null ? Map.of() : source;
    }

    private Map<String, Object> valueMap(CanonicalTemplateValues values) {
        return values == null ? Map.of() : safeMap(values.values());
    }

    private CanonicalTemplateOrigin toTemplateOrigin(Map<String, Object> origin) {
        final Map<String, Object> safeOrigin = safeMap(origin);
        final String sourceKind = safeOrigin.get("sourceKind") == null ? "UNSPECIFIED" : String.valueOf(safeOrigin.get("sourceKind"));
        final String sourceSummary = safeOrigin.get("sourceSummary") == null ? "" : String.valueOf(safeOrigin.get("sourceSummary"));
        final Map<String, Object> metadata = new LinkedHashMap<>(safeOrigin);
        metadata.remove("sourceKind");
        metadata.remove("sourceSummary");
        return new CanonicalTemplateOrigin(sourceKind, sourceSummary, metadata);
    }

    private Map<String, Object> toOriginMap(CanonicalTemplateOrigin origin) {
        if (origin == null) {
            return Map.of();
        }
        final Map<String, Object> result = new LinkedHashMap<>();
        result.put("sourceKind", origin.sourceKind());
        if (origin.sourceSummary() != null && !origin.sourceSummary().isBlank()) {
            result.put("sourceSummary", origin.sourceSummary());
        }
        if (origin.metadata() != null && !origin.metadata().isEmpty()) {
            result.putAll(origin.metadata());
        }
        return result;
    }

    private CanonicalParameterSchema toParameterSchema(Map<String, Object> schema) {
        final Map<String, CanonicalParameterDefinition> parameters = new LinkedHashMap<>();
        safeMap(schema).forEach((name, rawDefinition) -> parameters.put(name, toParameterDefinition(name, rawDefinition)));
        return new CanonicalParameterSchema(parameters);
    }

    private CanonicalParameterDefinition toParameterDefinition(String name, Object rawDefinition) {
        if (!(rawDefinition instanceof Map<?, ?> rawMap)) {
            return new CanonicalParameterDefinition(name, inferType(rawDefinition), Map.of(), Map.of(), null);
        }
        final Map<String, Object> map = castToStringObjectMap(rawMap);
        final Map<String, Object> metadata = new LinkedHashMap<>(map);
        metadata.remove("name");
        metadata.remove("type");
        metadata.remove("parametersMap");
        metadata.remove("items");

        final Map<String, CanonicalParameterDefinition> nested = new LinkedHashMap<>();
        final Object parametersMap = map.get("parametersMap");
        if (parametersMap instanceof Map<?, ?> nestedMap) {
            castToStringObjectMap(nestedMap).forEach((nestedName, nestedValue) ->
                    nested.put(nestedName, toParameterDefinition(nestedName, nestedValue)));
        }
        final CanonicalParameterDefinition items = map.get("items") == null
                ? null
                : toParameterDefinition(name + "_item", map.get("items"));

        return new CanonicalParameterDefinition(
                map.get("name") == null ? name : String.valueOf(map.get("name")),
                map.get("type") == null ? inferType(map.get("value")) : String.valueOf(map.get("type")),
                metadata,
                nested,
                items
        );
    }

    private Map<String, Object> toSchemaMap(CanonicalParameterSchema schema) {
        if (schema == null || schema.parameters() == null || schema.parameters().isEmpty()) {
            return Map.of();
        }
        final Map<String, Object> result = new LinkedHashMap<>();
        schema.parameters().forEach((name, definition) -> result.put(name, toDefinitionMap(definition)));
        return result;
    }

    private Map<String, Object> toDefinitionMap(CanonicalParameterDefinition definition) {
        final Map<String, Object> result = new LinkedHashMap<>();
        result.put("name", definition.name());
        result.put("type", definition.type());
        if (definition.metadata() != null && !definition.metadata().isEmpty()) {
            result.putAll(definition.metadata());
        }
        if (definition.parametersMap() != null && !definition.parametersMap().isEmpty()) {
            final Map<String, Object> nested = new LinkedHashMap<>();
            definition.parametersMap().forEach((nestedName, nestedDefinition) ->
                    nested.put(nestedName, toDefinitionMap(nestedDefinition)));
            result.put("parametersMap", nested);
        }
        if (definition.items() != null) {
            result.put("items", toDefinitionMap(definition.items()));
        }
        return result;
    }

    private String inferType(Object value) {
        if (value == null) {
            return "nullable";
        }
        if (value instanceof Boolean) {
            return "Boolean";
        }
        if (value instanceof Number) {
            return "Number";
        }
        if (value instanceof Map<?, ?>) {
            return "Object";
        }
        if (value instanceof Iterable<?>) {
            return "Array";
        }
        return "String";
    }

    private Map<String, Object> castToStringObjectMap(Map<?, ?> source) {
        final Map<String, Object> casted = new LinkedHashMap<>();
        source.forEach((key, value) -> casted.put(String.valueOf(key), value));
        return casted;
    }
}
