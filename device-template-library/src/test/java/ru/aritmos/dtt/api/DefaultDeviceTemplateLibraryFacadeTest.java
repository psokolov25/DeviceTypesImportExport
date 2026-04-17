package ru.aritmos.dtt.api;

import org.junit.jupiter.api.Test;
import ru.aritmos.dtt.api.dto.DeviceTypeMetadata;
import ru.aritmos.dtt.api.dto.EquipmentProfileAssemblyRequest;
import ru.aritmos.dtt.api.dto.EquipmentProfileDeviceTypeRequest;
import ru.aritmos.dtt.api.dto.MergeStrategy;
import ru.aritmos.dtt.api.dto.branch.BranchDeviceTypeImportRequest;
import ru.aritmos.dtt.api.dto.branch.BranchEquipmentAssemblyRequest;
import ru.aritmos.dtt.api.dto.branch.BranchImportRequest;
import ru.aritmos.dtt.archive.model.DttArchiveDescriptor;
import ru.aritmos.dtt.archive.model.DttArchiveTemplate;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

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
        assertThat(facade.toProfileJson(profile)).contains("display");

        final var branch = facade.assembleBranch(new BranchEquipmentAssemblyRequest(
                List.of(new BranchImportRequest(
                        "branch-1",
                        "Main",
                        List.of(new BranchDeviceTypeImportRequest(new EquipmentProfileDeviceTypeRequest(deviceTypeTemplate, true), List.of()))
                )),
                MergeStrategy.FAIL_IF_EXISTS
        ));

        assertThat(branch.branches()).containsKey("branch-1");
        assertThat(facade.toBranchJson(branch)).contains("branch-1");
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
}
