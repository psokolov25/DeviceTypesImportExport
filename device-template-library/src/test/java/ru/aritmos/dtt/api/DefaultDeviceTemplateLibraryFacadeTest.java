package ru.aritmos.dtt.api;

import org.junit.jupiter.api.Test;
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

import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

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
    void shouldSupportBase64AndZipFacadeModes() throws Exception {
        final DttArchiveTemplate template = template();
        final byte[] archiveBytes = facade.writeDtt(template);
        final String archiveBase64 = java.util.Base64.getEncoder().encodeToString(archiveBytes);

        final var profileFromBase64 = facade.importDttBase64SetToProfile(List.of(archiveBase64), MergeStrategy.FAIL_IF_EXISTS);
        assertThat(profileFromBase64.deviceTypes()).containsKey("display");

        final byte[] zipPayload = facade.exportProfileToDttZip(new ProfileExportRequest(profileFromBase64, List.of("display"), null));
        assertThat(countDttEntries(zipPayload)).isEqualTo(1);

        final var profileFromZip = facade.importDttZipToProfile(zipPayload, MergeStrategy.FAIL_IF_EXISTS);
        assertThat(profileFromZip.deviceTypes()).containsKey("display");

        final String zipBase64 = facade.exportProfileToDttZipBase64(new ProfileExportRequest(profileFromBase64, List.of("display"), null));
        assertThat(zipBase64).isNotBlank();
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

    private DttArchiveTemplate template() {
        return new DttArchiveTemplate(
                new DttArchiveDescriptor("DTT", "1.0", "display", null),
                new DeviceTypeMetadata("display", "Display", "Display", "desc"),
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
}
