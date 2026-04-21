package ru.aritmos.dtt.demo.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
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
import ru.aritmos.dtt.demo.dto.ExportSingleDttFromBranchRequest;
import ru.aritmos.dtt.demo.dto.ExportSingleDttFromProfileRequest;
import ru.aritmos.dtt.demo.dto.ImportBranchDeviceRequest;
import ru.aritmos.dtt.demo.dto.ImportBranchDeviceTypeRequest;
import ru.aritmos.dtt.demo.dto.ImportBranchRequest;
import ru.aritmos.dtt.demo.dto.ImportDeviceTypeMetadataOverrideRequest;
import ru.aritmos.dtt.demo.dto.ImportDttSetToBranchRequest;
import ru.aritmos.dtt.demo.dto.ImportDttSetToExistingBranchRequest;
import ru.aritmos.dtt.demo.dto.ImportDttSetToProfileRequest;
import ru.aritmos.dtt.demo.dto.ImportProfileDeviceTypeRequest;
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

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

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
        assertThat(response.profileJson().toString()).contains("display");
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
        assertThat(response.profileJson().toString()).contains("display");
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
    void shouldExportSingleDttFromProfileJson() {
        final ExportSingleDttFromProfileRequest request = new ExportSingleDttFromProfileRequest(
                new EquipmentProfile(Map.of(
                        "display",
                        new DeviceTypeTemplate(new DeviceTypeMetadata("display", "Display", "Display", "desc"), Map.of())
                )),
                null,
                "display",
                null
        );

        final var response = controller.exportSingleFromProfile(request);

        assertThat(response.deviceTypeId()).isEqualTo("display");
        assertThat(response.archiveBase64()).isNotBlank();
    }

    @Test
    void shouldExportSingleDttFromProfileAsDownload() {
        final ExportSingleDttFromProfileRequest request = new ExportSingleDttFromProfileRequest(
                new EquipmentProfile(Map.of(
                        "display",
                        new DeviceTypeTemplate(new DeviceTypeMetadata("display", "Display", "Display", "desc"), Map.of())
                )),
                null,
                "display",
                "2.1.0"
        );

        final var response = controller.exportSingleFromProfileDownload(request);

        assertThat(response.getStatus().getCode()).isEqualTo(200);
        assertThat(response.getHeaders().get("Content-Disposition")).contains("Display.dtt");
        final var exportedTemplate = new DefaultDttArchiveReader()
                .read(new java.io.ByteArrayInputStream(response.body()));
        assertThat(exportedTemplate.metadata().id()).isEqualTo("display");
    }

    @Test
    void shouldPreviewSingleExportFromProfile() {
        final ExportSingleDttFromProfileRequest request = new ExportSingleDttFromProfileRequest(
                new EquipmentProfile(Map.of(
                        "display",
                        new DeviceTypeTemplate(new DeviceTypeMetadata("display", "Display", "Display", "desc"), Map.of())
                )),
                null,
                "display",
                null
        );

        final var response = controller.previewSingleExportFromProfile(request);

        assertThat(response.canExport()).isTrue();
        assertThat(response.deviceTypeId()).isEqualTo("display");
        assertThat(response.archiveSizeBytes()).isNotNull();
        assertThat(response.archiveSizeBytes()).isGreaterThan(100);
        assertThat(response.issues()).isEmpty();
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
        assertThat(response.branchJson().toString()).contains("branch-1");
        assertThat(response.branchJson().toString()).contains("display");
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
        assertThat(response.branchJson().toString()).contains("branch-1");
        assertThat(response.branchJson().toString()).contains("display");
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
        assertThat(response.branchJson().toString()).contains("branch-1");
        assertThat(response.branchJson().toString()).contains("display");
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
    void shouldExportSingleDttFromBranchJson() {
        final ExportSingleDttFromBranchRequest request = new ExportSingleDttFromBranchRequest(
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
                "display",
                MergeStrategy.FAIL_IF_EXISTS,
                null
        );

        final var response = controller.exportSingleFromBranch(request);

        assertThat(response.deviceTypeId()).isEqualTo("display");
        assertThat(response.archiveBase64()).isNotBlank();
    }

    @Test
    void shouldExportSingleDttFromBranchAsDownload() {
        final ExportSingleDttFromBranchRequest request = new ExportSingleDttFromBranchRequest(
                null,
                json("{" +
                        "\"branch-1\":{" +
                        "\"id\":\"branch-1\",\"displayName\":\"Main\",\"deviceTypes\":{" +
                        "\"display\":{\"id\":\"display\",\"name\":\"Display\",\"displayName\":\"Display\",\"description\":\"desc\",\"type\":\"display\",\"deviceTypeParamValues\":{},\"devices\":{}}" +
                        "}" +
                        "}" +
                        "}"),
                List.of("branch-1"),
                "display",
                MergeStrategy.FAIL_IF_EXISTS,
                null
        );

        final var response = controller.exportSingleFromBranchDownload(request);

        assertThat(response.getStatus().getCode()).isEqualTo(200);
        assertThat(response.getHeaders().get("Content-Disposition")).contains("Display.dtt");
        final var exportedTemplate = new DefaultDttArchiveReader()
                .read(new java.io.ByteArrayInputStream(response.body()));
        assertThat(exportedTemplate.metadata().id()).isEqualTo("display");
    }

    @Test
    void shouldReturnMergeConflictDiagnosticsForBranchPreviewSingleExport() {
        final ExportSingleDttFromBranchRequest request = new ExportSingleDttFromBranchRequest(
                null,
                json("{" +
                        "\"branch-1\":{" +
                        "\"id\":\"branch-1\",\"displayName\":\"Main\",\"deviceTypes\":{" +
                        "\"display\":{\"id\":\"display\",\"name\":\"Display\",\"displayName\":\"Display\",\"description\":\"desc\",\"type\":\"display\",\"deviceTypeParamValues\":{},\"devices\":{}}" +
                        "}" +
                        "}," +
                        "\"branch-2\":{" +
                        "\"id\":\"branch-2\",\"displayName\":\"Backup\",\"deviceTypes\":{" +
                        "\"display\":{\"id\":\"display\",\"name\":\"Display\",\"displayName\":\"Display\",\"description\":\"desc\",\"type\":\"display\",\"deviceTypeParamValues\":{},\"devices\":{}}" +
                        "}" +
                        "}" +
                        "}"),
                null,
                "display",
                MergeStrategy.FAIL_IF_EXISTS,
                null
        );

        final var response = controller.previewSingleExportFromBranch(request);

        assertThat(response.canExport()).isFalse();
        assertThat(response.deviceTypeId()).isEqualTo("display");
        assertThat(response.archiveSizeBytes()).isNull();
        assertThat(response.issues()).isNotEmpty();
        assertThat(response.issues().get(0).code()).isEqualTo("MERGE_CONFLICT");
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
        assertThat(response.profileJson().toString()).contains("display");
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
    void shouldFailOnBlankDeviceTypeIdForSingleProfileExport() {
        assertThatThrownBy(() -> controller.exportSingleFromProfile(
                new ExportSingleDttFromProfileRequest(new EquipmentProfile(Map.of()), null, " ", null)
        ))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("deviceTypeId must not be blank");
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
    void shouldFailOnBlankDeviceTypeIdForSingleBranchExport() {
        assertThatThrownBy(() -> controller.exportSingleFromBranch(
                new ExportSingleDttFromBranchRequest(new BranchEquipment(Map.of()), null, List.of(), "", MergeStrategy.FAIL_IF_EXISTS, null)
        ))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("deviceTypeId must not be blank");
    }

    @Test
    void shouldExportAllDttFromProfileJsonString() {
        final ExportAllDttFromProfileRequest request = new ExportAllDttFromProfileRequest(
                null,
                json("{" +
                        "\"display\":{\"id\":\"display\",\"name\":\"Display\",\"displayName\":\"Display\",\"description\":\"desc\",\"deviceTypeParamValues\":{}}" +
                        "}"),
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
        assertThat(response.profileJson().toString()).contains("display");
    }

    @Test
    void shouldPreviewProfileFromDttZipUpload() {
        final byte[] zipPayload = zipArchives(Map.of("display.dtt", createArchiveBytes("display", "println 'ok'")));

        final var response = controller.previewProfileUpload(zipPayload, MergeStrategy.FAIL_IF_EXISTS);

        assertThat(response.deviceTypesCount()).isEqualTo(1);
        assertThat(response.profileJson().toString()).contains("display");
    }

    @Test
    void shouldImportBranchFromDttZipUpload() {
        final byte[] zipPayload = zipArchives(Map.of("display.dtt", createArchiveBytes("display", "println 'ok'")));

        final var response = controller.importToBranchUpload(zipPayload, List.of("branch-1"), MergeStrategy.FAIL_IF_EXISTS);

        assertThat(response.branchesCount()).isEqualTo(1);
        assertThat(response.branchJson().toString()).contains("branch-1");
        assertThat(response.branchJson().toString()).contains("display");
    }

    @Test
    void shouldPreviewBranchFromDttZipUpload() {
        final byte[] zipPayload = zipArchives(Map.of("display.dtt", createArchiveBytes("display", "println 'ok'")));

        final var response = controller.previewBranchUpload(zipPayload, List.of("branch-1"), MergeStrategy.FAIL_IF_EXISTS);

        assertThat(response.branchesCount()).isEqualTo(1);
        assertThat(response.branchJson().toString()).contains("branch-1");
        assertThat(response.branchJson().toString()).contains("display");
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
        assertThat(firstDttEntryName(response.body())).isEqualTo("Display.dtt");
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
                json("{" +
                        "\"branch-1\":{" +
                        "\"id\":\"branch-1\",\"displayName\":\"Main\",\"deviceTypes\":{" +
                        "\"display\":{\"id\":\"display\",\"name\":\"Display\",\"displayName\":\"Display\",\"description\":\"desc\",\"type\":\"display\",\"deviceTypeParamValues\":{},\"devices\":{}}" +
                        "}" +
                        "}" +
                "}"),
                List.of("branch-1"),
                List.of("display"),
                MergeStrategy.FAIL_IF_EXISTS,
                "2.1.0"
        );

        final var response = controller.exportAllFromBranchDownload(request);

        assertThat(response.getStatus().getCode()).isEqualTo(200);
        assertThat(response.getHeaders().get("Content-Disposition")).contains("branch-dtt-set.zip");
        assertThat(countDttEntries(response.body())).isEqualTo(1);
        assertThat(firstDttEntryName(response.body())).isEqualTo("Display.dtt");
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
                json("{" +
                        "\"branch-1\":{" +
                        "\"id\":\"branch-1\",\"displayName\":\"Main\",\"deviceTypes\":{" +
                        "\"display\":{\"id\":\"display\",\"name\":\"Display\",\"displayName\":\"Display\",\"description\":\"desc\",\"type\":\"display\",\"deviceTypeParamValues\":{},\"devices\":{}}" +
                        "}" +
                        "}" +
                "}"),
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

    @Test
    void shouldImportDttSetToProfileWithDeviceTypeParamOverrides() {
        final byte[] bytes = createArchiveBytes("display", "println 'ok'");
        final ImportDttSetToProfileRequest request = new ImportDttSetToProfileRequest(
                List.of(),
                MergeStrategy.FAIL_IF_EXISTS,
                List.of(new ImportProfileDeviceTypeRequest(
                        Base64.getEncoder().encodeToString(bytes),
                        null,
                        Map.of("printerServiceURL", "http://override.local:8084", "prefix", "OVR")
                ))
        );

        final var response = controller.importToProfile(request);

        assertThat(response.deviceTypesCount()).isEqualTo(1);
        assertThat(response.profileJson().toString()).contains("printerServiceURL");
        assertThat(response.profileJson().toString()).contains("http://override.local:8084");
        assertThat(response.profileJson().toString()).contains("OVR");
    }

    @Test
    void shouldImportDttSetToBranchWithDeviceAndDeviceTypeOverrides() {
        final byte[] bytes = createArchiveBytes("display", "println 'ok'");
        final ImportDttSetToBranchRequest request = new ImportDttSetToBranchRequest(
                List.of(),
                List.of(),
                MergeStrategy.FAIL_IF_EXISTS,
                List.of(new ImportBranchRequest(
                        "branch-custom",
                        "Отделение custom",
                        List.of(new ImportBranchDeviceTypeRequest(
                                Base64.getEncoder().encodeToString(bytes),
                                null,
                                Map.of("TicketZone", "9"),
                                List.of(new ImportBranchDeviceRequest(
                                        "display-1",
                                        "display-1",
                                        "Display 1",
                                        "Demo display",
                                        Map.of("IP", "10.10.10.10", "Port", 22224)
                                )),
                                null,
                                "display"
                        ))
                ))
        );

        final var response = controller.importToBranch(request);

        assertThat(response.branchesCount()).isEqualTo(1);
        assertThat(response.branchJson().toString()).contains("branch-custom");
        assertThat(response.branchJson().toString()).contains("TicketZone");
        assertThat(response.branchJson().toString()).contains("10.10.10.10");
        assertThat(response.branchJson().toString()).contains("display-1");
    }

    @Test
    void shouldImportSameDttToProfileAsSeveralDerivedDeviceTypes() {
        final byte[] bytes = createArchiveBytes("display-wd3264", "println 'ok'");
        final String base64 = Base64.getEncoder().encodeToString(bytes);
        final ImportDttSetToProfileRequest request = new ImportDttSetToProfileRequest(
                List.of(),
                MergeStrategy.FAIL_IF_EXISTS,
                List.of(
                        new ImportProfileDeviceTypeRequest(
                                base64,
                                new ImportDeviceTypeMetadataOverrideRequest(
                                        "display-wd3264-red-window",
                                        "Display WD3264 Красное окно",
                                        "Display WD3264 Красное окно",
                                        "Красное окно"
                                ),
                                Map.of("FirstZoneColor", "red")
                        ),
                        new ImportProfileDeviceTypeRequest(
                                base64,
                                new ImportDeviceTypeMetadataOverrideRequest(
                                        "display-wd3264-blue-window",
                                        "Display WD3264 Синее окно",
                                        "Display WD3264 Синее окно",
                                        "Синее окно"
                                ),
                                Map.of("FirstZoneColor", "blue")
                        )
                )
        );

        final var response = controller.importToProfile(request);

        assertThat(response.deviceTypesCount()).isEqualTo(2);
        assertThat(response.profileJson().toString()).contains("display-wd3264-red-window");
        assertThat(response.profileJson().toString()).contains("display-wd3264-blue-window");
        assertThat(response.profileJson().toString()).contains("Display WD3264 Красное окно");
        assertThat(response.profileJson().toString()).contains("Display WD3264 Синее окно");
        assertThat(response.profileJson().toString()).contains("red");
        assertThat(response.profileJson().toString()).contains("blue");
    }

    @Test
    void shouldImportSameDttToBranchAsSeveralDerivedDeviceTypesWithDifferentDeviceSets() {
        final byte[] bytes = createArchiveBytes("display-wd3264", "println 'ok'");
        final String base64 = Base64.getEncoder().encodeToString(bytes);
        final ImportDttSetToBranchRequest request = new ImportDttSetToBranchRequest(
                List.of(),
                List.of(),
                MergeStrategy.FAIL_IF_EXISTS,
                List.of(new ImportBranchRequest(
                        "branch-custom",
                        "Отделение custom",
                        List.of(
                                new ImportBranchDeviceTypeRequest(
                                        base64,
                                        new ImportDeviceTypeMetadataOverrideRequest(
                                                "display-wd3264-red-window",
                                                "Display WD3264 Красное окно",
                                                "Display WD3264 Красное окно",
                                                "Красное окно"
                                        ),
                                        Map.of("FirstZoneColor", "red"),
                                        List.of(
                                                new ImportBranchDeviceRequest("red-1", "red-1", "Red 1", "Red display 1", Map.of("IP", "10.10.10.11")),
                                                new ImportBranchDeviceRequest("red-2", "red-2", "Red 2", "Red display 2", Map.of("IP", "10.10.10.12"))
                                        ),
                                        true,
                                        "display"
                                ),
                                new ImportBranchDeviceTypeRequest(
                                        base64,
                                        new ImportDeviceTypeMetadataOverrideRequest(
                                                "display-wd3264-blue-window",
                                                "Display WD3264 Синее окно",
                                                "Display WD3264 Синее окно",
                                                "Синее окно"
                                        ),
                                        Map.of("FirstZoneColor", "blue"),
                                        List.of(
                                                new ImportBranchDeviceRequest("blue-1", "blue-1", "Blue 1", "Blue display 1", Map.of("IP", "10.10.10.21")),
                                                new ImportBranchDeviceRequest("blue-2", "blue-2", "Blue 2", "Blue display 2", Map.of("IP", "10.10.10.22")),
                                                new ImportBranchDeviceRequest("blue-3", "blue-3", "Blue 3", "Blue display 3", Map.of("IP", "10.10.10.23"))
                                        ),
                                        true,
                                        "display"
                                )
                        )
                ))
        );

        final var response = controller.importToBranch(request);

        assertThat(response.branchesCount()).isEqualTo(1);
        assertThat(response.branchJson().toString()).contains("display-wd3264-red-window");
        assertThat(response.branchJson().toString()).contains("display-wd3264-blue-window");
        assertThat(response.branchJson().toString()).contains("red-1");
        assertThat(response.branchJson().toString()).contains("red-2");
        assertThat(response.branchJson().toString()).contains("blue-1");
        assertThat(response.branchJson().toString()).contains("blue-2");
        assertThat(response.branchJson().toString()).contains("blue-3");
        assertThat(response.branchJson().toString()).contains("red");
        assertThat(response.branchJson().toString()).contains("blue");
    }



    @Test
    void shouldImportProfileZipUploadMultipartWithMetadata() {
        final byte[] zipBytes = zipArchives(Map.of(
                "Terminal.dtt", createArchiveBytes("terminal", "println 'ok'")
        ));
        final String metadataJson = """
                {
                  "mergeStrategy": "FAIL_IF_EXISTS",
                  "deviceTypes": [
                    {
                      "archiveEntryName": "Terminal.dtt",
                      "deviceTypeParamValues": {
                        "prefix": "OVR",
                        "printerServiceURL": "http://10.10.10.10:8084"
                      }
                    }
                  ]
                }
                """;

        final var response = controller.importToProfileUploadMultipart(zipBytes, metadataJson);

        assertThat(response.deviceTypesCount()).isEqualTo(1);
        assertThat(response.profileJson().toString()).contains("OVR");
        assertThat(response.profileJson().toString()).contains("http://10.10.10.10:8084");
    }

    @Test
    void shouldImportBranchZipUploadMultipartWithMetadata() {
        final byte[] zipBytes = zipArchives(Map.of(
                "Display WD3264.dtt", createArchiveBytes("display-wd3264", "println 'ok'"),
                "Terminal.dtt", createArchiveBytes("terminal", "println 'ok'")
        ));
        final String metadataJson = """
                {
                  "mergeStrategy": "FAIL_IF_EXISTS",
                  "branches": [
                    {
                      "branchId": "branch-custom",
                      "displayName": "Отделение custom",
                      "deviceTypes": [
                        {
                          "archiveEntryName": "Display WD3264.dtt",
                          "kind": "display",
                          "deviceTypeParamValues": {
                            "TicketZone": "9"
                          },
                          "devices": [
                            {
                              "id": "display-1",
                              "name": "display-1",
                              "displayName": "Display 1",
                              "deviceParamValues": {
                                "IP": "10.10.10.10",
                                "Port": 22224
                              }
                            }
                          ]
                        },
                        {
                          "archiveEntryName": "Terminal.dtt",
                          "deviceTypeParamValues": {
                            "prefix": "OVR"
                          }
                        }
                      ]
                    }
                  ]
                }
                """;

        final var response = controller.importToBranchUploadMultipart(zipBytes, metadataJson);

        assertThat(response.branchesCount()).isEqualTo(1);
        assertThat(response.branchJson().toString()).contains("branch-custom");
        assertThat(response.branchJson().toString()).contains("10.10.10.10");
        assertThat(response.branchJson().toString()).contains("TicketZone");
        assertThat(response.branchJson().toString()).contains("OVR");
    }

    private com.fasterxml.jackson.databind.JsonNode json(String rawJson) {
        try {
            return OBJECT_MAPPER.readTree(rawJson);
        } catch (IOException exception) {
            throw new RuntimeException(exception);
        }
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


    private String firstDttEntryName(byte[] zipBytes) throws IOException {
        try (ZipInputStream input = new ZipInputStream(new java.io.ByteArrayInputStream(zipBytes))) {
            ZipEntry entry;
            while ((entry = input.getNextEntry()) != null) {
                if (!entry.isDirectory() && entry.getName().endsWith(".dtt")) {
                    return entry.getName();
                }
            }
            throw new IllegalStateException("zip payload does not contain .dtt entry");
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
