package ru.aritmos.dtt.archive;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import ru.aritmos.dtt.archive.model.DttArchiveTemplate;
import ru.aritmos.dtt.exception.DttFormatException;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.TreeMap;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * Базовая реализация writer-а DTT-архива с детерминированным порядком и фиксированными timestamp.
 */
public class DefaultDttArchiveWriter implements DttArchiveWriter {

    private static final ObjectMapper YAML_MAPPER = new ObjectMapper(new YAMLFactory());
    private static final String EVENT_HANDLER_GROUP = "event handler";
    private static final String COMMAND_GROUP = "command";

    @Override
    public void write(DttArchiveTemplate template, OutputStream outputStream) {
        try (ZipOutputStream zipOutputStream = new ZipOutputStream(outputStream, StandardCharsets.UTF_8)) {
            final Map<String, Object> manifest = new LinkedHashMap<>();
            manifest.put("formatName", template.descriptor().formatName());
            manifest.put("formatVersion", template.descriptor().formatVersion());
            manifest.put("createdAt", "1970-01-01T00:00:00Z");
            manifest.put("createdBy", "device-template-library");
            manifest.put("libraryVersion", libraryVersion());
            manifest.put("deviceTypeId", template.descriptor().deviceTypeId());
            manifest.put("deviceTypeName", template.metadata().name());
            manifest.put("deviceTypeDisplayName", template.metadata().displayName());
            manifest.put("deviceTypeDescription", template.metadata().description());
            manifest.put("deviceTypeKind", resolveDeviceTypeKind(template));
            manifest.put("supportsChildDevices", template.deviceParametersSchema() != null && !template.deviceParametersSchema().isEmpty());
            manifest.put("containsLifecycleScripts", hasLifecycleScripts(template));
            manifest.put("containsEventHandlers", hasAnyScript(template.eventHandlers()));
            manifest.put("containsCommands", hasAnyScript(template.commands()));
            manifest.put("containsDeviceTypeFunctions", template.deviceTypeFunctions() != null && !template.deviceTypeFunctions().isBlank());
            manifest.put("parameterSchemaVersion", "1.0");
            manifest.put("defaultValuesIncluded", template.defaultValues() != null && !template.defaultValues().isEmpty());
            manifest.put("exampleValuesIncluded", template.exampleValues() != null && !template.exampleValues().isEmpty());
            manifest.put("supportsProfileImport", true);
            manifest.put("supportsBranchImport", true);
            manifest.put("sourceKind", resolveSourceKind(template));
            manifest.put("sourceSummary", resolveSourceSummary(template));
            if (template.descriptor().deviceTypeVersion() != null && !template.descriptor().deviceTypeVersion().isBlank()) {
                manifest.put("deviceTypeVersion", template.descriptor().deviceTypeVersion());
            }
            writeYaml(zipOutputStream, "manifest.yml", manifest);
            writeYaml(zipOutputStream, "template/device-type.yml", Map.of(
                    "id", template.metadata().id(),
                    "name", template.metadata().name(),
                    "displayName", template.metadata().displayName(),
                    "description", template.metadata().description()
            ));
            writeBytes(zipOutputStream, DttIconSupport.ICON_FILE_NAME, DttIconSupport.decode(template.metadata().iconBase64()));
            writeYaml(zipOutputStream, "template/device-type-parameters.yml", template.deviceTypeParametersSchema());
            writeYaml(zipOutputStream, "template/device-parameters-schema.yml", template.deviceParametersSchema());
            writeYaml(zipOutputStream, "template/template-origin.yml", defaultTemplateOrigin(template.templateOrigin()));
            writeYaml(zipOutputStream, "template/binding-hints.yml", template.bindingHints());
            writeYaml(zipOutputStream, "template/default-values.yml", template.defaultValues());
            writeYaml(zipOutputStream, "template/example-values.yml", template.exampleValues());
            writeYaml(zipOutputStream, "examples/profile-values-example.yml", profileValuesExample(template));
            writeYaml(zipOutputStream, "examples/branch-values-example.yml", branchValuesExample(template));
            writeText(zipOutputStream, "README-IN-ARCHIVE.md", archiveReadme(template));

            writeText(zipOutputStream, "scripts/onStartEvent.groovy", template.onStartEvent());
            writeText(zipOutputStream, "scripts/onStopEvent.groovy", template.onStopEvent());
            writeText(zipOutputStream, "scripts/onPublicStartEvent.groovy", template.onPublicStartEvent());
            writeText(zipOutputStream, "scripts/onPublicFinishEvent.groovy", template.onPublicFinishEvent());
            writeText(zipOutputStream, "scripts/deviceTypeFunctions.groovy", template.deviceTypeFunctions());

            for (Map.Entry<String, String> entry : sorted(template.eventHandlers()).entrySet()) {
                validateScriptEntryName(EVENT_HANDLER_GROUP, entry.getKey());
                writeText(zipOutputStream, "scripts/event-handlers/" + entry.getKey() + ".groovy", entry.getValue());
            }
            for (Map.Entry<String, String> entry : sorted(template.commands()).entrySet()) {
                validateScriptEntryName(COMMAND_GROUP, entry.getKey());
                writeText(zipOutputStream, "scripts/commands/" + entry.getKey() + ".groovy", entry.getValue());
            }
        } catch (IOException exception) {
            throw new DttFormatException("Ошибка записи DTT-архива", exception);
        }
    }

    private void writeYaml(ZipOutputStream stream, String entryName, Map<String, Object> payload) throws IOException {
        writeBytes(stream, entryName, yaml(payload));
    }

    private byte[] yaml(Map<String, Object> payload) {
        try {
            return YAML_MAPPER.writeValueAsBytes(payload == null ? Map.of() : payload);
        } catch (JsonProcessingException exception) {
            throw new DttFormatException("Ошибка сериализации YAML для DTT", exception);
        }
    }

    private void writeText(ZipOutputStream stream, String entryName, String content) throws IOException {
        writeBytes(stream, entryName, content == null ? new byte[0] : content.getBytes(StandardCharsets.UTF_8));
    }

    private void writeBytes(ZipOutputStream stream, String entryName, byte[] payload) throws IOException {
        final ZipEntry entry = new ZipEntry(entryName);
        entry.setTime(0L);
        stream.putNextEntry(entry);
        stream.write(payload);
        stream.closeEntry();
    }

    private Map<String, String> sorted(Map<String, String> source) {
        return source == null ? Map.of() : new TreeMap<>(source);
    }

    private Map<String, Object> profileValuesExample(DttArchiveTemplate template) {
        if (template.exampleValues() != null && !template.exampleValues().isEmpty()) {
            return template.exampleValues();
        }
        if (template.defaultValues() != null && !template.defaultValues().isEmpty()) {
            return template.defaultValues();
        }
        return Map.of();
    }

    private Map<String, Object> branchValuesExample(DttArchiveTemplate template) {
        return Map.of(
                "branches", Map.of(
                        "branch-1", Map.of(
                                "deviceTypes", Map.of(
                                        template.descriptor().deviceTypeId(),
                                        profileValuesExample(template)
                                )
                        )
                )
        );
    }

    private String archiveReadme(DttArchiveTemplate template) {
        final String nl = System.lineSeparator();
        return "# DTT Archive" + nl
                + nl
                + "Device type id: " + template.descriptor().deviceTypeId() + nl
                + "Format: " + template.descriptor().formatName() + "/" + template.descriptor().formatVersion() + nl
                + nl
                + "Files:" + nl
                + "- template/*.yml: schema/default/example/origin/binding hints" + nl
                + "- examples/*.yml: sample profile/branch values" + nl
                + "- scripts/*.groovy: lifecycle handlers, functions, events, commands" + nl;
    }

    private Map<String, Object> defaultTemplateOrigin(Map<String, Object> templateOrigin) {
        if (templateOrigin == null || templateOrigin.isEmpty()) {
            return Map.of("sourceKind", "UNSPECIFIED");
        }
        return templateOrigin;
    }

    private String resolveDeviceTypeKind(DttArchiveTemplate template) {
        if (template.bindingHints() != null && template.bindingHints().get("deviceTypeKind") != null) {
            return String.valueOf(template.bindingHints().get("deviceTypeKind"));
        }
        return template.metadata().name();
    }

    private boolean hasLifecycleScripts(DttArchiveTemplate template) {
        return hasScript(template.onStartEvent())
                || hasScript(template.onStopEvent())
                || hasScript(template.onPublicStartEvent())
                || hasScript(template.onPublicFinishEvent());
    }

    private boolean hasScript(String script) {
        return script != null && !script.isBlank();
    }

    private boolean hasAnyScript(Map<String, String> scripts) {
        if (scripts == null || scripts.isEmpty()) {
            return false;
        }
        for (String script : scripts.values()) {
            if (hasScript(script)) {
                return true;
            }
        }
        return false;
    }

    private String resolveSourceKind(DttArchiveTemplate template) {
        final Map<String, Object> origin = defaultTemplateOrigin(template.templateOrigin());
        return String.valueOf(origin.getOrDefault("sourceKind", "UNSPECIFIED"));
    }

    private String resolveSourceSummary(DttArchiveTemplate template) {
        final Map<String, Object> origin = defaultTemplateOrigin(template.templateOrigin());
        final Object value = origin.get("sourceSummary");
        return value == null ? "" : String.valueOf(value);
    }

    private String libraryVersion() {
        final Package packageInfo = DefaultDttArchiveWriter.class.getPackage();
        if (packageInfo == null || packageInfo.getImplementationVersion() == null) {
            return "unknown";
        }
        return packageInfo.getImplementationVersion();
    }

    private void validateScriptEntryName(String scriptGroup, String scriptName) {
        if (scriptName == null || scriptName.isBlank()) {
            throw new DttFormatException("Некорректное имя %s в DTT-архиве".formatted(scriptGroup));
        }
        if (scriptName.contains("/") || scriptName.contains("\\") || scriptName.contains("..")) {
            throw new DttFormatException("Недопустимое имя %s в DTT-архиве: %s".formatted(scriptGroup, scriptName));
        }
    }
}
