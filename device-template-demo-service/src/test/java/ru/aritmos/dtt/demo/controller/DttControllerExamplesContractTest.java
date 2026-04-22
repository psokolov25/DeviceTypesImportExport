package ru.aritmos.dtt.demo.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import ru.aritmos.dtt.api.DeviceTemplateLibrary;
import ru.aritmos.dtt.api.dto.MergeStrategy;
import ru.aritmos.dtt.demo.dto.ExportAllDttFromBranchRequest;
import ru.aritmos.dtt.demo.dto.ExportAllDttFromProfileRequest;
import ru.aritmos.dtt.demo.dto.ExportSingleDttFromBranchRequest;
import ru.aritmos.dtt.demo.dto.ExportSingleDttFromProfileRequest;
import ru.aritmos.dtt.demo.dto.ImportDttSetToBranchRequest;
import ru.aritmos.dtt.demo.dto.ImportDttSetToExistingBranchRequest;
import ru.aritmos.dtt.demo.dto.ImportDttSetToProfileRequest;
import ru.aritmos.dtt.demo.dto.ImportProfileBranchWithMetadataRequest;
import ru.aritmos.dtt.demo.openapi.DttSwaggerExamples;
import ru.aritmos.dtt.demo.service.DttDemoService;

import java.io.ByteArrayOutputStream;
import java.util.Base64;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Контрактные тесты: проверяют, что примеры из OpenAPI реально исполняются на всех ключевых REST-точках.
 */
class DttControllerExamplesContractTest {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private final DttController controller =
            new DttController(new DttDemoService(DeviceTemplateLibrary.createDefaultFacade()));

    @Test
    void shouldExecuteAllMajorEndpointsUsingOpenApiExamples() throws Exception {
        final byte[] displayDtt = decode(DttSwaggerExamples.SAMPLE_DTT_DISPLAY_BASE64);
        final byte[] terminalDtt = decode(DttSwaggerExamples.SAMPLE_DTT_TERMINAL_BASE64);

        assertThat(controller.validate(displayDtt).valid()).isTrue();
        assertThat(controller.inspect(displayDtt).deviceTypeId()).isEqualTo("display-wd3264");
        assertThat(controller.metadata(displayDtt).metadata()).hasSize(1);
        assertThat(controller.compareVersion(displayDtt, "2.1.0").greaterVersion()).isEqualTo("2.1.0");

        final ImportDttSetToProfileRequest profileStructured = OBJECT_MAPPER.readValue(
                DttSwaggerExamples.IMPORT_PROFILE_REQUEST_STRUCTURED_FULL,
                ImportDttSetToProfileRequest.class
        );
        assertThat(controller.importToProfile(profileStructured).profileJson().path("metadata").isArray()).isTrue();
        assertThat(controller.previewProfile(profileStructured).profileJson().path("metadata").isArray()).isTrue();
        assertThat(controller.previewProfileDetailed(profileStructured).profileJson().path("metadata").isArray()).isTrue();

        final byte[] profileZip = zipEntries(entry("Terminal.dtt", terminalDtt));
        assertThat(controller.importToProfileUpload(
                profileZip,
                MergeStrategy.FAIL_IF_EXISTS,
                DttSwaggerExamples.IMPORT_PROFILE_UPLOAD_METADATA_EXAMPLE
        ).profileJson().path("metadata").isArray()).isTrue();
        assertThat(controller.previewProfileUpload(
                profileZip,
                MergeStrategy.FAIL_IF_EXISTS,
                DttSwaggerExamples.IMPORT_PROFILE_UPLOAD_METADATA_EXAMPLE
        ).profileJson().path("metadata").isArray()).isTrue();
        assertThat(controller.importToProfileUploadMultipart(profileZip, DttSwaggerExamples.IMPORT_PROFILE_UPLOAD_METADATA_EXAMPLE)
                .profileJson().path("metadata").isArray()).isTrue();
        assertThat(controller.previewProfileUploadMultipart(profileZip, DttSwaggerExamples.IMPORT_PROFILE_UPLOAD_METADATA_EXAMPLE)
                .profileJson().path("metadata").isArray()).isTrue();

        final ImportDttSetToBranchRequest branchStructured = OBJECT_MAPPER.readValue(
                DttSwaggerExamples.IMPORT_BRANCH_REQUEST_STRUCTURED_FULL,
                ImportDttSetToBranchRequest.class
        );
        assertThat(controller.importToBranch(branchStructured).branchJson().path("metadata").isArray()).isTrue();
        assertThat(controller.previewBranch(branchStructured).branchJson().path("metadata").isArray()).isTrue();
        assertThat(controller.previewBranchDetailed(branchStructured).branchJson().path("metadata").isArray()).isTrue();

        final ImportDttSetToExistingBranchRequest mergeRequest = new ImportDttSetToExistingBranchRequest(
                """
                {
                  "branch-custom": {
                    "id": "branch-custom",
                    "displayName": "Отделение custom",
                    "deviceTypes": {}
                  }
                }
                """,
                List.of(DttSwaggerExamples.SAMPLE_DTT_DISPLAY_BASE64),
                List.of("branch-custom"),
                MergeStrategy.MERGE_NON_NULLS
        );
        assertThat(controller.importToExistingBranch(mergeRequest).branchJson().path("metadata").isArray()).isTrue();

        final byte[] branchZip = zipEntries(
                entry("Display WD3264.dtt", displayDtt),
                entry("Terminal.dtt", terminalDtt)
        );
        assertThat(controller.importToBranchUpload(branchZip, List.of(), MergeStrategy.FAIL_IF_EXISTS,
                DttSwaggerExamples.IMPORT_BRANCH_UPLOAD_METADATA_EXAMPLE).branchJson().path("metadata").isArray()).isTrue();
        assertThat(controller.previewBranchUpload(branchZip, List.of(), MergeStrategy.FAIL_IF_EXISTS,
                DttSwaggerExamples.IMPORT_BRANCH_UPLOAD_METADATA_EXAMPLE).branchJson().path("metadata").isArray()).isTrue();
        assertThat(controller.importToExistingBranchUpload(branchZip,
                DttSwaggerExamples.IMPORT_BRANCH_MERGE_UPLOAD_METADATA_EXAMPLE).branchJson().path("metadata").isArray()).isTrue();
        assertThat(controller.importToBranchUploadMultipart(branchZip, DttSwaggerExamples.IMPORT_BRANCH_UPLOAD_METADATA_EXAMPLE)
                .branchJson().path("metadata").isArray()).isTrue();
        assertThat(controller.previewBranchUploadMultipart(branchZip, DttSwaggerExamples.IMPORT_BRANCH_UPLOAD_METADATA_EXAMPLE)
                .branchJson().path("metadata").isArray()).isTrue();
        assertThat(controller.importToExistingBranchUploadMultipart(branchZip,
                DttSwaggerExamples.IMPORT_BRANCH_MERGE_UPLOAD_METADATA_EXAMPLE).branchJson().path("metadata").isArray()).isTrue();

        final ImportProfileBranchWithMetadataRequest profileBranchRequest = OBJECT_MAPPER.readValue(
                DttSwaggerExamples.IMPORT_PROFILE_BRANCH_REQUEST_EXAMPLE,
                ImportProfileBranchWithMetadataRequest.class
        );
        final var profileBranchResponse = controller.importProfileBranchWithMetadata(profileBranchRequest);
        assertThat(profileBranchResponse.profileJson().path("metadata").isArray()).isTrue();
        assertThat(profileBranchResponse.branchJson().path("metadata").isArray()).isTrue();

        final ExportSingleDttFromProfileRequest exportProfileOneRequest = OBJECT_MAPPER.readValue(
                DttSwaggerExamples.PROFILE_EXPORT_SINGLE_OBJECT,
                ExportSingleDttFromProfileRequest.class
        );
        assertThat(controller.exportSingleFromProfile(exportProfileOneRequest).archiveBase64()).isNotBlank();
        assertThat(controller.previewSingleExportFromProfile(exportProfileOneRequest).canExport()).isTrue();
        assertThat(controller.exportSingleFromProfileDownload(exportProfileOneRequest).body()).isNotEmpty();

        final ExportAllDttFromProfileRequest exportProfileAllRequest = new ExportAllDttFromProfileRequest(
                null,
                OBJECT_MAPPER.readTree(DttSwaggerExamples.PROFILE_EXPORT_ALL_OBJECT).path("profileJson"),
                List.of("ed650d7d-6201-42fb-a4c3-b9efb93dda0c"),
                "2.1.0"
        );
        assertThat(controller.exportAllFromProfile(exportProfileAllRequest).exportedCount()).isEqualTo(1);
        assertThat(controller.exportAllFromProfileDownload(exportProfileAllRequest).body()).isNotEmpty();

        final ExportSingleDttFromBranchRequest exportBranchOneRequest = new ExportSingleDttFromBranchRequest(
                null,
                OBJECT_MAPPER.readTree(DttSwaggerExamples.BRANCH_EXPORT_SINGLE_OBJECT)
                        .path("branchEquipment")
                        .path("branches"),
                List.of("ec8d252d-deb9-4ebb-accf-0ef7994bf17b"),
                "ed650d7d-6201-42fb-a4c3-b9efb93dda0c",
                MergeStrategy.FAIL_IF_EXISTS,
                "2.1.0"
        );
        assertThat(controller.exportSingleFromBranch(exportBranchOneRequest).archiveBase64()).isNotBlank();
        assertThat(controller.previewSingleExportFromBranch(exportBranchOneRequest).canExport()).isTrue();
        assertThat(controller.exportSingleFromBranchDownload(exportBranchOneRequest).body()).isNotEmpty();

        final ExportAllDttFromBranchRequest exportBranchAllRequest = new ExportAllDttFromBranchRequest(
                null,
                OBJECT_MAPPER.readTree(DttSwaggerExamples.BRANCH_EXPORT_OBJECT_AUTO_RESOLVE)
                        .path("branchEquipment")
                        .path("branches"),
                List.of("ec8d252d-deb9-4ebb-accf-0ef7994bf17b", "37493d1c-8282-4417-a729-dceac1f3e2b4"),
                null,
                MergeStrategy.MERGE_NON_NULLS,
                "2.1.0"
        );
        assertThat(controller.exportAllFromBranch(exportBranchAllRequest).exportedCount()).isGreaterThanOrEqualTo(1);
        assertThat(controller.exportAllFromBranchDownload(exportBranchAllRequest).body()).isNotEmpty();

        final ExportAllDttFromBranchRequest exportBranchFilteredRequest = new ExportAllDttFromBranchRequest(
                null,
                OBJECT_MAPPER.readTree(DttSwaggerExamples.BRANCH_EXPORT_JSON_STRING_FILTERED).path("branchJson"),
                List.of("ec8d252d-deb9-4ebb-accf-0ef7994bf17b"),
                List.of("ed650d7d-6201-42fb-a4c3-b9efb93dda0c"),
                MergeStrategy.MERGE_NON_NULLS,
                "2.1.0"
        );
        assertThat(controller.exportAllFromBranch(exportBranchFilteredRequest).exportedCount()).isEqualTo(1);
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
