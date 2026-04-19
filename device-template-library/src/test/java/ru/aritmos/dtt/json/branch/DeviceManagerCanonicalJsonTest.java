package ru.aritmos.dtt.json.branch;

import org.junit.jupiter.api.Test;
import ru.aritmos.dtt.api.DeviceTemplateLibrary;
import ru.aritmos.dtt.api.dto.MergeStrategy;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class DeviceManagerCanonicalJsonTest {

    private final DeviceManagerBranchJsonParser parser = new DefaultDeviceManagerBranchJsonParser();
    private final DeviceManagerBranchJsonGenerator generator = new DefaultDeviceManagerBranchJsonGenerator();

    @Test
    void shouldParseAndGenerateCanonicalDeviceManagerJson() throws Exception {
        final String json = Files.readString(Path.of("..", "DeviceManager.json"));

        final BranchEquipment parsed = parser.parse(json);
        assertThat(parsed.branches()).isNotEmpty();

        final String generated = generator.generate(parsed);
        final BranchEquipment reparsed = parser.parse(generated);

        assertThat(reparsed.branches()).isNotEmpty();
        final boolean hasRichCanonicalType = reparsed.branches().values().stream()
                .flatMap(branch -> branch.deviceTypes().values().stream())
                .anyMatch(deviceType ->
                        deviceType.kind() != null
                                && !deviceType.kind().isBlank()
                                && deviceType.eventHandlers() != null
                                && !deviceType.eventHandlers().isEmpty()
                                && deviceType.commands() != null
                                && !deviceType.commands().isEmpty()
                                && deviceType.eventHandlers().values().stream()
                                .anyMatch(script -> script.scriptCode() != null && !script.scriptCode().isBlank())
                );
        assertThat(hasRichCanonicalType).isTrue();
    }

    @Test
    void shouldExportDttFromCanonicalDeviceManagerJsonWithoutScriptLoss() throws Exception {
        final String json = Files.readString(Path.of("..", "DeviceManager.json"));
        final var facade = DeviceTemplateLibrary.createDefaultFacade();

        final var exported = facade.exportDttSetFromBranchJson(
                json,
                List.of(),
                List.of(),
                MergeStrategy.MERGE_PRESERVE_EXISTING,
                "1.0.0"
        );

        assertThat(exported.archivesByDeviceTypeId()).isNotEmpty();
        final byte[] firstArchive = exported.archivesByDeviceTypeId().values().iterator().next();
        final var template = facade.readDtt(firstArchive);

        final boolean hasLifecycleScript = template.onStartEvent() != null
                || template.onStopEvent() != null
                || template.onPublicStartEvent() != null
                || template.onPublicFinishEvent() != null;
        final boolean hasHandlerScripts = template.eventHandlers() != null && !template.eventHandlers().isEmpty();
        final boolean hasCommandScripts = template.commands() != null && !template.commands().isEmpty();

        assertThat(hasLifecycleScript || hasHandlerScripts || hasCommandScripts).isTrue();
    }

    @Test
    void shouldPreserveCanonicalDeviceTypeKindAndScriptsAfterBranchRoundTripViaDttSet() throws Exception {
        final String json = Files.readString(Path.of("..", "DeviceManager.json"));
        final var facade = DeviceTemplateLibrary.createDefaultFacade();
        final BranchEquipment source = facade.parseBranchJson(json);

        final String branchId = source.branches().keySet().iterator().next();
        final BranchDeviceType sourceType = source.branches().get(branchId).deviceTypes().entrySet().stream()
                .map(java.util.Map.Entry::getValue)
                .filter(deviceType -> deviceType.kind() != null && !deviceType.kind().isBlank())
                .filter(deviceType -> (deviceType.eventHandlers() != null && !deviceType.eventHandlers().isEmpty())
                        || (deviceType.commands() != null && !deviceType.commands().isEmpty()))
                .findFirst()
                .orElseThrow();
        final String sourceTypeId = sourceType.template().metadata().id();

        final var exported = facade.exportDttSetFromBranch(
                source,
                MergeStrategy.MERGE_PRESERVE_EXISTING
        );
        final BranchEquipment restored = facade.importDttSetToBranch(
                List.copyOf(exported.archivesByDeviceTypeId().values()),
                List.of(branchId),
                MergeStrategy.REPLACE
        );

        final BranchDeviceType restoredType = restored.branches().get(branchId).deviceTypes().get(sourceTypeId);
        assertThat(restoredType).isNotNull();
        assertThat(restoredType.kind()).isEqualTo(sourceType.kind());
        assertThat(restoredType.eventHandlers().keySet()).containsAll(sourceType.eventHandlers().keySet());
        assertThat(restoredType.commands().keySet()).containsAll(sourceType.commands().keySet());
    }

    @Test
    void shouldMergeDttIntoExistingCanonicalDeviceManagerJsonWithCopyStrategy() throws Exception {
        final String json = Files.readString(Path.of("..", "DeviceManager.json"));
        final var facade = DeviceTemplateLibrary.createDefaultFacade();
        final BranchEquipment source = facade.parseBranchJson(json);

        final String branchId = source.branches().keySet().iterator().next();
        final String deviceTypeId = source.branches().get(branchId).deviceTypes().keySet().iterator().next();
        final byte[] archive = facade.exportDttSetFromBranch(
                source,
                MergeStrategy.MERGE_PRESERVE_EXISTING
        ).archivesByDeviceTypeId().get(deviceTypeId);

        final BranchEquipment merged = facade.importDttSetToExistingBranch(
                List.of(archive),
                source,
                List.of(branchId),
                MergeStrategy.CREATE_COPY_WITH_SUFFIX
        );

        final var mergedTypes = merged.branches().get(branchId).deviceTypes();
        assertThat(mergedTypes).containsKey(deviceTypeId);
        assertThat(mergedTypes.keySet().stream().anyMatch(key -> key.startsWith(deviceTypeId + "_copy_"))).isTrue();
    }
}
