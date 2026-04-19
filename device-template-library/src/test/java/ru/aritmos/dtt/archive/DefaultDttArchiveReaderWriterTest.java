package ru.aritmos.dtt.archive;

import org.junit.jupiter.api.Test;
import ru.aritmos.dtt.api.dto.DeviceTypeMetadata;
import ru.aritmos.dtt.archive.model.DttArchiveDescriptor;
import ru.aritmos.dtt.archive.model.DttArchiveTemplate;
import ru.aritmos.dtt.exception.DttFormatException;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DefaultDttArchiveReaderWriterTest {

    private final DttArchiveWriter writer = new DefaultDttArchiveWriter();
    private final DttArchiveReader reader = new DefaultDttArchiveReader();

    @Test
    void shouldPreserveScriptsAndYamlOnRoundTrip() {
        final DttArchiveTemplate template = sampleTemplate();

        final ByteArrayOutputStream output = new ByteArrayOutputStream();
        writer.write(template, output);

        final DttArchiveTemplate restored = reader.read(new ByteArrayInputStream(output.toByteArray()));

        assertThat(restored.descriptor().formatName()).isEqualTo("DTT");
        assertThat(restored.metadata().id()).isEqualTo("display-type");
        assertThat(restored.eventHandlers()).containsEntry("VISIT_CALLED", "println 'called'");
        assertThat(restored.commands()).containsEntry("RESET", "println 'reset'");
        assertThat(restored.onStartEvent()).isEqualTo("println 'start'");
        assertThat(restored.defaultValues()).containsEntry("ip", "127.0.0.1");
        assertThat(restored.templateOrigin()).containsEntry("sourceKind", "PROFILE_JSON");
    }

    @Test
    void shouldWriteArchiveExamplesAndReadmeFiles() throws Exception {
        final ByteArrayOutputStream output = new ByteArrayOutputStream();
        writer.write(sampleTemplate(), output);

        final Map<String, String> zipEntries = readZipEntries(output.toByteArray());

        assertThat(zipEntries).containsKeys(
                "examples/profile-values-example.yml",
                "examples/branch-values-example.yml",
                "README-IN-ARCHIVE.md"
        );
        assertThat(zipEntries.get("examples/profile-values-example.yml")).contains("ip: \"192.168.0.10\"");
        assertThat(zipEntries.get("examples/branch-values-example.yml")).contains("display-type");
        assertThat(zipEntries.get("README-IN-ARCHIVE.md")).contains("Device type id: display-type");
        assertThat(zipEntries.get("manifest.yml"))
                .contains("createdAt: \"1970-01-01T00:00:00Z\"")
                .contains("createdBy: \"device-template-library\"")
                .contains("sourceKind: \"PROFILE_JSON\"")
                .contains("supportsProfileImport: true")
                .contains("supportsBranchImport: true")
                .contains("containsEventHandlers: true")
                .contains("containsCommands: true");
    }

    @Test
    void shouldMarkHandlersAndCommandsAsAbsentWhenScriptsBlank() throws Exception {
        final DttArchiveTemplate template = new DttArchiveTemplate(
                new DttArchiveDescriptor("DTT", "1.0", "display-type", null),
                new DeviceTypeMetadata("display-type", "Display", "Дисплей", "Табло"),
                Map.of(),
                Map.of(),
                Map.of(),
                Map.of(),
                Map.of(),
                Map.of("sourceKind", "PROFILE_JSON"),
                "",
                "",
                "",
                "",
                "",
                Map.of("VISIT_CALLED", " "),
                Map.of("RESET", "\n")
        );

        final ByteArrayOutputStream output = new ByteArrayOutputStream();
        writer.write(template, output);

        final Map<String, String> zipEntries = readZipEntries(output.toByteArray());
        final String manifest = zipEntries.get("manifest.yml");
        assertThat(manifest)
                .contains("containsEventHandlers: false")
                .contains("containsCommands: false");
        assertThat(zipEntries)
                .containsKeys(
                        "scripts/event-handlers/VISIT_CALLED.groovy",
                        "scripts/commands/RESET.groovy"
                );
        assertThat(zipEntries.get("scripts/event-handlers/VISIT_CALLED.groovy")).isEqualTo(" ");
        assertThat(zipEntries.get("scripts/commands/RESET.groovy")).isEqualTo("\n");
    }

    @Test
    void shouldPreserveBlankAndNonBlankHandlerAndCommandScripts() throws Exception {
        final DttArchiveTemplate template = new DttArchiveTemplate(
                new DttArchiveDescriptor("DTT", "1.0", "display-type", null),
                new DeviceTypeMetadata("display-type", "Display", "Дисплей", "Табло"),
                Map.of(),
                Map.of(),
                Map.of(),
                Map.of(),
                Map.of(),
                Map.of("sourceKind", "PROFILE_JSON"),
                "",
                "",
                "",
                "",
                "",
                Map.of("VISIT_CALLED", " ", "VISIT_FINISHED", "println 'done'"),
                Map.of("RESET", "\n", "PING", "println 'pong'")
        );

        final ByteArrayOutputStream output = new ByteArrayOutputStream();
        writer.write(template, output);
        final Map<String, String> zipEntries = readZipEntries(output.toByteArray());

        assertThat(zipEntries)
                .containsKey("scripts/event-handlers/VISIT_FINISHED.groovy")
                .containsKey("scripts/commands/PING.groovy")
                .containsKeys(
                        "scripts/event-handlers/VISIT_CALLED.groovy",
                        "scripts/commands/RESET.groovy"
                );
        assertThat(zipEntries.get("scripts/event-handlers/VISIT_CALLED.groovy")).isEqualTo(" ");
        assertThat(zipEntries.get("scripts/commands/RESET.groovy")).isEqualTo("\n");
        assertThat(zipEntries.get("manifest.yml"))
                .contains("containsEventHandlers: true")
                .contains("containsCommands: true");
    }

    @Test
    void shouldProduceDeterministicArchiveBytes() {
        final DttArchiveTemplate template = sampleTemplate();

        final ByteArrayOutputStream first = new ByteArrayOutputStream();
        final ByteArrayOutputStream second = new ByteArrayOutputStream();

        writer.write(template, first);
        writer.write(template, second);

        assertThat(first.toByteArray()).isEqualTo(second.toByteArray());
    }

    @Test
    void shouldFailWhenManifestMissing() throws Exception {
        final ByteArrayOutputStream output = new ByteArrayOutputStream();
        try (ZipOutputStream zip = new ZipOutputStream(output, StandardCharsets.UTF_8)) {
            zip.putNextEntry(new ZipEntry("template/device-type.yml"));
            zip.write("id: no-manifest\nname: test".getBytes(StandardCharsets.UTF_8));
            zip.closeEntry();
        }

        assertThatThrownBy(() -> reader.read(new ByteArrayInputStream(output.toByteArray())))
                .isInstanceOf(DttFormatException.class)
                .hasMessageContaining("manifest.yml");
    }

    @Test
    void shouldFailWhenYamlBroken() throws Exception {
        final ByteArrayOutputStream output = new ByteArrayOutputStream();
        try (ZipOutputStream zip = new ZipOutputStream(output, StandardCharsets.UTF_8)) {
            zip.putNextEntry(new ZipEntry("manifest.yml"));
            zip.write("formatName: DTT".getBytes(StandardCharsets.UTF_8));
            zip.closeEntry();
            zip.putNextEntry(new ZipEntry("template/device-type.yml"));
            zip.write("id: [broken".getBytes(StandardCharsets.UTF_8));
            zip.closeEntry();
        }

        assertThatThrownBy(() -> reader.read(new ByteArrayInputStream(output.toByteArray())))
                .isInstanceOf(DttFormatException.class)
                .hasMessageContaining("Некорректный YAML");
    }

    @Test
    void shouldFailWhenEventHandlerNameBlank() {
        final DttArchiveTemplate template = templateWithScriptNames(
                Map.of(" ", "println 'invalid'"),
                Map.of()
        );

        assertThatThrownBy(() -> writer.write(template, new ByteArrayOutputStream()))
                .isInstanceOf(DttFormatException.class)
                .hasMessageContaining("event handler");
    }

    @Test
    void shouldFailWhenCommandNameBlank() {
        final DttArchiveTemplate template = templateWithScriptNames(
                Map.of(),
                Map.of("", "println 'invalid'")
        );

        assertThatThrownBy(() -> writer.write(template, new ByteArrayOutputStream()))
                .isInstanceOf(DttFormatException.class)
                .hasMessageContaining("command");
    }

    @Test
    void shouldFailWhenEventHandlerNameContainsPathSeparator() {
        final DttArchiveTemplate template = templateWithScriptNames(
                Map.of("folder/HANDLER", "println 'invalid'"),
                Map.of()
        );

        assertThatThrownBy(() -> writer.write(template, new ByteArrayOutputStream()))
                .isInstanceOf(DttFormatException.class)
                .hasMessageContaining("event handler");
    }

    @Test
    void shouldFailWhenCommandNameContainsPathTraversal() {
        final DttArchiveTemplate template = templateWithScriptNames(
                Map.of(),
                Map.of("../RESET", "println 'invalid'")
        );

        assertThatThrownBy(() -> writer.write(template, new ByteArrayOutputStream()))
                .isInstanceOf(DttFormatException.class)
                .hasMessageContaining("command");
    }

    private DttArchiveTemplate sampleTemplate() {
        return new DttArchiveTemplate(
                new DttArchiveDescriptor("DTT", "1.0", "display-type", null),
                new DeviceTypeMetadata("display-type", "Display", "Дисплей", "Табло"),
                Map.of("TicketZone", Map.of("type", "String")),
                Map.of("IP", Map.of("type", "String")),
                Map.of("hint", "value"),
                Map.of("ip", "127.0.0.1"),
                Map.of("ip", "192.168.0.10"),
                Map.of("sourceKind", "PROFILE_JSON"),
                "println 'start'",
                "println 'stop'",
                "println 'publicStart'",
                "println 'publicFinish'",
                "def f() { 1 }",
                Map.of("VISIT_CALLED", "println 'called'"),
                Map.of("RESET", "println 'reset'")
        );
    }

    private DttArchiveTemplate templateWithScriptNames(Map<String, String> eventHandlers, Map<String, String> commands) {
        return new DttArchiveTemplate(
                new DttArchiveDescriptor("DTT", "1.0", "display-type", null),
                new DeviceTypeMetadata("display-type", "Display", "Дисплей", "Табло"),
                Map.of(),
                Map.of(),
                Map.of(),
                Map.of(),
                Map.of(),
                Map.of("sourceKind", "PROFILE_JSON"),
                "",
                "",
                "",
                "",
                "",
                eventHandlers,
                commands
        );
    }

    private Map<String, String> readZipEntries(byte[] archiveBytes) throws Exception {
        final Map<String, String> result = new HashMap<>();
        try (ZipInputStream zipInputStream = new ZipInputStream(new ByteArrayInputStream(archiveBytes), StandardCharsets.UTF_8)) {
            ZipEntry entry;
            while ((entry = zipInputStream.getNextEntry()) != null) {
                if (!entry.isDirectory()) {
                    result.put(entry.getName(), new String(zipInputStream.readAllBytes(), StandardCharsets.UTF_8));
                }
            }
        }
        return result;
    }
}
