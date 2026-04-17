package ru.aritmos.dtt.model.mapping;

import ru.aritmos.dtt.api.dto.DeviceTypeMetadata;
import ru.aritmos.dtt.archive.model.DttArchiveDescriptor;
import ru.aritmos.dtt.archive.model.DttArchiveTemplate;
import ru.aritmos.dtt.model.canonical.CanonicalDeviceTypeMetadata;
import ru.aritmos.dtt.model.canonical.CanonicalDeviceTypeTemplate;
import ru.aritmos.dtt.model.canonical.CanonicalScriptSet;

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
                safeMap(archiveTemplate.deviceTypeParametersSchema()),
                safeMap(archiveTemplate.deviceParametersSchema()),
                safeMap(archiveTemplate.bindingHints()),
                safeMap(archiveTemplate.defaultValues()),
                safeMap(archiveTemplate.exampleValues()),
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
                new DttArchiveDescriptor("DTT", canonicalTemplate.formatVersion(), metadata.id()),
                new DeviceTypeMetadata(metadata.id(), metadata.name(), metadata.displayName(), metadata.description()),
                safeMap(canonicalTemplate.deviceTypeParameterSchema()),
                safeMap(canonicalTemplate.deviceParameterSchema()),
                safeMap(canonicalTemplate.bindingHints()),
                safeMap(canonicalTemplate.defaultValues()),
                safeMap(canonicalTemplate.exampleValues()),
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
}
