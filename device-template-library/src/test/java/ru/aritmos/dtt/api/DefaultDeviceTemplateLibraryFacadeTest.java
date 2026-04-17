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
                        List.of(new BranchDeviceTypeImportRequest(new EquipmentProfileDeviceTypeRequest(deviceTypeTemplate, true), List.of()))
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
                        List.of(new BranchDeviceTypeImportRequest(new EquipmentProfileDeviceTypeRequest(deviceTypeTemplate, true), List.of()))
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

        final var exported = facade.exportDttSetFromProfile(new ProfileExportRequest(profile, List.of("cashbox")));

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
                        List.of(new BranchDeviceTypeImportRequest(new EquipmentProfileDeviceTypeRequest(deviceTypeTemplate, true), List.of()))
                )),
                MergeStrategy.FAIL_IF_EXISTS
        ));

        assertThatThrownBy(() -> facade.exportDttSetFromBranch(new BranchEquipmentExportRequest(
                branch,
                List.of("missing-branch"),
                List.of("display"),
                MergeStrategy.FAIL_IF_EXISTS
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

        final byte[] zipPayload = facade.exportProfileToDttZip(new ProfileExportRequest(profileFromBase64, List.of("display")));
        assertThat(countDttEntries(zipPayload)).isEqualTo(1);

        final var profileFromZip = facade.importDttZipToProfile(zipPayload, MergeStrategy.FAIL_IF_EXISTS);
        assertThat(profileFromZip.deviceTypes()).containsKey("display");

        final String zipBase64 = facade.exportProfileToDttZipBase64(new ProfileExportRequest(profileFromBase64, List.of("display")));
        assertThat(zipBase64).isNotBlank();
    }

    private DttArchiveTemplate template() {
        return new DttArchiveTemplate(
                new DttArchiveDescriptor("DTT", "1.0", "display"),
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
