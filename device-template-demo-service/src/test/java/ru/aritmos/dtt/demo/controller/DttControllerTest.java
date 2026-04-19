package ru.aritmos.dtt.demo.controller;

import org.junit.jupiter.api.Test;
import ru.aritmos.dtt.api.DeviceTemplateLibrary;
import ru.aritmos.dtt.api.dto.DeviceTypeMetadata;
import ru.aritmos.dtt.api.dto.MergeStrategy;
import ru.aritmos.dtt.archive.DefaultDttArchiveReader;
import ru.aritmos.dtt.archive.DefaultDttArchiveWriter;
import ru.aritmos.dtt.archive.model.DttArchiveDescriptor;
import ru.aritmos.dtt.archive.model.DttArchiveTemplate;
import ru.aritmos.dtt.demo.dto.ExportAllDttFromProfileRequest;
import ru.aritmos.dtt.demo.dto.ExportAllDttFromBranchRequest;
import ru.aritmos.dtt.demo.dto.ImportDttSetToBranchRequest;
import ru.aritmos.dtt.demo.dto.ImportDttSetToExistingBranchRequest;
import ru.aritmos.dtt.demo.dto.ImportDttSetToProfileRequest;
import ru.aritmos.dtt.demo.service.DttDemoService;
import ru.aritmos.dtt.exception.DttFormatException;
import ru.aritmos.dtt.exception.TemplateAssemblyException;
import ru.aritmos.dtt.exception.TemplateExportException;
import ru.aritmos.dtt.exception.TemplateImportException;
import ru.aritmos.dtt.exception.TemplateValidationException;
import ru.aritmos.dtt.json.branch.BranchDeviceType;
import ru.aritmos.dtt.json.branch.BranchEquipment;
import ru.aritmos.dtt.json.branch.BranchNode;
import ru.aritmos.dtt.json.branch.DeviceInstanceTemplate;
import ru.aritmos.dtt.json.profile.EquipmentProfile;
import ru.aritmos.dtt.api.dto.DeviceTypeTemplate;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DttControllerTest {

    private final DttController controller =
            new DttController(new DttDemoService(DeviceTemplateLibrary.createDefaultFacade()));

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
    void shouldPreviewDttSetToProfileJson() {
        final byte[] bytes = createArchiveBytes("display", "println 'ok'");
        final ImportDttSetToProfileRequest request = new ImportDttSetToProfileRequest(
                List.of(Base64.getEncoder().encodeToString(bytes)),
                MergeStrategy.FAIL_IF_EXISTS
        );

        final var response = controller.previewProfile(request);

        assertThat(response.deviceTypesCount()).isEqualTo(1);
        assertThat(response.profileJson()).contains("display");
    }

    @Test
    void shouldExportAllDttFromProfileJson() {
        final ExportAllDttFromProfileRequest request = new ExportAllDttFromProfileRequest(
                new EquipmentProfile(Map.of(
                        "display",
                        new DeviceTypeTemplate(new DeviceTypeMetadata("display", "Display", "Display", "desc"), Map.of())
                )),
                null,
                List.of("display"),
                null
        );

        final var response = controller.exportAllFromProfile(request);

        assertThat(response.exportedCount()).isEqualTo(1);
        assertThat(response.archivesBase64ByDeviceTypeId()).containsKey("display");
        assertThat(response.archivesBase64ByDeviceTypeId().get("display")).isNotBlank();
    }

    @Test
    void shouldImportDttSetToBranchJson() {
        final byte[] bytes = createArchiveBytes("display", "println 'ok'");
        final ImportDttSetToBranchRequest request = new ImportDttSetToBranchRequest(
                List.of(Base64.getEncoder().encodeToString(bytes)),
                List.of("branch-1", "branch-2"),
                MergeStrategy.FAIL_IF_EXISTS
        );

        final var response = controller.importToBranch(request);

        assertThat(response.branchesCount()).isEqualTo(2);
        assertThat(response.branchJson()).contains("branch-1");
        assertThat(response.branchJson()).contains("display");
    }

    @Test
    void shouldPreviewDttSetToBranchJson() {
        final byte[] bytes = createArchiveBytes("display", "println 'ok'");
        final ImportDttSetToBranchRequest request = new ImportDttSetToBranchRequest(
                List.of(Base64.getEncoder().encodeToString(bytes)),
                List.of("branch-1", "branch-2"),
                MergeStrategy.FAIL_IF_EXISTS
        );

        final var response = controller.previewBranch(request);

        assertThat(response.branchesCount()).isEqualTo(2);
        assertThat(response.branchJson()).contains("branch-1");
        assertThat(response.branchJson()).contains("display");
    }

    @Test
    void shouldImportDttSetIntoExistingBranchJson() {
        final byte[] bytes = createArchiveBytes("display", "println 'ok'");
        final String existingBranchJson = """
                {
                  "branch-1": {
                    "id": "branch-1",
                    "displayName": "Main",
                    "deviceTypes": {}
                  }
                }
                """;
        final ImportDttSetToExistingBranchRequest request = new ImportDttSetToExistingBranchRequest(
                existingBranchJson,
                List.of(Base64.getEncoder().encodeToString(bytes)),
                List.of("branch-1"),
                MergeStrategy.REPLACE
        );

        final var response = controller.importToExistingBranch(request);

        assertThat(response.branchesCount()).isEqualTo(1);
        assertThat(response.branchJson()).contains("branch-1");
        assertThat(response.branchJson()).contains("display");
    }

    @Test
    void shouldExportAllDttFromBranchJson() {
        final ExportAllDttFromBranchRequest request = new ExportAllDttFromBranchRequest(
                new BranchEquipment(Map.of(
                        "branch-1",
                        new BranchNode(
                                "branch-1",
                                "Main",
                                Map.of(
                                        "display",
                                        new BranchDeviceType(
                                                new DeviceTypeTemplate(new DeviceTypeMetadata("display", "Display", "Display", "desc"), Map.of()),
                                                Map.of("dev-1", new DeviceInstanceTemplate("dev-1", "d1", "d1", "desc", Map.of())),
                                                null,
                                                null,
                                                null,
                                                null,
                                                null,
                                                null,
                                                Map.of(),
                                                Map.of()
                                        )
                                )
                        )
                )),
                null,
                List.of("branch-1"),
                List.of("display"),
                MergeStrategy.FAIL_IF_EXISTS,
                null
        );

        final var response = controller.exportAllFromBranch(request);

        assertThat(response.exportedCount()).isEqualTo(1);
        assertThat(response.archivesBase64ByDeviceTypeId()).containsKey("display");
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
                .hasMessageContaining("Invalid Base64 DTT archive payload at index 0");
    }

    @Test
    void shouldFailOnBlankProfileJson() {
        assertThatThrownBy(() -> controller.exportAllFromProfile(new ExportAllDttFromProfileRequest(null, null, List.of(), null)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Either profile or profileJson must be provided");
    }

    @Test
    void shouldReturnStructuredErrorForAllKnownExceptionTypes() {
        assertThat(controller.handleBadRequest(new IllegalArgumentException("bad")).body())
                .isEqualTo(new ru.aritmos.dtt.demo.dto.DemoErrorResponse("BAD_REQUEST", "bad"));
        assertThat(controller.handleFormatError(new DttFormatException("format")).body())
                .isEqualTo(new ru.aritmos.dtt.demo.dto.DemoErrorResponse("BAD_REQUEST", "format"));
        assertThat(controller.handleValidationError(new TemplateValidationException("validation")).body())
                .isEqualTo(new ru.aritmos.dtt.demo.dto.DemoErrorResponse("BAD_REQUEST", "validation"));
        assertThat(controller.handleImportError(new TemplateImportException("import")).body())
                .isEqualTo(new ru.aritmos.dtt.demo.dto.DemoErrorResponse("BAD_REQUEST", "import"));
        assertThat(controller.handleExportError(new TemplateExportException("export")).body())
                .isEqualTo(new ru.aritmos.dtt.demo.dto.DemoErrorResponse("BAD_REQUEST", "export"));
        assertThat(controller.handleAssemblyError(new TemplateAssemblyException("assembly")).body())
                .isEqualTo(new ru.aritmos.dtt.demo.dto.DemoErrorResponse("BAD_REQUEST", "assembly"));
    }

    @Test
    void shouldReturnStructuredInternalErrorForUnexpectedException() {
        final var response = controller.handleUnexpectedError(new RuntimeException("boom"));

        assertThat(response.code()).isEqualTo(500);
        assertThat(response.body()).isEqualTo(new ru.aritmos.dtt.demo.dto.DemoErrorResponse("INTERNAL_ERROR", "boom"));
    }

    @Test
    void shouldFailOnBlankBranchJson() {
        assertThatThrownBy(() -> controller.exportAllFromBranch(new ExportAllDttFromBranchRequest(
                null,
                null,
                List.of(),
                List.of(),
                MergeStrategy.FAIL_IF_EXISTS,
                null
        )))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Either branchEquipment or branchJson must be provided");
    }

    @Test
    void shouldExportAllDttFromProfileJsonString() {
        final ExportAllDttFromProfileRequest request = new ExportAllDttFromProfileRequest(
                null,
                "{" +
                        "\"display\":{\"id\":\"display\",\"name\":\"Display\",\"displayName\":\"Display\",\"description\":\"desc\",\"deviceTypeParamValues\":{}}" +
                        "}",
                List.of("display"),
                null
        );

        final var response = controller.exportAllFromProfile(request);

        assertThat(response.exportedCount()).isEqualTo(1);
        assertThat(response.archivesBase64ByDeviceTypeId()).containsKey("display");
    }

    @Test
    void shouldImportProfileFromDttZipUpload() {
        final byte[] zipPayload = zipArchives(Map.of("display.dtt", createArchiveBytes("display", "println 'ok'")));

        final var response = controller.importToProfileUpload(zipPayload, MergeStrategy.FAIL_IF_EXISTS);

        assertThat(response.deviceTypesCount()).isEqualTo(1);
        assertThat(response.profileJson()).contains("display");
    }

    @Test
    void shouldPreviewProfileFromDttZipUpload() {
        final byte[] zipPayload = zipArchives(Map.of("display.dtt", createArchiveBytes("display", "println 'ok'")));

        final var response = controller.previewProfileUpload(zipPayload, MergeStrategy.FAIL_IF_EXISTS);

        assertThat(response.deviceTypesCount()).isEqualTo(1);
        assertThat(response.profileJson()).contains("display");
    }

    @Test
    void shouldImportBranchFromDttZipUpload() {
        final byte[] zipPayload = zipArchives(Map.of("display.dtt", createArchiveBytes("display", "println 'ok'")));

        final var response = controller.importToBranchUpload(zipPayload, List.of("branch-1"), MergeStrategy.FAIL_IF_EXISTS);

        assertThat(response.branchesCount()).isEqualTo(1);
        assertThat(response.branchJson()).contains("branch-1");
        assertThat(response.branchJson()).contains("display");
    }

    @Test
    void shouldPreviewBranchFromDttZipUpload() {
        final byte[] zipPayload = zipArchives(Map.of("display.dtt", createArchiveBytes("display", "println 'ok'")));

        final var response = controller.previewBranchUpload(zipPayload, List.of("branch-1"), MergeStrategy.FAIL_IF_EXISTS);

        assertThat(response.branchesCount()).isEqualTo(1);
        assertThat(response.branchJson()).contains("branch-1");
        assertThat(response.branchJson()).contains("display");
    }

    @Test
    void shouldExportProfileAsDttZipDownload() throws IOException {
        final ExportAllDttFromProfileRequest request = new ExportAllDttFromProfileRequest(
                new EquipmentProfile(Map.of(
                        "display",
                        new DeviceTypeTemplate(new DeviceTypeMetadata("display", "Display", "Display", "desc"), Map.of())
                )),
                null,
                List.of("display"),
                "2.1.0"
        );

        final var response = controller.exportAllFromProfileDownload(request);

        assertThat(response.getStatus().getCode()).isEqualTo(200);
        assertThat(response.getHeaders().get("Content-Disposition")).contains("profile-dtt-set.zip");
        assertThat(countDttEntries(response.body())).isEqualTo(1);
        final var exportedTemplate = new DefaultDttArchiveReader()
                .read(new java.io.ByteArrayInputStream(firstDttEntry(response.body())));
        assertThat(exportedTemplate.descriptor().formatVersion()).isEqualTo("1.0");
        assertThat(exportedTemplate.descriptor().deviceTypeVersion()).isEqualTo("2.1.0");
        assertThat(exportedTemplate.metadata().description()).endsWith("2.1.0");
    }

    @Test
    void shouldExportBranchAsDttZipDownload() throws IOException {
        final ExportAllDttFromBranchRequest request = new ExportAllDttFromBranchRequest(
                null,
                "{" +
                        "\"branch-1\":{" +
                        "\"id\":\"branch-1\",\"displayName\":\"Main\",\"deviceTypes\":{" +
                        "\"display\":{\"id\":\"display\",\"name\":\"Display\",\"displayName\":\"Display\",\"description\":\"desc\",\"type\":\"display\",\"deviceTypeParamValues\":{},\"devices\":{}}" +
                        "}" +
                        "}" +
                "}",
                List.of("branch-1"),
                List.of("display"),
                MergeStrategy.FAIL_IF_EXISTS,
                "2.1.0"
        );

        final var response = controller.exportAllFromBranchDownload(request);

        assertThat(response.getStatus().getCode()).isEqualTo(200);
        assertThat(response.getHeaders().get("Content-Disposition")).contains("branch-dtt-set.zip");
        assertThat(countDttEntries(response.body())).isEqualTo(1);
        final var exportedTemplate = new DefaultDttArchiveReader()
                .read(new java.io.ByteArrayInputStream(firstDttEntry(response.body())));
        assertThat(exportedTemplate.descriptor().formatVersion()).isEqualTo("1.0");
        assertThat(exportedTemplate.descriptor().deviceTypeVersion()).isEqualTo("2.1.0");
        assertThat(exportedTemplate.metadata().description()).endsWith("2.1.0");
    }

    @Test
    void shouldExportAllDttFromBranchJsonString() {
        final ExportAllDttFromBranchRequest request = new ExportAllDttFromBranchRequest(
                null,
                "{" +
                        "\"branch-1\":{" +
                        "\"id\":\"branch-1\",\"displayName\":\"Main\",\"deviceTypes\":{" +
                        "\"display\":{\"id\":\"display\",\"name\":\"Display\",\"displayName\":\"Display\",\"description\":\"desc\",\"type\":\"display\",\"deviceTypeParamValues\":{},\"devices\":{}}" +
                        "}" +
                        "}" +
                "}",
                List.of("branch-1"),
                List.of("display"),
                MergeStrategy.FAIL_IF_EXISTS,
                null
        );

        final var response = controller.exportAllFromBranch(request);

        assertThat(response.exportedCount()).isEqualTo(1);
        assertThat(response.archivesBase64ByDeviceTypeId()).containsKey("display");
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
                new DttArchiveDescriptor("DTT", "1.0", deviceTypeId, null),
                new DeviceTypeMetadata(deviceTypeId, "Display", "Display", "desc"),
                Map.of(),
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

    private byte[] zipArchives(Map<String, byte[]> files) {
        try (ByteArrayOutputStream output = new ByteArrayOutputStream();
             ZipOutputStream zipOutput = new ZipOutputStream(output)) {
            for (Map.Entry<String, byte[]> entry : files.entrySet()) {
                zipOutput.putNextEntry(new ZipEntry(entry.getKey()));
                zipOutput.write(entry.getValue());
                zipOutput.closeEntry();
            }
            zipOutput.finish();
            return output.toByteArray();
        } catch (IOException exception) {
            throw new RuntimeException(exception);
        }
    }

    private int countDttEntries(byte[] zipBytes) throws IOException {
        try (ZipInputStream input = new ZipInputStream(new java.io.ByteArrayInputStream(zipBytes))) {
            int count = 0;
            ZipEntry entry;
            while ((entry = input.getNextEntry()) != null) {
                if (!entry.isDirectory() && entry.getName().endsWith(".dtt")) {
                    count++;
                }
            }
            return count;
        }
    }

    private byte[] firstDttEntry(byte[] zipBytes) throws IOException {
        try (ZipInputStream input = new ZipInputStream(new java.io.ByteArrayInputStream(zipBytes))) {
            ZipEntry entry;
            while ((entry = input.getNextEntry()) != null) {
                if (!entry.isDirectory() && entry.getName().endsWith(".dtt")) {
                    return input.readAllBytes();
                }
            }
            throw new IllegalStateException("zip payload does not contain .dtt entry");
        }
    }
}
