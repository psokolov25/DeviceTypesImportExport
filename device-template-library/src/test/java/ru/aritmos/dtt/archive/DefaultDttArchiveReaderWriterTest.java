package ru.aritmos.dtt.archive;

import org.junit.jupiter.api.Test;
import ru.aritmos.dtt.api.dto.DeviceTypeMetadata;
import ru.aritmos.dtt.archive.model.DttArchiveDescriptor;
import ru.aritmos.dtt.archive.model.DttArchiveTemplate;
import ru.aritmos.dtt.exception.DttFormatException;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.zip.ZipEntry;
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

    private DttArchiveTemplate sampleTemplate() {
        return new DttArchiveTemplate(
                new DttArchiveDescriptor("DTT", "1.0", "display-type", null),
                new DeviceTypeMetadata("display-type", "Display", "Дисплей", "Табло"),
                Map.of("TicketZone", Map.of("type", "String")),
                Map.of("IP", Map.of("type", "String")),
                Map.of("hint", "value"),
                Map.of("ip", "127.0.0.1"),
                Map.of("ip", "192.168.0.10"),
                "println 'start'",
                "println 'stop'",
                "println 'publicStart'",
                "println 'publicFinish'",
                "def f() { 1 }",
                Map.of("VISIT_CALLED", "println 'called'"),
                Map.of("RESET", "println 'reset'")
        );
    }
}
