package ru.aritmos.dtt.demo.controller;

import org.junit.jupiter.api.Test;
import ru.aritmos.dtt.api.dto.DeviceTypeMetadata;
import ru.aritmos.dtt.archive.DefaultDttArchiveWriter;
import ru.aritmos.dtt.archive.model.DttArchiveDescriptor;
import ru.aritmos.dtt.archive.model.DttArchiveTemplate;

import java.io.ByteArrayOutputStream;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class DttControllerTest {

    private final DttController controller = new DttController();

    @Test
    void shouldValidateArchive() {
        final byte[] bytes = createArchiveBytes("println 'ok'");

        final var response = controller.validate(bytes);

        assertThat(response.valid()).isTrue();
        assertThat(response.issues()).isEmpty();
    }

    @Test
    void shouldInspectArchive() {
        final byte[] bytes = createArchiveBytes("println 'ok'");

        final var response = controller.inspect(bytes);

        assertThat(response.deviceTypeId()).isEqualTo("display");
        assertThat(response.formatName()).isEqualTo("DTT");
        assertThat(response.eventHandlersCount()).isEqualTo(1);
    }

    private byte[] createArchiveBytes(String onStart) {
        final DttArchiveTemplate template = new DttArchiveTemplate(
                new DttArchiveDescriptor("DTT", "1.0", "display"),
                new DeviceTypeMetadata("display", "Display", "Display", "desc"),
                Map.of(),
                Map.of(),
                Map.of(),
                Map.of(),
                Map.of(),
                onStart,
                null,
                null,
                null,
                null,
                Map.of("EVENT", "println 'e'"),
                Map.of("RESET", "println 'r'")
        );
        final ByteArrayOutputStream output = new ByteArrayOutputStream();
        new DefaultDttArchiveWriter().write(template, output);
        return output.toByteArray();
    }
}
