package ru.aritmos.dtt.demo.controller;

import org.junit.jupiter.api.Test;
import ru.aritmos.dtt.api.dto.DeviceTypeMetadata;
import ru.aritmos.dtt.api.dto.MergeStrategy;
import ru.aritmos.dtt.archive.DefaultDttArchiveWriter;
import ru.aritmos.dtt.archive.model.DttArchiveDescriptor;
import ru.aritmos.dtt.archive.model.DttArchiveTemplate;
import ru.aritmos.dtt.demo.dto.ExportAllDttFromProfileRequest;
import ru.aritmos.dtt.demo.dto.ImportDttSetToProfileRequest;
import ru.aritmos.dtt.demo.service.DttDemoService;
import ru.aritmos.dtt.exception.DttFormatException;

import java.io.ByteArrayOutputStream;
import java.util.Base64;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DttControllerTest {

    private final DttController controller = new DttController(new DttDemoService());

    @Test
    void shouldValidateArchive() {
        final byte[] bytes = createArchiveBytes("display", "println 'ok'");

        final var response = controller.validate(bytes);

        assertThat(response.valid()).isTrue();
        assertThat(response.issues()).isEmpty();
    }

    @Test
    void shouldInspectArchive() {
        final byte[] bytes = createArchiveBytes("display", "println 'ok'");

        final var response = controller.inspect(bytes);

        assertThat(response.deviceTypeId()).isEqualTo("display");
        assertThat(response.formatName()).isEqualTo("DTT");
        assertThat(response.eventHandlersCount()).isEqualTo(1);
    }

    @Test
    void shouldImportDttSetToProfileJson() {
        final byte[] bytes = createArchiveBytes("display", "println 'ok'");
        final ImportDttSetToProfileRequest request = new ImportDttSetToProfileRequest(
                List.of(Base64.getEncoder().encodeToString(bytes)),
                MergeStrategy.FAIL_IF_EXISTS
        );

        final var response = controller.importToProfile(request);

        assertThat(response.deviceTypesCount()).isEqualTo(1);
        assertThat(response.profileJson()).contains("display");
    }

    @Test
    void shouldExportAllDttFromProfileJson() {
        final ExportAllDttFromProfileRequest request = new ExportAllDttFromProfileRequest(
                "{" +
                        "\"display\":{\"metadata\":{\"id\":\"display\",\"name\":\"Display\",\"displayName\":\"Display\",\"description\":\"desc\"},\"deviceTypeParamValues\":{}}" +
                        "}"
        );

        final var response = controller.exportAllFromProfile(request);

        assertThat(response.exportedCount()).isEqualTo(1);
        assertThat(response.archivesBase64ByDeviceTypeId()).containsKey("display");
        assertThat(response.archivesBase64ByDeviceTypeId().get("display")).isNotBlank();
    }

    @Test
    void shouldReturnActualDeviceTypesCountForReplaceStrategy() {
        final byte[] oldArchive = createArchiveBytes("display", "println 'old'");
        final byte[] replacementArchive = createArchiveBytes("display", "println 'new'");
        final ImportDttSetToProfileRequest request = new ImportDttSetToProfileRequest(
                List.of(
                        Base64.getEncoder().encodeToString(oldArchive),
                        Base64.getEncoder().encodeToString(replacementArchive)
                ),
                MergeStrategy.REPLACE
        );

        final var response = controller.importToProfile(request);

        assertThat(response.deviceTypesCount()).isEqualTo(1);
        assertThat(response.profileJson()).contains("display");
    }

    @Test
    void shouldFailOnInvalidBase64Archive() {
        final ImportDttSetToProfileRequest request = new ImportDttSetToProfileRequest(
                List.of("not-base64"),
                MergeStrategy.FAIL_IF_EXISTS
        );

        assertThatThrownBy(() -> controller.importToProfile(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Invalid Base64 archive at index 0");
    }

    @Test
    void shouldFailOnBlankProfileJson() {
        assertThatThrownBy(() -> controller.exportAllFromProfile(new ExportAllDttFromProfileRequest("  ")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("profileJson must not be blank");
    }

    @Test
    void shouldReturnStructuredBadRequestPayload() {
        final var response = controller.handleBadRequest(new IllegalArgumentException("bad payload"));

        assertThat(response.getStatus().getCode()).isEqualTo(400);
        assertThat(response.body()).isNotNull();
        assertThat(response.body().code()).isEqualTo("BAD_REQUEST");
        assertThat(response.body().message()).isEqualTo("bad payload");
    }

    @Test
    void shouldReturnStructuredFormatErrorPayload() {
        final var response = controller.handleFormatError(new DttFormatException("Ошибка парсинга profile JSON"));

        assertThat(response.getStatus().getCode()).isEqualTo(400);
        assertThat(response.body()).isNotNull();
        assertThat(response.body().code()).isEqualTo("BAD_REQUEST");
        assertThat(response.body().message()).contains("Ошибка парсинга profile JSON");
    }

    private byte[] createArchiveBytes(String deviceTypeId, String onStart) {
        final DttArchiveTemplate template = new DttArchiveTemplate(
                new DttArchiveDescriptor("DTT", "1.0", deviceTypeId),
                new DeviceTypeMetadata(deviceTypeId, "Display", "Display", "desc"),
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
