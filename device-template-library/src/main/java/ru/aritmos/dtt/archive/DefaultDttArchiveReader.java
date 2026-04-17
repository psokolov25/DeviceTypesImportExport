package ru.aritmos.dtt.archive;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import ru.aritmos.dtt.api.dto.DeviceTypeMetadata;
import ru.aritmos.dtt.archive.model.DttArchiveDescriptor;
import ru.aritmos.dtt.archive.model.DttArchiveTemplate;
import ru.aritmos.dtt.exception.DttFormatException;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * Базовая реализация reader-а DTT, читающая YAML/скрипты из ZIP-архива.
 */
public class DefaultDttArchiveReader implements DttArchiveReader {

    private static final ObjectMapper YAML_MAPPER = new ObjectMapper(new YAMLFactory());
    private static final TypeReference<Map<String, Object>> MAP_TYPE = new TypeReference<>() { };

    @Override
    public DttArchiveTemplate read(InputStream inputStream) {
        final Map<String, byte[]> entries = readEntries(inputStream);
        require(entries, "manifest.yml");
        require(entries, "template/device-type.yml");

        final Map<String, Object> manifest = readYaml(entries.get("manifest.yml"), "manifest.yml");
        final Map<String, Object> deviceType = readYaml(entries.get("template/device-type.yml"), "template/device-type.yml");

        final DttArchiveDescriptor descriptor = new DttArchiveDescriptor(
                Objects.toString(manifest.get("formatName"), "DTT"),
                Objects.toString(manifest.get("formatVersion"), "1.0"),
                Objects.toString(manifest.get("deviceTypeId"), Objects.toString(deviceType.get("id"), "unknown")),
                manifest.get("deviceTypeVersion") == null ? null : Objects.toString(manifest.get("deviceTypeVersion"))
        );

        final DeviceTypeMetadata metadata = new DeviceTypeMetadata(
                Objects.toString(deviceType.get("id"), descriptor.deviceTypeId()),
                Objects.toString(deviceType.get("name"), "unknown"),
                Objects.toString(deviceType.get("displayName"), Objects.toString(deviceType.get("name"), "unknown")),
                Objects.toString(deviceType.get("description"), "")
        );

        return new DttArchiveTemplate(
                descriptor,
                metadata,
                readOptionalYaml(entries, "template/device-type-parameters.yml"),
                readOptionalYaml(entries, "template/device-parameters-schema.yml"),
                readOptionalYaml(entries, "template/binding-hints.yml"),
                readOptionalYaml(entries, "template/default-values.yml"),
                readOptionalYaml(entries, "template/example-values.yml"),
                readText(entries, "scripts/onStartEvent.groovy"),
                readText(entries, "scripts/onStopEvent.groovy"),
                readText(entries, "scripts/onPublicStartEvent.groovy"),
                readText(entries, "scripts/onPublicFinishEvent.groovy"),
                readText(entries, "scripts/deviceTypeFunctions.groovy"),
                readScriptMap(entries, "scripts/event-handlers/"),
                readScriptMap(entries, "scripts/commands/")
        );
    }

    private Map<String, byte[]> readEntries(InputStream inputStream) {
        final Map<String, byte[]> entries = new LinkedHashMap<>();
        try (ZipInputStream zipInputStream = new ZipInputStream(inputStream, StandardCharsets.UTF_8)) {
            ZipEntry zipEntry = zipInputStream.getNextEntry();
            while (zipEntry != null) {
                if (!zipEntry.isDirectory()) {
                    entries.put(zipEntry.getName(), zipInputStream.readAllBytes());
                }
                zipInputStream.closeEntry();
                zipEntry = zipInputStream.getNextEntry();
            }
            return entries;
        } catch (IOException exception) {
            throw new DttFormatException("Ошибка чтения ZIP-архива DTT", exception);
        }
    }

    private void require(Map<String, byte[]> entries, String name) {
        if (!entries.containsKey(name)) {
            throw new DttFormatException("В DTT отсутствует обязательный файл: " + name);
        }
    }

    private Map<String, Object> readOptionalYaml(Map<String, byte[]> entries, String name) {
        if (!entries.containsKey(name)) {
            return Map.of();
        }
        return readYaml(entries.get(name), name);
    }

    private Map<String, Object> readYaml(byte[] bytes, String entryName) {
        try {
            final Map<String, Object> value = YAML_MAPPER.readValue(bytes, MAP_TYPE);
            return value == null ? Map.of() : value;
        } catch (IOException exception) {
            throw new DttFormatException("Некорректный YAML в файле " + entryName, exception);
        }
    }

    private String readText(Map<String, byte[]> entries, String name) {
        final byte[] bytes = entries.get(name);
        return bytes == null ? null : new String(bytes, StandardCharsets.UTF_8);
    }

    private Map<String, String> readScriptMap(Map<String, byte[]> entries, String prefix) {
        final Map<String, String> scripts = new HashMap<>();
        entries.forEach((key, value) -> {
            if (key.startsWith(prefix) && key.endsWith(".groovy")) {
                final String scriptName = key.substring(prefix.length(), key.length() - ".groovy".length());
                scripts.put(scriptName, new String(value, StandardCharsets.UTF_8));
            }
        });
        return scripts;
    }
}
