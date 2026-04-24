package ru.aritmos.dtt.demo.service;

import org.junit.jupiter.api.Test;
import ru.aritmos.dtt.api.dto.MergeStrategy;
import ru.aritmos.dtt.api.dto.importplan.BranchImportSourceRequest;
import ru.aritmos.dtt.api.dto.importplan.ProfileDeviceTypeImportSourceRequest;
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
}
