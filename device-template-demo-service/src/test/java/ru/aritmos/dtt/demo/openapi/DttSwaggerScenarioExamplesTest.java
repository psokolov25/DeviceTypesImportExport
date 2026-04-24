package ru.aritmos.dtt.demo.openapi;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import ru.aritmos.dtt.api.dto.MergeStrategy;
import ru.aritmos.dtt.demo.controller.DttController;
import ru.aritmos.dtt.demo.dto.ImportDttSetToBranchRequest;
import ru.aritmos.dtt.demo.dto.ImportDttSetToProfileRequest;
import ru.aritmos.dtt.demo.dto.ImportDttZipToBranchUploadRequest;
import ru.aritmos.dtt.demo.dto.ImportDttZipToProfileUploadRequest;
import ru.aritmos.dtt.demo.dto.ImportProfileBranchWithMetadataRequest;
import ru.aritmos.dtt.demo.service.DttDemoService;
import ru.aritmos.dtt.api.DeviceTemplateLibrary;

import java.io.ByteArrayOutputStream;
import java.util.Base64;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static org.assertj.core.api.Assertions.assertThat;

class DttSwaggerScenarioExamplesTest {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private final DttController controller =
            new DttController(new DttDemoService(DeviceTemplateLibrary.createDefaultFacade()));

    @Test
    void shouldExecuteStructuredProfileExample() throws Exception {
        final ImportDttSetToProfileRequest request = OBJECT_MAPPER.readValue(
                DttSwaggerExamples.IMPORT_PROFILE_REQUEST_STRUCTURED_FULL,
                ImportDttSetToProfileRequest.class
        );

        final var response = controller.importToProfile(request);

        assertThat(response.deviceTypesCount()).isEqualTo(1);
        assertThat(response.profileJson().toString()).contains("TicketZone");
        assertThat(response.profileJson().toString()).contains("ServicePointNameZone");
        assertThat(response.profileJson().toString()).contains("display");
    }

    @Test
    void shouldExecuteProfileUploadMetadataExample() {
        final byte[] zipPayload = zipEntries(
                entry("Terminal.dtt", decode(DttSwaggerExamples.SAMPLE_DTT_TERMINAL_BASE64))
        );

        final var response = controller.importToProfileUpload(
                zipPayload,
                MergeStrategy.FAIL_IF_EXISTS,
                DttSwaggerExamples.IMPORT_PROFILE_UPLOAD_METADATA_EXAMPLE
        );

        assertThat(response.deviceTypesCount()).isEqualTo(1);
        assertThat(response.profileJson().toString()).contains("OVR");
        assertThat(response.profileJson().toString()).contains("printerServiceURL");
    }

    @Test
    void shouldExecuteStructuredBranchExample() throws Exception {
        final ImportDttSetToBranchRequest request = OBJECT_MAPPER.readValue(
                DttSwaggerExamples.IMPORT_BRANCH_REQUEST_STRUCTURED_FULL,
                ImportDttSetToBranchRequest.class
        );

        final var response = controller.importToBranch(request);

        assertThat(response.branchesCount()).isEqualTo(1);
        assertThat(response.metadata()).isNotEmpty();
        assertThat(response.branchJson().path("metadata").isArray()).isTrue();
        assertThat(response.branchJson().toString()).contains("TicketZone");
        assertThat(response.branchJson().toString()).contains("display-1");
    }

    @Test
    void shouldExecuteBranchUploadMetadataExample() {
        final byte[] zipPayload = zipEntries(
                entry("Display WD3264.dtt", decode(DttSwaggerExamples.SAMPLE_DTT_DISPLAY_BASE64)),
                entry("Terminal.dtt", decode(DttSwaggerExamples.SAMPLE_DTT_TERMINAL_BASE64))
        );

        final var response = controller.importToBranchUpload(
                zipPayload,
                List.of(),
                MergeStrategy.FAIL_IF_EXISTS,
                DttSwaggerExamples.IMPORT_BRANCH_UPLOAD_METADATA_EXAMPLE
        );

        assertThat(response.branchesCount()).isEqualTo(1);
        assertThat(response.metadata()).hasSize(2);
        assertThat(response.branchJson().path("metadata").size()).isEqualTo(2);
        assertThat(response.branchJson().toString()).contains("OVR");
        assertThat(response.branchJson().toString()).contains("10.10.10.10");
    }

    @Test
    void shouldExecuteProfileBranchExample() throws Exception {
        final ImportProfileBranchWithMetadataRequest request = OBJECT_MAPPER.readValue(
                DttSwaggerExamples.IMPORT_PROFILE_BRANCH_REQUEST_EXAMPLE,
                ImportProfileBranchWithMetadataRequest.class
        );

        final var response = controller.importProfileBranchWithMetadata(request);

        assertThat(response.profileJson().toString()).contains("Дисплей с красным названием окна");
        assertThat(response.profileJson().toString()).contains("Дисплей с синим названием окна");
        assertThat(response.branchJson().toString()).contains("Дисплей красного окна (СПб Петроград)");
        assertThat(response.branchJson().toString()).contains("blue-1");
        assertThat(response.branchJson().path("metadata").isArray()).isTrue();
        assertThat(response.branchJson().path("metadata").size()).isGreaterThanOrEqualTo(2);
    }

    private static byte[] decode(String base64) {
        return Base64.getDecoder().decode(base64);
    }

    private static ZipEntryPayload entry(String name, byte[] payload) {
        return new ZipEntryPayload(name, payload);
    }

    private static byte[] zipEntries(ZipEntryPayload... entries) {
        try {
            final ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            try (ZipOutputStream zipOutputStream = new ZipOutputStream(outputStream)) {
                for (ZipEntryPayload entry : entries) {
                    zipOutputStream.putNextEntry(new ZipEntry(entry.name()));
                    zipOutputStream.write(entry.payload());
                    zipOutputStream.closeEntry();
                }
            }
            return outputStream.toByteArray();
        } catch (Exception exception) {
            throw new RuntimeException(exception);
        }
    }

    private record ZipEntryPayload(String name, byte[] payload) {
    }
}
