package ru.aritmos.dtt.api;

import org.junit.jupiter.api.Test;
import ru.aritmos.dtt.api.dto.BatchDttExportResult;
import ru.aritmos.dtt.api.dto.DeviceTypeMetadata;
import ru.aritmos.dtt.api.dto.EquipmentProfileAssemblyRequest;
import ru.aritmos.dtt.api.dto.EquipmentProfileDeviceTypeRequest;
import ru.aritmos.dtt.api.dto.MergeStrategy;
import ru.aritmos.dtt.api.dto.ProfileExportRequest;
import ru.aritmos.dtt.api.dto.branch.BranchDeviceTypeImportRequest;
import ru.aritmos.dtt.api.dto.branch.BranchEquipmentAssemblyRequest;
import ru.aritmos.dtt.api.dto.branch.BranchEquipmentExportRequest;
import ru.aritmos.dtt.api.dto.branch.BranchImportRequest;
import ru.aritmos.dtt.archive.model.DttArchiveDescriptor;
import ru.aritmos.dtt.archive.model.DttArchiveTemplate;
import ru.aritmos.dtt.json.branch.BranchEquipment;
import ru.aritmos.dtt.json.profile.EquipmentProfile;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DefaultDeviceTemplateLibraryFacadeTest {

    private final DeviceTemplateLibraryFacade facade = DeviceTemplateLibrary.createDefaultFacade();

    @Test
    void shouldReadWriteAndValidateArchiveViaFacade() {
        final DttArchiveTemplate template = template();

        final byte[] bytes = facade.writeDtt(template);
        final DttArchiveTemplate restored = facade.readDtt(bytes);

        assertThat(restored.metadata().id()).isEqualTo("display");
        assertThat(facade.validate(bytes).valid()).isTrue();
    }

    @Test
    void shouldCompareInputVersionWithVersionFromDttViaFacade() {
        final byte[] bytes = facade.writeDtt(template());

        final var result = facade.compareDttVersion(bytes, "2.0.0");

        assertThat(result.inputVersion()).isEqualTo("2.0.0");
        assertThat(result.dttVersion()).isEqualTo("1.0");
        assertThat(result.greaterVersion()).isEqualTo("2.0.0");
        assertThat(result.greaterSource()).isEqualTo("INPUT");
    }

    @Test
    void shouldAssembleProfileAndBranchViaFacade() {
        final var deviceTypeTemplate = new ru.aritmos.dtt.api.dto.DeviceTypeTemplate(
                new DeviceTypeMetadata("display", "Display", "Display", "desc"),
                Map.of("ip", "127.0.0.1")
        );

        final var profile = facade.assembleProfile(new EquipmentProfileAssemblyRequest(
                List.of(new EquipmentProfileDeviceTypeRequest(deviceTypeTemplate, true)),
                List.of(),
                MergeStrategy.FAIL_IF_EXISTS
        ));
        assertThat(profile.deviceTypes()).containsKey("display");
        final String profileJson = facade.toProfileJson(profile);
        assertThat(profileJson).contains("display");
        assertThat(facade.parseProfileJson(profileJson).deviceTypes()).containsKey("display");

        final var branch = facade.assembleBranch(new BranchEquipmentAssemblyRequest(
                List.of(new BranchImportRequest(
                        "branch-1",
                        "Main",
                        List.of(new BranchDeviceTypeImportRequest(
                                new EquipmentProfileDeviceTypeRequest(deviceTypeTemplate, true),
                                List.of(),
                                null, null, null, null, null, null, Map.of(), Map.of()
                        ))
                )),
                MergeStrategy.FAIL_IF_EXISTS
        ));

        assertThat(branch.branches()).containsKey("branch-1");
        final String branchJson = facade.toBranchJson(branch);
        assertThat(branchJson).contains("branch-1");
        assertThat(facade.parseBranchJson(branchJson).branches()).containsKey("branch-1");
    }


    @Test
    void shouldExportAndImportDttSetFromProfile() {
        final var deviceTypeTemplate = new ru.aritmos.dtt.api.dto.DeviceTypeTemplate(
                new DeviceTypeMetadata("display", "Display", "Display", "desc"),
                Map.of("ip", "127.0.0.1")
        );
        final var profile = facade.assembleProfile(new EquipmentProfileAssemblyRequest(
                List.of(new EquipmentProfileDeviceTypeRequest(deviceTypeTemplate, true)),
                List.of(),
                MergeStrategy.FAIL_IF_EXISTS
        ));

        final var batch = facade.exportDttSetFromProfile(profile);
        final var imported = facade.importDttSetToProfile(List.copyOf(batch.archivesByDeviceTypeId().values()), MergeStrategy.FAIL_IF_EXISTS);

        assertThat(batch.archivesByDeviceTypeId()).containsKey("display");
        assertThat(imported.deviceTypes()).containsKey("display");
    }

    @Test
    void shouldExportAndImportDttSetFromBranch() {
        final var deviceTypeTemplate = new ru.aritmos.dtt.api.dto.DeviceTypeTemplate(
                new DeviceTypeMetadata("display", "Display", "Display", "desc"),
                Map.of("ip", "127.0.0.1")
        );
        final var branch = facade.assembleBranch(new BranchEquipmentAssemblyRequest(
                List.of(new BranchImportRequest(
                        "branch-1",
                        "Main",
                        List.of(new BranchDeviceTypeImportRequest(
                                new EquipmentProfileDeviceTypeRequest(deviceTypeTemplate, true),
                                List.of(),
                                null, null, null, null, null, null, Map.of(), Map.of()
                        ))
                )),
                MergeStrategy.FAIL_IF_EXISTS
        ));

        final var exported = facade.exportDttSetFromBranch(branch, MergeStrategy.FAIL_IF_EXISTS);
        final var imported = facade.importDttSetToBranch(
                List.copyOf(exported.archivesByDeviceTypeId().values()),
                List.of("branch-a", "branch-b"),
                MergeStrategy.FAIL_IF_EXISTS
        );

        assertThat(exported.archivesByDeviceTypeId()).containsKey("display");
        assertThat(imported.branches()).containsKeys("branch-a", "branch-b");
        assertThat(imported.branches().get("branch-a").deviceTypes()).containsKey("display");
    }

    @Test
    void shouldExportSubsetFromProfileByDeviceTypeIds() {
        final var first = new ru.aritmos.dtt.api.dto.DeviceTypeTemplate(
                new DeviceTypeMetadata("display", "Display", "Display", "desc"),
                Map.of("ip", "127.0.0.1")
        );
        final var second = new ru.aritmos.dtt.api.dto.DeviceTypeTemplate(
                new DeviceTypeMetadata("cashbox", "Cashbox", "Cashbox", "desc"),
                Map.of("port", 1001)
        );
        final var profile = facade.assembleProfile(new EquipmentProfileAssemblyRequest(
                List.of(
                        new EquipmentProfileDeviceTypeRequest(first, true),
                        new EquipmentProfileDeviceTypeRequest(second, true)
                ),
                List.of(),
                MergeStrategy.FAIL_IF_EXISTS
        ));

        final var exported = facade.exportDttSetFromProfile(new ProfileExportRequest(profile, List.of("cashbox"), null));

        assertThat(exported.archivesByDeviceTypeId()).containsOnlyKeys("cashbox");
    }

    @Test
    void shouldFailOnUnknownBranchInBranchExportRequest() {
        final var deviceTypeTemplate = new ru.aritmos.dtt.api.dto.DeviceTypeTemplate(
                new DeviceTypeMetadata("display", "Display", "Display", "desc"),
                Map.of("ip", "127.0.0.1")
        );
        final var branch = facade.assembleBranch(new BranchEquipmentAssemblyRequest(
                List.of(new BranchImportRequest(
                        "branch-1",
                        "Main",
                        List.of(new BranchDeviceTypeImportRequest(
                                new EquipmentProfileDeviceTypeRequest(deviceTypeTemplate, true),
                                List.of(),
                                null, null, null, null, null, null, Map.of(), Map.of()
                        ))
                )),
                MergeStrategy.FAIL_IF_EXISTS
        ));

        assertThatThrownBy(() -> facade.exportDttSetFromBranch(new BranchEquipmentExportRequest(
                branch,
                List.of("missing-branch"),
                List.of("display"),
                MergeStrategy.FAIL_IF_EXISTS,
                null
        )))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Unknown branchIds");
    }


    @Test
    void shouldImportDttSetToProfileAndBranchWithMetadataInheritance() {
        final DttArchiveTemplate template = template();
        final byte[] bytes = facade.writeDtt(template);

        final var result = facade.importDttSetToProfileAndBranchWithMetadata(
                List.of(bytes),
                List.of("branch-1"),
                Map.of("display", new DeviceTypeMetadata("display", "Display Profile", "Display Profile", "profile-desc")),
                Map.of("branch-1", Map.of("display", new DeviceTypeMetadata("display", "Display Branch", "Display Branch", "branch-desc"))),
                MergeStrategy.FAIL_IF_EXISTS
        );

        assertThat(result.profile().deviceTypes().get("display").metadata().name()).isEqualTo("Display Profile");
        assertThat(result.branchEquipment().branches().get("branch-1").deviceTypes().get("display").template().metadata().name())
                .isEqualTo("Display Branch");
    }

    @Test
    void shouldSupportBase64AndZipFacadeModes() throws Exception {
        final DttArchiveTemplate template = template();
        final byte[] archiveBytes = facade.writeDtt(template);
        final String archiveBase64 = java.util.Base64.getEncoder().encodeToString(archiveBytes);

        final var profileFromBase64 = facade.importDttBase64SetToProfile(List.of(archiveBase64), MergeStrategy.FAIL_IF_EXISTS);
        assertThat(profileFromBase64.deviceTypes()).containsKey("display");

        final byte[] zipPayload = facade.exportProfileToDttZip(new ProfileExportRequest(profileFromBase64, List.of("display"), null));
        assertThat(countDttEntries(zipPayload)).isEqualTo(1);
        assertThat(firstDttEntryName(zipPayload)).isEqualTo("Display.dtt");

        final var profileFromZip = facade.importDttZipToProfile(zipPayload, MergeStrategy.FAIL_IF_EXISTS);
        assertThat(profileFromZip.deviceTypes()).containsKey("display");

        final String zipBase64 = facade.exportProfileToDttZipBase64(new ProfileExportRequest(profileFromBase64, List.of("display"), null));
        assertThat(zipBase64).isNotBlank();

        final String profileJson = facade.toProfileJson(profileFromBase64);
        final byte[] zipFromProfileJson = facade.exportProfileToDttZip(profileJson, List.of("display"), "2.0.0");
        assertThat(countDttEntries(zipFromProfileJson)).isEqualTo(1);

        final var branch = facade.importDttSetToBranch(List.of(archiveBytes), List.of("branch-1"), MergeStrategy.FAIL_IF_EXISTS);
        final String branchJson = facade.toBranchJson(branch);
        final byte[] zipFromBranchJson = facade.exportBranchToDttZip(
                branchJson,
                List.of("branch-1"),
                List.of("display"),
                MergeStrategy.FAIL_IF_EXISTS,
                "2.0.0"
        );
        assertThat(countDttEntries(zipFromBranchJson)).isEqualTo(1);
    }

    @Test
    void shouldReadAndResolveDttEntriesFromZipViaFacade() throws Exception {
        final byte[] displayDtt = facade.writeDtt(template());
        final byte[] cashboxDtt = facade.writeDtt(new DttArchiveTemplate(
                new DttArchiveDescriptor("DTT", "1.0", "cashbox", "1.0"),
                new DeviceTypeMetadata("cashbox", "Cashbox", "Cashbox", "desc"),
                Map.of(),
                Map.of(),
                Map.of("deviceTypeKind", "cashbox"),
                Map.of(),
                Map.of(),
                Map.of(),
                null,
                null,
                null,
                null,
                null,
                Map.of(),
                Map.of()
        ));
        final byte[] zipPayload = zipWithEntries(Map.of(
                "nested/Display.dtt", displayDtt,
                "Cashbox.dtt", cashboxDtt
        ));

        final Map<String, byte[]> archivesByEntryName = facade.readDttFilesFromZipByEntryName(zipPayload);
        assertThat(archivesByEntryName).containsKeys("nested/Display.dtt", "Cashbox.dtt");

        final byte[] resolvedByExact = facade.resolveDttArchiveEntry(archivesByEntryName, "nested/Display.dtt");
        final byte[] resolvedByNormalized = facade.resolveDttArchiveEntry(archivesByEntryName, "cashbox");
        assertThat(facade.readDtt(resolvedByExact).metadata().id()).isEqualTo("display");
        assertThat(facade.readDtt(resolvedByNormalized).metadata().id()).isEqualTo("cashbox");
    }

    @Test
    void shouldExtractMetadataAndResolveArchiveBaseNameViaFacade() throws Exception {
        final DttArchiveTemplate displayTemplate = template();
        final byte[] displayDtt = facade.writeDtt(displayTemplate);
        final byte[] cashboxDtt = facade.writeDtt(new DttArchiveTemplate(
                new DttArchiveDescriptor("DTT", "1.0", "cashbox", "2.0.0"),
                new DeviceTypeMetadata("cashbox", "Cashbox", "Cashbox", "desc"),
                Map.of(),
                Map.of(),
                Map.of("deviceTypeKind", "cashbox"),
                Map.of(),
                Map.of(),
                Map.of(),
                null,
                null,
                null,
                null,
                null,
                Map.of(),
                Map.of()
        ));
        final byte[] zipPayload = zipWithEntries(Map.of(
                "Display.dtt", displayDtt,
                "nested/Cashbox.dtt", cashboxDtt
        ));

        final List<DeviceTypeMetadata> metadataFromDtt = facade.extractDeviceTypeMetadataFromDttOrZip(displayDtt);
        final List<DeviceTypeMetadata> metadataFromZip = facade.extractDeviceTypeMetadataFromDttOrZip(zipPayload);
        final String archiveBaseName = facade.resolveDeviceTypeArchiveBaseName(displayDtt, "fallback-name");

        assertThat(metadataFromDtt).hasSize(1);
        assertThat(metadataFromDtt.get(0).id()).isEqualTo("display");
        assertThat(metadataFromZip).extracting(DeviceTypeMetadata::id).containsExactlyInAnyOrder("display", "cashbox");
        assertThat(metadataFromZip).extracting(DeviceTypeMetadata::version).contains("2.0.0");
        assertThat(archiveBaseName).isEqualTo("Display");
    }

    @Test
    void shouldPreviewSingleExportFromProfileAndBranchViaFacade() {
        final var typeTemplate = new ru.aritmos.dtt.api.dto.DeviceTypeTemplate(
                new DeviceTypeMetadata("display", "Display", "Display", "desc"),
                Map.of("ip", "127.0.0.1")
        );
        final EquipmentProfile profile = facade.assembleProfile(new EquipmentProfileAssemblyRequest(
                List.of(new EquipmentProfileDeviceTypeRequest(typeTemplate, true)),
                List.of(),
                MergeStrategy.FAIL_IF_EXISTS
        ));
        final BranchEquipment branch = facade.assembleBranch(new BranchEquipmentAssemblyRequest(
                List.of(new BranchImportRequest(
                        "branch-1",
                        "Main",
                        List.of(new BranchDeviceTypeImportRequest(
                                new EquipmentProfileDeviceTypeRequest(typeTemplate, true),
                                List.of(),
                                null, null, null, null, null, null, Map.of(), Map.of()
                        ))
                )),
                MergeStrategy.FAIL_IF_EXISTS
        ));

        final var profilePreview = facade.previewSingleDttExportFromProfile(profile, "display", "1.2.3");
        final var branchPreview = facade.previewSingleDttExportFromBranch(branch, List.of("branch-1"), "display", MergeStrategy.FAIL_IF_EXISTS, "1.2.3");
        final var failedPreview = facade.previewSingleDttExportFromProfile(profile, "missing", "1.2.3");

        assertThat(profilePreview.success()).isTrue();
        assertThat(profilePreview.archiveSizeBytes()).isGreaterThan(0);
        assertThat(branchPreview.success()).isTrue();
        assertThat(branchPreview.archiveSizeBytes()).isGreaterThan(0);
        assertThat(failedPreview.success()).isFalse();
        assertThat(failedPreview.issueCode()).isEqualTo("EXPORT_PREVIEW_ERROR");
    }

    @Test
    void shouldExportSingleDttFromProfileAndBranchViaFacade() {
        final var typeTemplate = new ru.aritmos.dtt.api.dto.DeviceTypeTemplate(
                new DeviceTypeMetadata("display", "Display", "Display", "desc"),
                Map.of("ip", "127.0.0.1")
        );
        final EquipmentProfile profile = facade.assembleProfile(new EquipmentProfileAssemblyRequest(
                List.of(new EquipmentProfileDeviceTypeRequest(typeTemplate, true)),
                List.of(),
                MergeStrategy.FAIL_IF_EXISTS
        ));
        final BranchEquipment branch = facade.assembleBranch(new BranchEquipmentAssemblyRequest(
                List.of(new BranchImportRequest(
                        "branch-1",
                        "Main",
                        List.of(new BranchDeviceTypeImportRequest(
                                new EquipmentProfileDeviceTypeRequest(typeTemplate, true),
                                List.of(),
                                null, null, null, null, null, null, Map.of(), Map.of()
                        ))
                )),
                MergeStrategy.FAIL_IF_EXISTS
        ));

        final byte[] profileArchive = facade.exportSingleDttFromProfile(profile, "display", "3.0.0");
        final byte[] branchArchive = facade.exportSingleDttFromBranch(branch, List.of("branch-1"), "display", MergeStrategy.FAIL_IF_EXISTS, "3.0.0");

        assertThat(facade.readDtt(profileArchive).metadata().id()).isEqualTo("display");
        assertThat(facade.readDtt(branchArchive).metadata().id()).isEqualTo("display");
        assertThat(facade.readDtt(branchArchive).descriptor().deviceTypeVersion()).isEqualTo("3.0.0");

        final String profileJson = facade.toProfileJson(profile);
        final String branchJson = facade.toBranchJson(branch);
        final byte[] profileArchiveFromJson = facade.exportSingleDttFromProfileJson(profileJson, "display", "3.0.0");
        final byte[] branchArchiveFromJson = facade.exportSingleDttFromBranchJson(
                branchJson,
                List.of("branch-1"),
                "display",
                MergeStrategy.FAIL_IF_EXISTS,
                "3.0.0"
        );
        final var profilePreviewFromJson = facade.previewSingleDttExportFromProfileJson(profileJson, "display", "3.0.0");
        final var branchPreviewFromJson = facade.previewSingleDttExportFromBranchJson(
                branchJson,
                List.of("branch-1"),
                "display",
                MergeStrategy.FAIL_IF_EXISTS,
                "3.0.0"
        );

        assertThat(facade.readDtt(profileArchiveFromJson).metadata().id()).isEqualTo("display");
        assertThat(facade.readDtt(branchArchiveFromJson).metadata().id()).isEqualTo("display");
        assertThat(profilePreviewFromJson.success()).isTrue();
        assertThat(branchPreviewFromJson.success()).isTrue();
    }


    @Test
    void shouldUseDeviceTypeNamesForDttFilesInsideZip() throws Exception {
        final var profile = new EquipmentProfile(Map.of(
                "display",
                new ru.aritmos.dtt.api.dto.DeviceTypeTemplate(
                        new DeviceTypeMetadata("display", "Display WD3264", "Display WD3264", "desc"),
                        Map.of("ip", "127.0.0.1")
                ),
                "terminal",
                new ru.aritmos.dtt.api.dto.DeviceTypeTemplate(
                        new DeviceTypeMetadata("terminal", "Терминал (Киоск)", "Терминал (Киоск)", "desc"),
                        Map.of("prefix", "SSS")
                )
        ));

        final byte[] zipPayload = facade.exportProfileToDttZip(new ProfileExportRequest(profile, List.of(), null));

        assertThat(zipEntryNames(zipPayload))
                .contains("Display WD3264.dtt")
                .contains("Терминал (Киоск).dtt");
    }

    @Test
    void shouldExportDttSetsAsBase64FromProfileAndBranch() {
        final var deviceTypeTemplate = new ru.aritmos.dtt.api.dto.DeviceTypeTemplate(
                new DeviceTypeMetadata("display", "Display", "Display", "desc"),
                Map.of("ip", "127.0.0.1")
        );
        final var profile = facade.assembleProfile(new EquipmentProfileAssemblyRequest(
                List.of(new EquipmentProfileDeviceTypeRequest(deviceTypeTemplate, true)),
                List.of(),
                MergeStrategy.FAIL_IF_EXISTS
        ));
        final Map<String, String> profileExport = facade.exportDttSetFromProfileBase64(
                new ProfileExportRequest(profile, List.of("display"), null)
        );

        assertThat(profileExport).containsKey("display");
        assertThat(java.util.Base64.getDecoder().decode(profileExport.get("display"))).isNotEmpty();

        final var branch = facade.assembleBranch(new BranchEquipmentAssemblyRequest(
                List.of(new BranchImportRequest(
                        "branch-1",
                        "Main",
                        List.of(new BranchDeviceTypeImportRequest(
                                new EquipmentProfileDeviceTypeRequest(deviceTypeTemplate, true),
                                List.of(),
                                null, null, null, null, null, null, Map.of(), Map.of()
                        ))
                )),
                MergeStrategy.FAIL_IF_EXISTS
        ));
        final Map<String, String> branchExport = facade.exportDttSetFromBranchBase64(
                new BranchEquipmentExportRequest(branch, List.of("branch-1"), List.of("display"), MergeStrategy.FAIL_IF_EXISTS, null)
        );

        assertThat(branchExport).containsKey("display");
        assertThat(java.util.Base64.getDecoder().decode(branchExport.get("display"))).isNotEmpty();
    }

    @Test
    void shouldImportDttSetIntoExistingBranchEquipmentWithMergeStrategy() {
        final var existingType = new ru.aritmos.dtt.api.dto.DeviceTypeTemplate(
                new DeviceTypeMetadata("display", "Display", "Display", "old-desc"),
                Map.of("ip", "10.0.0.10")
        );
        final var existing = facade.assembleBranch(new BranchEquipmentAssemblyRequest(
                List.of(new BranchImportRequest(
                        "branch-1",
                        "Main",
                        List.of(new BranchDeviceTypeImportRequest(
                                new EquipmentProfileDeviceTypeRequest(existingType, true),
                                List.of(),
                                "display_kind",
                                null,
                                null,
                                null,
                                null,
                                null,
                                Map.of("OLD_EVENT", new ru.aritmos.dtt.json.branch.BranchScript(Map.of(), List.of(), "println 'old'")),
                                Map.of()
                        ))
                )),
                MergeStrategy.FAIL_IF_EXISTS
        ));

        final DttArchiveTemplate importedTemplate = new DttArchiveTemplate(
                new DttArchiveDescriptor("DTT", "1.0", "display", "1.0.0"),
                new DeviceTypeMetadata("display", "Display", "Display", "new-desc"),
                Map.of(),
                Map.of(),
                Map.of("deviceTypeKind", "display_kind"),
                Map.of("ip", "10.0.0.20"),
                Map.of(),
                Map.of(),
                null,
                null,
                null,
                null,
                null,
                Map.of("NEW_EVENT", "println 'new'"),
                Map.of()
        );
        final byte[] archive = facade.writeDtt(importedTemplate);

        final BranchEquipment merged = facade.importDttSetToExistingBranch(
                List.of(archive),
                existing,
                List.of("branch-1"),
                MergeStrategy.MERGE_NON_NULLS
        );

        final var mergedType = merged.branches().get("branch-1").deviceTypes().get("display");
        assertThat(mergedType.template().deviceTypeParamValues()).containsEntry("ip", "10.0.0.20");
        assertThat(mergedType.eventHandlers()).containsKeys("OLD_EVENT", "NEW_EVENT");
    }

    @Test
    void shouldImportIntoExistingBranchFromJsonConvenienceMethods() throws Exception {
        final var existingType = new ru.aritmos.dtt.api.dto.DeviceTypeTemplate(
                new DeviceTypeMetadata("display", "Display", "Display", "old-desc"),
                Map.of("ip", "10.0.0.10")
        );
        final var existing = facade.assembleBranch(new BranchEquipmentAssemblyRequest(
                List.of(new BranchImportRequest(
                        "branch-1",
                        "Main",
                        List.of(new BranchDeviceTypeImportRequest(
                                new EquipmentProfileDeviceTypeRequest(existingType, true),
                                List.of(),
                                "display_kind",
                                null, null, null, null, null, Map.of(), Map.of()
                        ))
                )),
                MergeStrategy.FAIL_IF_EXISTS
        ));

        final DttArchiveTemplate importedTemplate = new DttArchiveTemplate(
                new DttArchiveDescriptor("DTT", "1.0", "display", "1.0.0"),
                new DeviceTypeMetadata("display", "Display", "Display", "new-desc"),
                Map.of(),
                Map.of(),
                Map.of("deviceTypeKind", "display_kind"),
                Map.of("ip", "10.0.0.20"),
                Map.of(),
                Map.of(),
                null, null, null, null, null,
                Map.of("NEW_EVENT", "println 'new'"),
                Map.of()
        );
        final byte[] archive = facade.writeDtt(importedTemplate);
        final String archiveBase64 = java.util.Base64.getEncoder().encodeToString(archive);
        final String existingJson = facade.toBranchJson(existing);
        final byte[] zipPayload = zipWithEntries(Map.of("display.dtt", archive));

        final BranchEquipment mergedFromBase64Json = facade.importDttBase64SetToExistingBranchJson(
                List.of(archiveBase64),
                existingJson,
                List.of("branch-1"),
                MergeStrategy.MERGE_NON_NULLS
        );
        final BranchEquipment mergedFromZipJson = facade.importDttZipToExistingBranchJson(
                zipPayload,
                existingJson,
                List.of("branch-1"),
                MergeStrategy.MERGE_NON_NULLS
        );

        assertThat(mergedFromBase64Json.branches().get("branch-1").deviceTypes().get("display").template().deviceTypeParamValues())
                .containsEntry("ip", "10.0.0.20");
        assertThat(mergedFromZipJson.branches().get("branch-1").deviceTypes().get("display").template().deviceTypeParamValues())
                .containsEntry("ip", "10.0.0.20");
    }

    @Test
    void shouldMergeBranchEquipmentViaFacadeMethod() {
        final var existingType = new ru.aritmos.dtt.api.dto.DeviceTypeTemplate(
                new DeviceTypeMetadata("display", "Display", "Display", "old-desc"),
                Map.of("ip", "10.0.0.10")
        );
        final var incomingType = new ru.aritmos.dtt.api.dto.DeviceTypeTemplate(
                new DeviceTypeMetadata("display", "Display", "Display", "new-desc"),
                Map.of("ip", "10.0.0.20")
        );
        final BranchEquipment existing = facade.assembleBranch(new BranchEquipmentAssemblyRequest(
                List.of(new BranchImportRequest(
                        "branch-1",
                        "Main",
                        List.of(new BranchDeviceTypeImportRequest(
                                new EquipmentProfileDeviceTypeRequest(existingType, true),
                                List.of(),
                                "display_kind",
                                null,
                                null,
                                null,
                                null,
                                null,
                                Map.of("OLD_EVENT", new ru.aritmos.dtt.json.branch.BranchScript(Map.of(), List.of(), "println 'old'")),
                                Map.of()
                        ))
                )),
                MergeStrategy.FAIL_IF_EXISTS
        ));
        final BranchEquipment incoming = facade.assembleBranch(new BranchEquipmentAssemblyRequest(
                List.of(new BranchImportRequest(
                        "branch-1",
                        "Main",
                        List.of(new BranchDeviceTypeImportRequest(
                                new EquipmentProfileDeviceTypeRequest(incomingType, true),
                                List.of(),
                                "display_kind",
                                null,
                                null,
                                null,
                                null,
                                null,
                                Map.of("NEW_EVENT", new ru.aritmos.dtt.json.branch.BranchScript(Map.of(), List.of(), "println 'new'")),
                                Map.of()
                        ))
                )),
                MergeStrategy.FAIL_IF_EXISTS
        ));

        final BranchEquipment merged = facade.mergeBranchEquipment(existing, incoming, MergeStrategy.MERGE_NON_NULLS);
        final var mergedType = merged.branches().get("branch-1").deviceTypes().get("display");

        assertThat(mergedType.template().deviceTypeParamValues()).containsEntry("ip", "10.0.0.20");
        assertThat(mergedType.eventHandlers()).containsKeys("OLD_EVENT", "NEW_EVENT");
    }

    @Test
    void shouldSupportPreviewModesInFacade() {
        final DttArchiveTemplate template = template();
        final byte[] archiveBytes = facade.writeDtt(template);
        final String archiveBase64 = java.util.Base64.getEncoder().encodeToString(archiveBytes);

        final var profileFromBytes = facade.previewDttSetToProfile(List.of(archiveBytes), MergeStrategy.FAIL_IF_EXISTS);
        assertThat(profileFromBytes.deviceTypes()).containsKey("display");

        final var profileFromBase64 = facade.previewDttBase64SetToProfile(List.of(archiveBase64), MergeStrategy.FAIL_IF_EXISTS);
        assertThat(profileFromBase64.deviceTypes()).containsKey("display");

        final byte[] zipPayload = facade.exportProfileToDttZip(new ProfileExportRequest(profileFromBytes, List.of("display"), null));
        final var profileFromZip = facade.previewDttZipToProfile(zipPayload, MergeStrategy.FAIL_IF_EXISTS);
        assertThat(profileFromZip.deviceTypes()).containsKey("display");

        final var branchFromBytes = facade.previewDttSetToBranch(List.of(archiveBytes), List.of("branch-1"), MergeStrategy.FAIL_IF_EXISTS);
        assertThat(branchFromBytes.branches()).containsKey("branch-1");

        final var branchFromBase64 = facade.previewDttBase64SetToBranch(
                List.of(archiveBase64),
                List.of("branch-1"),
                MergeStrategy.FAIL_IF_EXISTS
        );
        assertThat(branchFromBase64.branches()).containsKey("branch-1");

        final var branchFromZip = facade.previewDttZipToBranch(zipPayload, List.of("branch-1"), MergeStrategy.FAIL_IF_EXISTS);
        assertThat(branchFromZip.branches()).containsKey("branch-1");
    }

    @Test
    void shouldImportDttSetToBranchWithScriptsAndMetadata() {
        final DttArchiveTemplate source = new DttArchiveTemplate(
                new DttArchiveDescriptor("DTT", "1.0", "display", "1.0.0"),
                new DeviceTypeMetadata("display", "Display", "Display", "desc"),
                Map.of(),
                Map.of(),
                Map.of(
                        "deviceTypeKind", "display_kind",
                        "onStartEvent", Map.of("inputParameters", Map.of("a", "b"), "outputParameters", List.of("x")),
                        "eventHandlers", Map.of("EVENT", Map.of("inputParameters", Map.of("k", "v"), "outputParameters", List.of()))
                ),
                Map.of("ip", "127.0.0.1"),
                Map.of(),
                Map.of(),
                "println 'start'",
                null,
                null,
                null,
                "println 'functions'",
                Map.of("EVENT", "println 'event'"),
                Map.of("CMD", "println 'command'")
        );
        final byte[] archive = facade.writeDtt(source);

        final var branch = facade.importDttSetToBranch(List.of(archive), List.of("branch-1"), MergeStrategy.FAIL_IF_EXISTS);
        final var branchType = branch.branches().get("branch-1").deviceTypes().get("display");

        assertThat(branchType.onStartEvent()).isNotNull();
        assertThat(branchType.onStartEvent().scriptCode()).isEqualTo("println 'start'");
        assertThat(branchType.onStartEvent().inputParameters()).containsEntry("a", "b");
        assertThat(branchType.eventHandlers()).containsKey("EVENT");
        assertThat(branchType.eventHandlers().get("EVENT").inputParameters()).containsEntry("k", "v");
        assertThat(branchType.commands()).containsKey("CMD");
        assertThat(branchType.deviceTypeFunctions()).isEqualTo("println 'functions'");
        assertThat(branchType.kind()).isEqualTo("display_kind");
    }

    @Test
    void shouldFailWhenPreviewArchivesCollectionIsEmpty() {
        assertThatThrownBy(() -> facade.previewDttSetToProfile(List.of(), MergeStrategy.FAIL_IF_EXISTS))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("archives must contain at least one DTT archive");

        assertThatThrownBy(() -> facade.previewDttSetToBranch(List.of(), List.of("branch-1"), MergeStrategy.FAIL_IF_EXISTS))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("archives must contain at least one DTT archive");
    }

    @Test
    void shouldExportDttWithRequestedVersionAndDefaultValuesFromProfileJson() {
        final var sourceTemplate = new ru.aritmos.dtt.api.dto.DeviceTypeTemplate(
                new DeviceTypeMetadata("display", "Display", "Display", "desc"),
                Map.of("ip", "127.0.0.1", "port", 8080)
        );
        final var profile = facade.assembleProfile(new EquipmentProfileAssemblyRequest(
                List.of(new EquipmentProfileDeviceTypeRequest(sourceTemplate, true)),
                List.of(),
                MergeStrategy.FAIL_IF_EXISTS
        ));

        final var exported = facade.exportDttSetFromProfile(new ProfileExportRequest(profile, List.of("display"), "2.5.0"));
        final byte[] archiveBytes = exported.archivesByDeviceTypeId().get("display");
        final DttArchiveTemplate restored = facade.readDtt(archiveBytes);

        assertThat(restored.descriptor().formatVersion()).isEqualTo("1.0");
        assertThat(restored.descriptor().deviceTypeVersion()).isEqualTo("2.5.0");
        assertThat(restored.defaultValues()).containsEntry("ip", "127.0.0.1").containsEntry("port", 8080);
        assertThat(restored.metadata().description()).endsWith("2.5.0");
    }

    @Test
    void shouldUseGreaterVersionBetweenRequestedAndTemplateMetadataVersion() {
        final var sourceTemplate = new ru.aritmos.dtt.api.dto.DeviceTypeTemplate(
                new DeviceTypeMetadata("display", "Display", "Display", "desc", "4.2.0", null),
                Map.of("ip", "127.0.0.1")
        );
        final var profile = facade.assembleProfile(new EquipmentProfileAssemblyRequest(
                List.of(new EquipmentProfileDeviceTypeRequest(sourceTemplate, true)),
                List.of(),
                MergeStrategy.FAIL_IF_EXISTS
        ));

        final var exported = facade.exportDttSetFromProfile(new ProfileExportRequest(profile, List.of("display"), "3.1.0"));
        final DttArchiveTemplate restored = facade.readDtt(exported.archivesByDeviceTypeId().get("display"));

        assertThat(restored.descriptor().deviceTypeVersion()).isEqualTo("4.2.0");
        assertThat(restored.metadata().version()).isEqualTo("4.2.0");
    }

    @Test
    void shouldExportDttFromProfileJsonStringWithRequestedVersion() {
        final String profileJson = """
                {
                  "display": {
                    "id": "display",
                    "name": "Display",
                    "displayName": "Display",
                    "description": "desc",
                    "deviceTypeParamValues": {
                      "ip": "10.0.0.1"
                    }
                  }
                }
                """;

        final var exported = facade.exportDttSetFromProfileJson(profileJson, List.of("display"), "3.0.1");
        final DttArchiveTemplate restored = facade.readDtt(exported.archivesByDeviceTypeId().get("display"));

        assertThat(restored.descriptor().deviceTypeVersion()).isEqualTo("3.0.1");
        assertThat(restored.defaultValues()).containsEntry("ip", "10.0.0.1");
        assertThat(restored.metadata().description()).endsWith("3.0.1");
    }

    @Test
    void shouldPreserveParameterMetadataInExportedDeviceTypeSchema() {
        final String profileJson = """
                {
                  "display": {
                    "id": "display",
                    "name": "Display",
                    "displayName": "Display",
                    "description": "desc",
                    "deviceTypeParamValues": {
                      "ip": {
                        "value": "10.0.0.1",
                        "name": "ip",
                        "displayName": "IP address",
                        "type": "String",
                        "description": "Printer host",
                        "exampleValue": "192.168.1.100"
                      },
	                      "zones": {
	                        "value": {
	                          "main": {
	                            "value": 1,
	                            "name": "main",
	                            "type": "Number"
	                          }
	                        },
	                        "name": "zones",
	                        "type": "Object"
	                      },
	                      "sensors": {
	                        "value": [
	                          {
	                            "value": "A1",
	                            "name": "channelId",
	                            "type": "String",
	                            "exampleValue": "CH-1"
	                          }
	                        ],
	                        "name": "sensors",
	                        "type": "Array"
	                      }
	                    }
	                  }
	                }
	                """;

        final var exported = facade.exportDttSetFromProfileJson(profileJson, List.of("display"), "3.1.0");
        final DttArchiveTemplate restored = facade.readDtt(exported.archivesByDeviceTypeId().get("display"));

	        assertThat(restored.deviceTypeParametersSchema()).containsKeys("ip", "zones", "sensors");
	        final Map<String, Object> ipSchema = castToMap(restored.deviceTypeParametersSchema().get("ip"));
	        assertThat(ipSchema).containsEntry("displayName", "IP address");
	        assertThat(ipSchema).containsEntry("type", "String");
	        assertThat(ipSchema).containsEntry("description", "Printer host");
	        final Map<String, Object> zonesSchema = castToMap(restored.deviceTypeParametersSchema().get("zones"));
	        assertThat(zonesSchema).containsEntry("type", "Object");
	        assertThat(zonesSchema).containsKey("parametersMap");
	        final Map<String, Object> sensorsSchema = castToMap(restored.deviceTypeParametersSchema().get("sensors"));
	        assertThat(sensorsSchema).containsEntry("type", "Array");
	        final Map<String, Object> sensorsItems = castToMap(sensorsSchema.get("items"));
	        assertThat(sensorsItems).containsEntry("type", "String");
	        assertThat(sensorsItems).containsEntry("exampleValue", "CH-1");
	        assertThat(restored.exampleValues()).containsEntry("ip", "192.168.1.100");
	        final Map<String, Object> zonesExamples = castToMap(restored.exampleValues().get("zones"));
	        assertThat(zonesExamples).containsEntry("main", 1);
	        assertThat(restored.exampleValues()).containsEntry("sensors", List.of("CH-1"));
            assertThat(restored.templateOrigin())
                    .containsEntry("sourceKind", "PROFILE_JSON")
                    .containsEntry("sourceSummary", "export profile->dtt");
	    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> castToMap(Object value) {
        return (Map<String, Object>) value;
    }

    @Test
    void shouldExportDttFromBranchJsonStringWithRequestedVersion() {
        final String branchJson = """
                {
                  "branch-1": {
                    "id": "branch-1",
                    "displayName": "Main",
                    "deviceTypes": {
                      "display": {
                        "id": "display",
                        "name": "Display",
                        "displayName": "Display",
                        "description": "desc",
                        "type": "display_kind",
                        "deviceTypeParamValues": {
                          "ip": "10.0.0.2"
                        },
                        "onStartEvent": {
                          "inputParameters": {},
                          "outputParameters": [],
                          "scriptCode": "println 'start'"
                        },
                        "onStopEvent": null,
                        "onPublicStartEvent": null,
                        "onPublicFinishEvent": {
                          "inputParameters": {},
                          "outputParameters": [],
                          "scriptCode": "println 'finish'"
                        },
                        "eventHandlers": {
                          "EVENT": {
                            "inputParameters": {},
                            "outputParameters": [],
                            "scriptCode": "println 'event'"
                          }
                        },
                        "commands": {
                          "CMD": {
                            "inputParameters": {},
                            "outputParameters": [],
                            "scriptCode": "println 'command'"
                          }
                        },
                        "deviceTypeFunctions": "println 'functions'",
                        "devices": {}
                      }
                    }
                  }
                }
                """;

        final var exported = facade.exportDttSetFromBranchJson(
                branchJson,
                List.of("branch-1"),
                List.of("display"),
                MergeStrategy.FAIL_IF_EXISTS,
                "3.0.2"
        );
        final DttArchiveTemplate restored = facade.readDtt(exported.archivesByDeviceTypeId().get("display"));

        assertThat(restored.descriptor().deviceTypeVersion()).isEqualTo("3.0.2");
        assertThat(restored.defaultValues()).containsEntry("ip", "10.0.0.2");
        assertThat(restored.metadata().description()).endsWith("3.0.2");
        assertThat(restored.onStartEvent()).isEqualTo("println 'start'");
        assertThat(restored.onPublicFinishEvent()).isEqualTo("println 'finish'");
        assertThat(restored.deviceTypeFunctions()).isEqualTo("println 'functions'");
        assertThat(restored.eventHandlers()).containsEntry("EVENT", "println 'event'");
        assertThat(restored.commands()).containsEntry("CMD", "println 'command'");
        assertThat(restored.bindingHints()).containsKeys("deviceTypeKind", "onStartEvent", "onPublicFinishEvent", "eventHandlers", "commands");
        assertThat(restored.bindingHints()).containsEntry("deviceTypeKind", "display_kind");
        final Map<String, Object> eventHandlersHint = (Map<String, Object>) restored.bindingHints().get("eventHandlers");
        assertThat(eventHandlersHint).containsKey("EVENT");
        final Map<String, Object> eventHint = (Map<String, Object>) eventHandlersHint.get("EVENT");
        assertThat(eventHint).containsKeys("inputParameters", "outputParameters");
    }

    @Test
    void shouldExtractDefaultValuesFromDeviceManagerStyleParameterObjects() {
        final var sourceTemplate = new ru.aritmos.dtt.api.dto.DeviceTypeTemplate(
                new DeviceTypeMetadata("display", "Display", "Display", "desc"),
                Map.of(
                        "prefix", Map.of(
                                "value", "SSS",
                                "name", "prefix",
                                "displayName", "entrypoint_prefix",
                                "type", "String"
                        ),
                        "zones", Map.of(
                                "value", Map.of(
                                        "0", Map.of(
                                                "enabled", Map.of("value", true),
                                                "text", Map.of("value", "CLOSED")
                                        )
                                ),
                                "name", "zones",
                                "type", "Object"
                        )
                )
        );
        final var profile = facade.assembleProfile(new EquipmentProfileAssemblyRequest(
                List.of(new EquipmentProfileDeviceTypeRequest(sourceTemplate, true)),
                List.of(),
                MergeStrategy.FAIL_IF_EXISTS
        ));

        final var exported = facade.exportDttSetFromProfile(new ProfileExportRequest(profile, List.of("display"), "4.0.0"));
        final DttArchiveTemplate restored = facade.readDtt(exported.archivesByDeviceTypeId().get("display"));

        assertThat(restored.defaultValues()).containsEntry("prefix", "SSS");
        assertThat(restored.defaultValues()).containsKey("zones");
        final Map<String, Object> zones = (Map<String, Object>) restored.defaultValues().get("zones");
        assertThat(zones).containsKey("0");
        final Map<String, Object> zoneZero = (Map<String, Object>) zones.get("0");
        assertThat(zoneZero)
                .containsEntry("enabled", true)
                .containsEntry("text", "CLOSED");
    }

    @Test
    void shouldRestoreSchemaMetadataWhenImportingDttToProfile() {
        final DttArchiveTemplate template = new DttArchiveTemplate(
                new DttArchiveDescriptor("DTT", "1.0", "display", "1.0.0"),
                new DeviceTypeMetadata("display", "Display", "Display", "desc"),
                Map.of(
                        "ip", Map.of(
                                "name", "ip",
                                "type", "String",
                                "displayName", "IP Address",
                                "description", "Terminal IP"
                        ),
                        "zones", Map.of(
                                "name", "zones",
                                "type", "Object",
                                "parametersMap", Map.of(
                                        "main", Map.of(
                                                "name", "main",
                                                "type", "Number",
                                                "description", "Main zone"
                                        )
                                )
                        )
                ),
                Map.of(),
                Map.of(),
                Map.of("ip", "10.0.0.1", "zones", Map.of("main", 1)),
                Map.of("ip", "192.168.0.1", "zones", Map.of("main", 7)),
                Map.of("sourceKind", "PROFILE_JSON"),
                null,
                null,
                null,
                null,
                null,
                Map.of(),
                Map.of()
        );
        final byte[] archive = facade.writeDtt(template);

        final EquipmentProfile profile = facade.importDttSetToProfile(List.of(archive), MergeStrategy.FAIL_IF_EXISTS);
        final Object ipValue = profile.deviceTypes().get("display").deviceTypeParamValues().get("ip");
        final Map<String, Object> ip = castToMap(ipValue);
        assertThat(ip).containsEntry("displayName", "IP Address");
        assertThat(ip).containsEntry("description", "Terminal IP");
        assertThat(ip).containsEntry("value", "10.0.0.1");
        assertThat(ip).containsEntry("exampleValue", "192.168.0.1");

        final Map<String, Object> zones = castToMap(profile.deviceTypes().get("display").deviceTypeParamValues().get("zones"));
        final Map<String, Object> zonesValue = castToMap(zones.get("value"));
        final Map<String, Object> main = castToMap(zonesValue.get("main"));
        assertThat(main).containsEntry("description", "Main zone");
        assertThat(main).containsEntry("value", 1);
        assertThat(main).containsEntry("exampleValue", 7);
    }

    @Test
    void shouldRestoreArrayItemSchemaMetadataWhenImportingDttToProfile() {
        final DttArchiveTemplate template = new DttArchiveTemplate(
                new DttArchiveDescriptor("DTT", "1.0", "display", "1.0.0"),
                new DeviceTypeMetadata("display", "Display", "Display", "desc"),
                Map.of(
                        "sensors", Map.of(
                                "name", "sensors",
                                "type", "Array",
                                "displayName", "Sensors",
                                "items", Map.of(
                                        "name", "sensor",
                                        "type", "Object",
                                        "parametersMap", Map.of(
                                                "id", Map.of(
                                                        "name", "id",
                                                        "type", "String",
                                                        "description", "Sensor id"
                                                )
                                        )
                                )
                        )
                ),
                Map.of(),
                Map.of(),
                Map.of("sensors", List.of(Map.of("id", "A1"))),
                Map.of("sensors", List.of(Map.of("id", "B1"))),
                Map.of("sourceKind", "PROFILE_JSON"),
                null,
                null,
                null,
                null,
                null,
                Map.of(),
                Map.of()
        );
        final byte[] archive = facade.writeDtt(template);

        final EquipmentProfile profile = facade.importDttSetToProfile(List.of(archive), MergeStrategy.FAIL_IF_EXISTS);
        final Map<String, Object> sensors = castToMap(profile.deviceTypes().get("display").deviceTypeParamValues().get("sensors"));
        assertThat(sensors).containsEntry("displayName", "Sensors");
        final List<?> restoredValues = (List<?>) sensors.get("value");
        final Map<String, Object> firstItem = castToMap(restoredValues.get(0));
        final Map<String, Object> itemValue = castToMap(firstItem.get("value"));
        final Map<String, Object> id = castToMap(itemValue.get("id"));
        assertThat(id).containsEntry("description", "Sensor id");
        assertThat(id).containsEntry("value", "A1");
        assertThat(id).containsEntry("exampleValue", "B1");
    }

    @Test
    void shouldRestoreArrayItemSchemaMetadataWhenImportingDttToBranch() {
        final DttArchiveTemplate template = new DttArchiveTemplate(
                new DttArchiveDescriptor("DTT", "1.0", "display", "1.0.0"),
                new DeviceTypeMetadata("display", "Display", "Display", "desc"),
                Map.of(
                        "sensors", Map.of(
                                "name", "sensors",
                                "type", "Array",
                                "displayName", "Sensors",
                                "items", Map.of(
                                        "name", "sensor",
                                        "type", "Object",
                                        "parametersMap", Map.of(
                                                "id", Map.of(
                                                        "name", "id",
                                                        "type", "String",
                                                        "description", "Sensor id"
                                                )
                                        )
                                )
                        )
                ),
                Map.of(),
                Map.of("deviceTypeKind", "display_kind"),
                Map.of("sensors", List.of(Map.of("id", "A1"))),
                Map.of("sensors", List.of(Map.of("id", "B1"))),
                Map.of("sourceKind", "BRANCH_EQUIPMENT_JSON"),
                null,
                null,
                null,
                null,
                null,
                Map.of(),
                Map.of()
        );
        final byte[] archive = facade.writeDtt(template);

        final BranchEquipment branch = facade.importDttSetToBranch(
                List.of(archive),
                List.of("branch-1"),
                MergeStrategy.FAIL_IF_EXISTS
        );
        final Map<String, Object> sensors = castToMap(
                branch.branches().get("branch-1").deviceTypes().get("display").template().deviceTypeParamValues().get("sensors")
        );
        assertThat(sensors).containsEntry("displayName", "Sensors");
        final List<?> restoredValues = (List<?>) sensors.get("value");
        final Map<String, Object> firstItem = castToMap(restoredValues.get(0));
        final Map<String, Object> itemValue = castToMap(firstItem.get("value"));
        final Map<String, Object> id = castToMap(itemValue.get("id"));
        assertThat(id).containsEntry("description", "Sensor id");
        assertThat(id).containsEntry("value", "A1");
        assertThat(id).containsEntry("exampleValue", "B1");
    }

    @Test
    void shouldKeepExplicitArrayItemsSchemaAfterDttToProfileToDttRoundTrip() {
        final DttArchiveTemplate source = new DttArchiveTemplate(
                new DttArchiveDescriptor("DTT", "1.0", "display", "1.0.0"),
                new DeviceTypeMetadata("display", "Display", "Display", "desc"),
                Map.of(
                        "sensors", Map.of(
                                "name", "sensors",
                                "type", "Array",
                                "items", Map.of(
                                        "name", "sensor",
                                        "type", "Object",
                                        "parametersMap", Map.of(
                                                "id", Map.of(
                                                        "name", "id",
                                                        "type", "String",
                                                        "description", "Sensor id from schema"
                                                )
                                        )
                                )
                        )
                ),
                Map.of(),
                Map.of(),
                Map.of("sensors", List.of(Map.of("id", "A1"))),
                Map.of("sensors", List.of(Map.of("id", "B1"))),
                Map.of("sourceKind", "PROFILE_JSON"),
                null,
                null,
                null,
                null,
                null,
                Map.of(),
                Map.of()
        );
        final byte[] archive = facade.writeDtt(source);
        final EquipmentProfile profile = facade.importDttSetToProfile(List.of(archive), MergeStrategy.FAIL_IF_EXISTS);

        final BatchDttExportResult exported = facade.exportDttSetFromProfile(
                new ProfileExportRequest(profile, List.of("display"), "1.0.1")
        );
        final DttArchiveTemplate restored = facade.readDtt(exported.archivesByDeviceTypeId().get("display"));

        final Map<String, Object> sensorsSchema = castToMap(restored.deviceTypeParametersSchema().get("sensors"));
        final Map<String, Object> items = castToMap(sensorsSchema.get("items"));
        final Map<String, Object> nested = castToMap(items.get("parametersMap"));
        final Map<String, Object> id = castToMap(nested.get("id"));
        assertThat(id).containsEntry("description", "Sensor id from schema");
    }

    @Test
    void shouldKeepExtendedMetadataInProfileRoundTrip() {
        final String profileJson = """
                {
                  "display": {
                    "id": "display",
                    "name": "Display",
                    "displayName": "Display",
                    "description": "desc",
                    "deviceTypeParamValues": {
                      "ip": {
                        "value": "10.0.0.1",
                        "name": "ip",
                        "displayName": "IP address",
                        "type": "String",
                        "description": "Terminal IP",
                        "exampleValue": "192.168.0.1"
                      },
                      "sensors": {
                        "value": [
                          {
                            "id": {
                              "value": "A1",
                              "name": "id",
                              "type": "String",
                              "description": "Sensor id",
                              "exampleValue": "B1"
                            }
                          }
                        ],
                        "name": "sensors",
                        "type": "Array",
                        "items": {
                          "name": "sensor",
                          "type": "Object",
                          "parametersMap": {
                            "id": {
                              "name": "id",
                              "type": "String",
                              "description": "Sensor id"
                            }
                          }
                        }
                      }
                    }
                  }
                }
                """;

        final BatchDttExportResult exported = facade.exportDttSetFromProfileJson(profileJson, List.of("display"), "2.0.0");
        final EquipmentProfile importedProfile = facade.importDttSetToProfile(
                List.of(exported.archivesByDeviceTypeId().get("display")),
                MergeStrategy.FAIL_IF_EXISTS
        );
        final BatchDttExportResult exportedAgain = facade.exportDttSetFromProfile(
                new ProfileExportRequest(importedProfile, List.of("display"), "2.0.1")
        );
        final DttArchiveTemplate restored = facade.readDtt(exportedAgain.archivesByDeviceTypeId().get("display"));
        final Map<String, Object> sensors = castToMap(restored.deviceTypeParametersSchema().get("sensors"));
        final Map<String, Object> items = castToMap(sensors.get("items"));
        final Map<String, Object> nested = castToMap(items.get("parametersMap"));
        final Map<String, Object> id = castToMap(nested.get("id"));
        assertThat(id).containsEntry("description", "Sensor id");
        assertThat(restored.exampleValues()).containsEntry("ip", "192.168.0.1");
    }

    @Test
    void shouldKeepExtendedMetadataInBranchRoundTrip() {
        final String branchJson = """
                {
                  "branch-1": {
                    "id": "branch-1",
                    "displayName": "Main",
                    "deviceTypes": {
                      "display": {
                        "id": "display",
                        "name": "Display",
                        "displayName": "Display",
                        "description": "desc",
                        "type": "display_kind",
                        "deviceTypeParamValues": {
                          "ip": {
                            "value": "10.0.0.2",
                            "name": "ip",
                            "displayName": "IP address",
                            "type": "String",
                            "description": "Terminal IP",
                            "exampleValue": "192.168.0.2"
                          }
                        },
                        "onStartEvent": { "scriptCode": "println 'start'" },
                        "eventHandlers": {
                          "E1": { "scriptCode": "println 'event'" }
                        }
                      }
                    }
                  }
                }
                """;
        final BatchDttExportResult exported = facade.exportDttSetFromBranchJson(
                branchJson,
                List.of("branch-1"),
                List.of("display"),
                MergeStrategy.FAIL_IF_EXISTS,
                "2.0.0"
        );
        final BranchEquipment importedBranch = facade.importDttSetToBranch(
                List.of(exported.archivesByDeviceTypeId().get("display")),
                List.of("branch-1"),
                MergeStrategy.FAIL_IF_EXISTS
        );
        final BatchDttExportResult exportedAgain = facade.exportDttSetFromBranch(
                new BranchEquipmentExportRequest(importedBranch, List.of("branch-1"), List.of("display"), MergeStrategy.FAIL_IF_EXISTS, "2.0.1")
        );
        final DttArchiveTemplate restored = facade.readDtt(exportedAgain.archivesByDeviceTypeId().get("display"));
        final Map<String, Object> ip = castToMap(restored.deviceTypeParametersSchema().get("ip"));
        assertThat(ip).containsEntry("displayName", "IP address");
        assertThat(ip).containsEntry("description", "Terminal IP");
        assertThat(restored.bindingHints()).containsEntry("deviceTypeKind", "display_kind");
        assertThat(restored.eventHandlers()).containsKey("E1");
    }

    @Test
    void shouldExportUniqueDttSetFromDeviceManagerAndRebuildOriginalBranchJson() {
        final String sourceBranchJson = """
                {
                  "branch-1": {
                    "id": "branch-1",
                    "displayName": "Main",
                    "deviceTypes": {
                      "display": {
                        "id": "display",
                        "name": "Display",
                        "displayName": "Display",
                        "description": "Display device type",
                        "type": "display_kind",
                        "deviceTypeParamValues": {
                          "ip": { "value": "10.10.0.1", "name": "ip", "type": "String" }
                        },
                        "onStartEvent": { "scriptCode": "println 'start display'" },
                        "onStopEvent": { "scriptCode": "" },
                        "onPublicStartEvent": { "scriptCode": "" },
                        "onPublicFinishEvent": { "scriptCode": "" },
                        "deviceTypeFunctions": ""
                      }
                    }
                  },
                  "branch-2": {
                    "id": "branch-2",
                    "displayName": "Backup",
                    "deviceTypes": {
                      "display": {
                        "id": "display",
                        "name": "Display",
                        "displayName": "Display",
                        "description": "Display device type",
                        "type": "display_kind",
                        "deviceTypeParamValues": {
                          "ip": { "value": "10.10.0.1", "name": "ip", "type": "String" }
                        },
                        "onStartEvent": { "scriptCode": "println 'start display'" },
                        "onStopEvent": { "scriptCode": "" },
                        "onPublicStartEvent": { "scriptCode": "" },
                        "onPublicFinishEvent": { "scriptCode": "" },
                        "deviceTypeFunctions": ""
                      }
                    }
                  }
                }
                """;
        final BatchDttExportResult exported = facade.exportDttSetFromBranchJson(
                sourceBranchJson,
                List.of("branch-1", "branch-2"),
                null,
                MergeStrategy.MERGE_NON_NULLS,
                null
        );

        assertThat(exported.archivesByDeviceTypeId()).containsOnlyKeys("display");

        final BranchEquipment rebuilt = facade.importDttSetToExistingBranch(
                List.copyOf(exported.archivesByDeviceTypeId().values()),
                facade.parseBranchJson("""
                        {
                          "branch-1": { "id": "branch-1", "displayName": "Main", "deviceTypes": {} },
                          "branch-2": { "id": "branch-2", "displayName": "Backup", "deviceTypes": {} }
                        }
                        """),
                List.of("branch-1", "branch-2"),
                MergeStrategy.FAIL_IF_EXISTS
        );

        assertThat(rebuilt.branches()).containsKeys("branch-1", "branch-2");
        assertThat(rebuilt.branches().get("branch-1").deviceTypes()).containsOnlyKeys("display");
        assertThat(rebuilt.branches().get("branch-2").deviceTypes()).containsOnlyKeys("display");

        final var firstType = rebuilt.branches().get("branch-1").deviceTypes().get("display");
        final var secondType = rebuilt.branches().get("branch-2").deviceTypes().get("display");
        assertThat(firstType.template().metadata().id()).isEqualTo("display");
        assertThat(secondType.template().metadata().id()).isEqualTo("display");
        assertThat(castToMap(firstType.template().deviceTypeParamValues().get("ip")))
                .containsEntry("name", "ip")
                .containsEntry("type", "String")
                .containsEntry("value", "10.10.0.1");
        assertThat(castToMap(secondType.template().deviceTypeParamValues().get("ip")))
                .containsEntry("name", "ip")
                .containsEntry("type", "String")
                .containsEntry("value", "10.10.0.1");
        assertThat(firstType.onStartEvent().scriptCode()).isEqualTo("println 'start display'");
        assertThat(secondType.onStartEvent().scriptCode()).isEqualTo("println 'start display'");
    }

    @Test
    void shouldPreferMostFilledDeviceTypeWhenSameTypeExistsInDifferentBranchesAndMergeStrategyIsNotSpecified() {
        final String sourceBranchJson = """
                {
                  "branch-1": {
                    "id": "branch-1",
                    "displayName": "Main",
                    "deviceTypes": {
                      "display": {
                        "id": "display",
                        "name": "Display",
                        "displayName": "Display",
                        "description": "Less filled",
                        "type": "display_kind",
                        "deviceTypeParamValues": {
                          "ip": { "value": "10.10.0.1", "name": "ip", "type": "String" }
                        }
                      }
                    }
                  },
                  "branch-2": {
                    "id": "branch-2",
                    "displayName": "Backup",
                    "deviceTypes": {
                      "display": {
                        "id": "display",
                        "name": "Display",
                        "displayName": "Display terminal",
                        "description": "More filled",
                        "type": "display_kind",
                        "deviceTypeParamValues": {
                          "ip": {
                            "value": "10.10.0.2",
                            "name": "ip",
                            "displayName": "IP address",
                            "type": "String",
                            "description": "Terminal IP",
                            "exampleValue": "192.168.0.2"
                          }
                        },
                        "onStartEvent": { "scriptCode": "println 'start display'" },
                        "eventHandlers": {
                          "E1": { "scriptCode": "println 'event'" }
                        },
                        "commands": {
                          "C1": { "scriptCode": "println 'command'" }
                        },
                        "deviceTypeFunctions": "println 'fn'"
                      }
                    }
                  }
                }
                """;

        final BatchDttExportResult exported = facade.exportDttSetFromBranchJson(
                sourceBranchJson,
                List.of("branch-1", "branch-2"),
                null,
                null,
                "2.0.0"
        );

        assertThat(exported.archivesByDeviceTypeId()).containsOnlyKeys("display");
        final DttArchiveTemplate restored = facade.readDtt(exported.archivesByDeviceTypeId().get("display"));
        final Map<String, Object> ip = castToMap(restored.deviceTypeParametersSchema().get("ip"));
        assertThat(restored.metadata().description()).contains("More filled");
        assertThat(ip).containsEntry("displayName", "IP address");
        assertThat(ip).containsEntry("description", "Terminal IP");
        assertThat(restored.eventHandlers()).containsKey("E1");
        assertThat(restored.commands()).containsKey("C1");
        assertThat(restored.deviceTypeFunctions()).isEqualTo("println 'fn'");
        assertThat(castToMap(restored.templateOrigin().get("branchTopologyByBranchId")))
                .containsKeys("branch-1", "branch-2");
    }

    @Test
    void shouldKeepFirstDeviceTypeWhenSameTypeCompletenessIsEqualAndMergeStrategyIsNotSpecified() {
        final String sourceBranchJson = """
                {
                  "branch-1": {
                    "id": "branch-1",
                    "displayName": "Main",
                    "deviceTypes": {
                      "display": {
                        "id": "display",
                        "name": "Display",
                        "displayName": "Display A",
                        "description": "First branch version",
                        "type": "display_kind",
                        "deviceTypeParamValues": {
                          "ip": {
                            "value": "10.10.0.1",
                            "name": "ip",
                            "displayName": "IP A",
                            "type": "String"
                          }
                        }
                      }
                    }
                  },
                  "branch-2": {
                    "id": "branch-2",
                    "displayName": "Backup",
                    "deviceTypes": {
                      "display": {
                        "id": "display",
                        "name": "Display",
                        "displayName": "Display B",
                        "description": "Second branch version",
                        "type": "display_kind",
                        "deviceTypeParamValues": {
                          "ip": {
                            "value": "10.10.0.2",
                            "name": "ip",
                            "displayName": "IP B",
                            "type": "String"
                          }
                        }
                      }
                    }
                  }
                }
                """;

        final BatchDttExportResult exported = facade.exportDttSetFromBranchJson(
                sourceBranchJson,
                List.of("branch-1", "branch-2"),
                null,
                null,
                "2.0.0"
        );

        final DttArchiveTemplate restored = facade.readDtt(exported.archivesByDeviceTypeId().get("display"));
        final Map<String, Object> ip = castToMap(restored.deviceTypeParametersSchema().get("ip"));
        assertThat(restored.metadata().description()).contains("First branch version");
        assertThat(ip).containsEntry("displayName", "IP A");
    }

    private byte[] zipWithEntries(Map<String, byte[]> entries) throws IOException {
        try (ByteArrayOutputStream output = new ByteArrayOutputStream();
             ZipOutputStream zipOutput = new ZipOutputStream(output)) {
            for (Map.Entry<String, byte[]> entry : entries.entrySet()) {
                zipOutput.putNextEntry(new ZipEntry(entry.getKey()));
                zipOutput.write(entry.getValue());
                zipOutput.closeEntry();
            }
            zipOutput.finish();
            return output.toByteArray();
        }
    }

    private DttArchiveTemplate template() {
        return new DttArchiveTemplate(
                new DttArchiveDescriptor("DTT", "1.0", "display", null),
                new DeviceTypeMetadata("display", "Display", "Display", "desc"),
                Map.of(),
                Map.of(),
                Map.of(),
                Map.of(),
                Map.of(),
                Map.of(),
                "println 'ok'",
                null,
                null,
                null,
                null,
                Map.of(),
                Map.of()
        );
    }

    private int countDttEntries(byte[] zipPayload) throws Exception {
        try (ZipInputStream input = new ZipInputStream(new java.io.ByteArrayInputStream(zipPayload))) {
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
        try (ZipInputStream input = new ZipInputStream(new ByteArrayInputStream(zipBytes))) {
            ZipEntry entry;
            while ((entry = input.getNextEntry()) != null) {
                if (!entry.isDirectory() && entry.getName().endsWith(".dtt")) {
                    return entry.getName();
                }
            }
            throw new IllegalStateException("zip payload does not contain .dtt entry");
        }
    }

    private List<String> zipEntryNames(byte[] zipBytes) throws IOException {
        try (ZipInputStream input = new ZipInputStream(new ByteArrayInputStream(zipBytes))) {
            final java.util.ArrayList<String> names = new java.util.ArrayList<>();
            ZipEntry entry;
            while ((entry = input.getNextEntry()) != null) {
                if (!entry.isDirectory() && entry.getName().endsWith(".dtt")) {
                    names.add(entry.getName());
                }
            }
            return names;
        }
    }

}
