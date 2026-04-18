package ru.aritmos.dtt.assembly.branch;

import org.junit.jupiter.api.Test;
import ru.aritmos.dtt.api.TemplateAssemblyService;
import ru.aritmos.dtt.api.dto.DeviceTypeMetadata;
import ru.aritmos.dtt.api.dto.DeviceTypeTemplate;
import ru.aritmos.dtt.api.dto.EquipmentProfileDeviceTypeRequest;
import ru.aritmos.dtt.api.dto.MergeStrategy;
import ru.aritmos.dtt.api.dto.branch.BranchDeviceTypeImportRequest;
import ru.aritmos.dtt.api.dto.branch.BranchEquipmentAssemblyRequest;
import ru.aritmos.dtt.api.dto.branch.BranchImportRequest;
import ru.aritmos.dtt.api.dto.branch.DeviceInstanceImportRequest;
import ru.aritmos.dtt.assembly.DefaultTemplateAssemblyService;
import ru.aritmos.dtt.exception.TemplateAssemblyException;
import ru.aritmos.dtt.json.branch.BranchScript;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class BranchAssemblyServiceTest {

    private final TemplateAssemblyService service = new DefaultTemplateAssemblyService();

    @Test
    void shouldAssembleBranchWithTypeWithoutChildDevices() {
        final DeviceTypeTemplate type = type("type-1");
        final BranchEquipmentAssemblyRequest request = new BranchEquipmentAssemblyRequest(
                List.of(new BranchImportRequest(
                        "branch-1",
                        "Main",
                        List.of(new BranchDeviceTypeImportRequest(
                                new EquipmentProfileDeviceTypeRequest(type, true),
                                List.of(),
                                null, null, null, null, null, null, Map.of(), Map.of()
                        ))
                )),
                MergeStrategy.FAIL_IF_EXISTS
        );

        final var result = service.assembleBranchEquipment(request);

        assertThat(result.branches()).containsKey("branch-1");
        assertThat(result.branches().get("branch-1").deviceTypes().get("type-1").devices()).isEmpty();
    }

    @Test
    void shouldAssembleBranchWithMultipleDevices() {
        final DeviceTypeTemplate type = type("type-1");
        final BranchEquipmentAssemblyRequest request = new BranchEquipmentAssemblyRequest(
                List.of(new BranchImportRequest(
                        "branch-1",
                        "Main",
                        List.of(new BranchDeviceTypeImportRequest(
                                new EquipmentProfileDeviceTypeRequest(type, true),
                                List.of(
                                        new DeviceInstanceImportRequest("dev-1", "Dev1", "Dev1", "d", Map.of("ip", "1")),
                                        new DeviceInstanceImportRequest("dev-2", "Dev2", "Dev2", "d", Map.of("ip", "2"))
                                ),
                                null, null, null, null, null, null, Map.of(), Map.of()
                        ))
                )),
                MergeStrategy.FAIL_IF_EXISTS
        );

        final var result = service.assembleBranchEquipment(request);

        assertThat(result.branches().get("branch-1").deviceTypes().get("type-1").devices())
                .containsKeys("dev-1", "dev-2");
    }

    @Test
    void shouldCreateCopyWhenTypeConflictInBranch() {
        final DeviceTypeTemplate type = type("type-1");
        final BranchDeviceTypeImportRequest first = new BranchDeviceTypeImportRequest(
                new EquipmentProfileDeviceTypeRequest(type, true),
                List.of(new DeviceInstanceImportRequest("dev-1", "Dev1", "Dev1", "d", Map.of())),
                null, null, null, null, null, null, Map.of(), Map.of()
        );
        final BranchDeviceTypeImportRequest second = new BranchDeviceTypeImportRequest(
                new EquipmentProfileDeviceTypeRequest(type, true),
                List.of(new DeviceInstanceImportRequest("dev-2", "Dev2", "Dev2", "d", Map.of())),
                null, null, null, null, null, null, Map.of(), Map.of()
        );

        final var result = service.assembleBranchEquipment(new BranchEquipmentAssemblyRequest(
                List.of(new BranchImportRequest("branch-1", "Main", List.of(first, second))),
                MergeStrategy.CREATE_COPY_WITH_SUFFIX
        ));

        assertThat(result.branches().get("branch-1").deviceTypes())
                .containsKeys("type-1", "type-1_copy_1");
    }

    @Test
    void shouldFailOnConflictWhenStrategyFailIfExists() {
        final DeviceTypeTemplate type = type("type-1");
        final BranchDeviceTypeImportRequest first = new BranchDeviceTypeImportRequest(
                new EquipmentProfileDeviceTypeRequest(type, true),
                List.of(),
                null, null, null, null, null, null, Map.of(), Map.of()
        );
        final BranchDeviceTypeImportRequest second = new BranchDeviceTypeImportRequest(
                new EquipmentProfileDeviceTypeRequest(type, true),
                List.of(),
                null, null, null, null, null, null, Map.of(), Map.of()
        );

        assertThatThrownBy(() -> service.assembleBranchEquipment(new BranchEquipmentAssemblyRequest(
                List.of(new BranchImportRequest("branch-1", "Main", List.of(first, second))),
                MergeStrategy.FAIL_IF_EXISTS
        )))
                .isInstanceOf(TemplateAssemblyException.class)
                .hasMessageContaining("уже существует");
    }

    @Test
    void shouldPreviewBranchAssembly() {
        final DeviceTypeTemplate type = type("type-1");
        final BranchEquipmentAssemblyRequest request = new BranchEquipmentAssemblyRequest(
                List.of(new BranchImportRequest(
                        "branch-1",
                        "Main",
                        List.of(new BranchDeviceTypeImportRequest(
                                new EquipmentProfileDeviceTypeRequest(type, true),
                                List.of(),
                                null, null, null, null, null, null, Map.of(), Map.of()
                        ))
                )),
                MergeStrategy.FAIL_IF_EXISTS
        );

        final var result = service.previewBranchEquipment(request);

        assertThat(result.branches()).containsKey("branch-1");
        assertThat(result.branches().get("branch-1").deviceTypes()).containsKey("type-1");
    }

    @Test
    void shouldRespectMergeStrategyForBranchScriptSections() {
        final DeviceTypeTemplate type = type("type-1");
        final BranchDeviceTypeImportRequest first = new BranchDeviceTypeImportRequest(
                new EquipmentProfileDeviceTypeRequest(type, true),
                List.of(),
                "kind-a",
                new BranchScript(Map.of(), List.of(), "println 'first'"),
                null,
                null,
                null,
                "println 'fn-first'",
                Map.of("EVENT", new BranchScript(Map.of(), List.of(), "println 'event-first'")),
                Map.of()
        );
        final BranchDeviceTypeImportRequest second = new BranchDeviceTypeImportRequest(
                new EquipmentProfileDeviceTypeRequest(type, true),
                List.of(),
                "kind-b",
                new BranchScript(Map.of(), List.of(), "println 'second'"),
                null,
                null,
                null,
                "println 'fn-second'",
                Map.of("EVENT", new BranchScript(Map.of(), List.of(), "println 'event-second'")),
                Map.of()
        );

        final var mergedNonNulls = service.assembleBranchEquipment(new BranchEquipmentAssemblyRequest(
                List.of(new BranchImportRequest("branch-1", "Main", List.of(first, second))),
                MergeStrategy.MERGE_NON_NULLS
        ));
        final var mergedPreserve = service.assembleBranchEquipment(new BranchEquipmentAssemblyRequest(
                List.of(new BranchImportRequest("branch-1", "Main", List.of(first, second))),
                MergeStrategy.MERGE_PRESERVE_EXISTING
        ));

        final var nonNullsType = mergedNonNulls.branches().get("branch-1").deviceTypes().get("type-1");
        assertThat(nonNullsType.kind()).isEqualTo("kind-b");
        assertThat(nonNullsType.onStartEvent().scriptCode()).isEqualTo("println 'second'");
        assertThat(nonNullsType.deviceTypeFunctions()).isEqualTo("println 'fn-second'");
        assertThat(nonNullsType.eventHandlers().get("EVENT").scriptCode()).isEqualTo("println 'event-second'");

        final var preserveType = mergedPreserve.branches().get("branch-1").deviceTypes().get("type-1");
        assertThat(preserveType.kind()).isEqualTo("kind-a");
        assertThat(preserveType.onStartEvent().scriptCode()).isEqualTo("println 'first'");
        assertThat(preserveType.deviceTypeFunctions()).isEqualTo("println 'fn-first'");
        assertThat(preserveType.eventHandlers().get("EVENT").scriptCode()).isEqualTo("println 'event-first'");
    }

    private DeviceTypeTemplate type(String id) {
        return new DeviceTypeTemplate(new DeviceTypeMetadata(id, id, id, id), Map.of("a", "b"));
    }
}
