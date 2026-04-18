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
}
