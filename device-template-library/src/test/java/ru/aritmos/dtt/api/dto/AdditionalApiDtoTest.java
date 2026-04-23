package ru.aritmos.dtt.api.dto;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class AdditionalApiDtoTest {

    @Test
    void shouldInstantiateAdditionalPublicApiDtos() {
        final TemplateParameterSchema nestedSchema = new TemplateParameterSchema(Map.of());
        final TemplateParameterDefinition definition = new TemplateParameterDefinition(
                "ip",
                "IP",
                "String",
                "IP address",
                "127.0.0.1",
                Map.of("required", true),
                false,
                nestedSchema
        );
        final TemplateParameterSchema schema = new TemplateParameterSchema(Map.of("ip", definition));
        final TemplateScriptSet scriptSet = new TemplateScriptSet(
                "println 'start'",
                "println 'stop'",
                null,
                null,
                "println 'fn'",
                Map.of("EVENT", "println 'event'"),
                Map.of("CMD", "println 'cmd'")
        );
        final TemplateDefaultValues defaults = new TemplateDefaultValues(
                Map.of("ip", "127.0.0.1"),
                Map.of("dev-1", Map.of("port", 9100))
        );
        final ExportResult exportResult = new ExportResult("display", new byte[]{1, 2, 3}, "AQID");
        final ImportResult importResult = new ImportResult(
                new DeviceTypeTemplate(new DeviceTypeMetadata("display", "Display", "Display", "desc"), Map.of()),
                new ValidationResult(true, java.util.List.of())
        );
        final DeviceInstanceValueOverride override = new DeviceInstanceValueOverride(
                "display",
                "dev-1",
                Map.of("port", 9100)
        );
        final ru.aritmos.dtt.api.dto.importplan.ProfileImportPreviewResult profilePreview =
                new ru.aritmos.dtt.api.dto.importplan.ProfileImportPreviewResult(
                        new ru.aritmos.dtt.json.profile.EquipmentProfile(Map.of()),
                        Map.of("display", new ru.aritmos.dtt.api.dto.importplan.ImportPreviewComputationEntry(1, 2))
                );
        final ru.aritmos.dtt.api.dto.importplan.BranchImportPreviewResult branchPreview =
                new ru.aritmos.dtt.api.dto.importplan.BranchImportPreviewResult(
                        new ru.aritmos.dtt.json.branch.BranchEquipment(Map.of(), java.util.List.of()),
                        Map.of("branch-1:display", new ru.aritmos.dtt.api.dto.importplan.ImportPreviewComputationEntry(3, 4))
                );

        assertThat(schema.parametersMap()).containsKey("ip");
        assertThat(scriptSet.eventHandlers()).containsKey("EVENT");
        assertThat(defaults.deviceDefaults()).containsKey("dev-1");
        assertThat(exportResult.deviceTypeId()).isEqualTo("display");
        assertThat(importResult.template().metadata().id()).isEqualTo("display");
        assertThat(override.deviceId()).isEqualTo("dev-1");
        assertThat(profilePreview.computationsByDeviceType()).containsKey("display");
        assertThat(branchPreview.computationsByTarget()).containsKey("branch-1:display");
    }
}
