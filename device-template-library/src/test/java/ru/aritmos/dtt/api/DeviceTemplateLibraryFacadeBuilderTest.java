package ru.aritmos.dtt.api;

import org.junit.jupiter.api.Test;
import ru.aritmos.dtt.api.dto.ValidationIssue;
import ru.aritmos.dtt.api.dto.ValidationResult;
import ru.aritmos.dtt.api.dto.DeviceTypeMetadata;
import ru.aritmos.dtt.archive.model.DttArchiveDescriptor;
import ru.aritmos.dtt.archive.model.DttArchiveTemplate;
import ru.aritmos.dtt.json.branch.BranchEquipment;
import ru.aritmos.dtt.json.branch.DeviceManagerBranchJsonGenerator;
import ru.aritmos.dtt.json.profile.EquipmentProfile;
import ru.aritmos.dtt.json.profile.EquipmentProfileJsonGenerator;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class DeviceTemplateLibraryFacadeBuilderTest {

    @Test
    void shouldUseCustomValidationAndJsonGenerators() {
        final TemplateValidationService customValidation = template ->
                new ValidationResult(false, List.of(new ValidationIssue("X", "p", "m")));
        final EquipmentProfileJsonGenerator customProfileGenerator = profile -> "custom-profile";
        final DeviceManagerBranchJsonGenerator customBranchGenerator = branch -> "custom-branch";

        final DeviceTemplateLibraryFacade facade = DeviceTemplateLibrary.createFacadeBuilder()
                .withValidationService(customValidation)
                .withProfileJsonGenerator(customProfileGenerator)
                .withBranchJsonGenerator(customBranchGenerator)
                .build();

        final ValidationResult result = facade.validate(template());

        assertThat(result.valid()).isFalse();
        assertThat(result.issues()).hasSize(1);
        assertThat(facade.toProfileJson(new EquipmentProfile(Map.of()))).isEqualTo("custom-profile");
        assertThat(facade.toBranchJson(new BranchEquipment(Map.of()))).isEqualTo("custom-branch");
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
