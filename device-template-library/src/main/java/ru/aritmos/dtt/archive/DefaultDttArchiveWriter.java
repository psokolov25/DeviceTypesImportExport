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

    @Override
    public void write(DttArchiveTemplate template, OutputStream outputStream) {
        try (ZipOutputStream zipOutputStream = new ZipOutputStream(outputStream, StandardCharsets.UTF_8)) {
            final Map<String, Object> manifest = new LinkedHashMap<>();
            manifest.put("formatName", template.descriptor().formatName());
            manifest.put("formatVersion", template.descriptor().formatVersion());
            manifest.put("deviceTypeId", template.descriptor().deviceTypeId());
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
            writeYaml(zipOutputStream, "template/device-type-parameters.yml", template.deviceTypeParametersSchema());
            writeYaml(zipOutputStream, "template/device-parameters-schema.yml", template.deviceParametersSchema());
            writeYaml(zipOutputStream, "template/template-origin.yml", Map.of("sourceKind", "UNSPECIFIED"));
            writeYaml(zipOutputStream, "template/binding-hints.yml", template.bindingHints());
            writeYaml(zipOutputStream, "template/default-values.yml", template.defaultValues());
            writeYaml(zipOutputStream, "template/example-values.yml", template.exampleValues());

            writeText(zipOutputStream, "scripts/onStartEvent.groovy", template.onStartEvent());
            writeText(zipOutputStream, "scripts/onStopEvent.groovy", template.onStopEvent());
            writeText(zipOutputStream, "scripts/onPublicStartEvent.groovy", template.onPublicStartEvent());
            writeText(zipOutputStream, "scripts/onPublicFinishEvent.groovy", template.onPublicFinishEvent());
            writeText(zipOutputStream, "scripts/deviceTypeFunctions.groovy", template.deviceTypeFunctions());

            for (Map.Entry<String, String> entry : sorted(template.eventHandlers()).entrySet()) {
                writeText(zipOutputStream, "scripts/event-handlers/" + entry.getKey() + ".groovy", entry.getValue());
            }
            for (Map.Entry<String, String> entry : sorted(template.commands()).entrySet()) {
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
}
