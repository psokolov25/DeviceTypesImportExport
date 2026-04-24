package ru.aritmos.dtt.demo.service;

import org.junit.jupiter.api.Test;
import ru.aritmos.dtt.api.dto.MergeStrategy;
import ru.aritmos.dtt.api.dto.branch.DeviceInstanceImportRequest;
import ru.aritmos.dtt.api.dto.importplan.BranchImportSourceRequest;
import ru.aritmos.dtt.api.dto.importplan.ProfileDeviceTypeImportSourceRequest;
import ru.aritmos.dtt.demo.dto.ImportBranchDerivedDeviceTypeRequest;
import ru.aritmos.dtt.demo.dto.ImportBranchMetadataRequest;
import ru.aritmos.dtt.demo.dto.ImportDeviceTypeMetadataOverrideRequest;
import ru.aritmos.dtt.demo.dto.ImportProfileBranchWithMetadataRequest;
import ru.aritmos.dtt.demo.dto.ImportDttSetToBranchRequest;
import ru.aritmos.dtt.demo.dto.ImportDttSetToProfileRequest;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class ImportPlanRequestMapperTest {

    private final ImportPlanRequestMapper mapper = new ImportPlanRequestMapper();

    @Test
    void shouldMapProfileImportRequest() {
        final ImportDttSetToProfileRequest request = new ImportDttSetToProfileRequest(
                List.of("base64"),
                MergeStrategy.FAIL_IF_EXISTS,
                List.of(new ProfileDeviceTypeImportSourceRequest("base64", null, Map.of("ip", "10.0.0.1"), null))
        );

        final var mapped = mapper.toLibraryProfileImportPlan(request);

        assertThat(mapped.archivesBase64()).containsExactly("base64");
        assertThat(mapped.mergeStrategy()).isEqualTo(MergeStrategy.FAIL_IF_EXISTS);
        assertThat(mapped.deviceTypes()).hasSize(1);
    }

    @Test
    void shouldMapBranchImportRequest() {
        final ImportDttSetToBranchRequest request = new ImportDttSetToBranchRequest(
                List.of("base64"),
                List.of("branch-a"),
                MergeStrategy.REPLACE,
                List.of(new BranchImportSourceRequest("branch-b", "Branch B", List.of()))
        );

        final var mapped = mapper.toLibraryBranchImportPlan(request);

        assertThat(mapped.archivesBase64()).containsExactly("base64");
        assertThat(mapped.branchIds()).containsExactly("branch-a");
        assertThat(mapped.mergeStrategy()).isEqualTo(MergeStrategy.REPLACE);
        assertThat(mapped.branches()).extracting(BranchImportSourceRequest::branchId).containsExactly("branch-b");
    }

    @Test
    void shouldMapProfileBranchRequestWithDerivedBranchOverrides() {
        final ImportProfileBranchWithMetadataRequest request = new ImportProfileBranchWithMetadataRequest(
                List.of(new ru.aritmos.dtt.demo.dto.ImportProfileDeviceTypeRequest(
                        "base64",
                        Map.of("SecondZoneColor", "white"),
                        new ImportDeviceTypeMetadataOverrideRequest("display-white", "Display White", "Display White", "white")
                )),
                List.of(new ImportBranchMetadataRequest(
                        "branch-a",
                        List.of(),
                        List.of(new ImportBranchDerivedDeviceTypeRequest(
                                "display-white",
                                Map.of("SecondZoneColor", "red"),
                                new ImportDeviceTypeMetadataOverrideRequest("display-white", "Display Red", "Display Red", "red"),
                                List.of(new ru.aritmos.dtt.demo.dto.ImportBranchDeviceRequest(
                                        "dev-1", "dev-1", "Dev 1", "desc", Map.of("IP", "10.0.0.1")
                                )),
                                "DisplayKind"
                        ))
                )),
                MergeStrategy.FAIL_IF_EXISTS
        );

        final var mapped = mapper.toLibraryProfileBranchMetadataImportPlan(request);

        assertThat(mapped.branches()).hasSize(1);
        assertThat(mapped.branches().get(0).deviceTypeOverrides()).hasSize(1);
        assertThat(mapped.branches().get(0).deviceTypeOverrides().get(0).profileDeviceTypeId()).isEqualTo("display-white");
        assertThat(mapped.branches().get(0).deviceTypeOverrides().get(0).deviceTypeParamValues())
                .containsEntry("SecondZoneColor", "red");
        final DeviceInstanceImportRequest mappedDevice = mapped.branches().get(0).deviceTypeOverrides().get(0).devices().get(0);
        assertThat(mappedDevice.id()).isEqualTo("dev-1");
        assertThat(mappedDevice.deviceParamValues()).containsEntry("IP", "10.0.0.1");
    }
}
