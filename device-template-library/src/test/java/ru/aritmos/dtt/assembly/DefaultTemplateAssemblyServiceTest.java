package ru.aritmos.dtt.assembly;

import org.junit.jupiter.api.Test;
import ru.aritmos.dtt.api.TemplateAssemblyService;
import ru.aritmos.dtt.api.dto.DeviceTypeMetadata;
import ru.aritmos.dtt.api.dto.DeviceTypeTemplate;
import ru.aritmos.dtt.api.dto.EquipmentProfileAssemblyRequest;
import ru.aritmos.dtt.api.dto.EquipmentProfileDeviceTypeRequest;
import ru.aritmos.dtt.api.dto.MergeStrategy;
import ru.aritmos.dtt.api.dto.TemplateValueOverride;
import ru.aritmos.dtt.exception.TemplateAssemblyException;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DefaultTemplateAssemblyServiceTest {

    private final TemplateAssemblyService service = new DefaultTemplateAssemblyService();

    @Test
    void shouldFailIfExists() {
        final EquipmentProfileAssemblyRequest request = requestWithConflict(MergeStrategy.FAIL_IF_EXISTS);

        assertThatThrownBy(() -> service.assembleEquipmentProfile(request))
                .isInstanceOf(TemplateAssemblyException.class)
                .hasMessageContaining("уже существует");
    }

    @Test
    void shouldReplaceOnConflict() {
        final var profile = service.assembleEquipmentProfile(requestWithConflict(MergeStrategy.REPLACE));

        assertThat(profile.deviceTypes()).hasSize(1);
        assertThat(profile.deviceTypes().get("type-1").deviceTypeParamValues())
                .containsEntry("url", "incoming");
    }

    @Test
    void shouldCreateCopyOnConflict() {
        final var profile = service.assembleEquipmentProfile(requestWithConflict(MergeStrategy.CREATE_COPY_WITH_SUFFIX));

        assertThat(profile.deviceTypes()).hasSize(2);
        assertThat(profile.deviceTypes()).containsKeys("type-1", "type-1_copy_1");
    }

    @Test
    void shouldApplyOverridesBeforeMerge() {
        final DeviceTypeTemplate template = new DeviceTypeTemplate(
                new DeviceTypeMetadata("type-2", "Name", "Display", "Desc"),
                Map.of("a", "x", "b", "y")
        );
        final EquipmentProfileAssemblyRequest request = new EquipmentProfileAssemblyRequest(
                List.of(new EquipmentProfileDeviceTypeRequest(template, true)),
                List.of(new TemplateValueOverride("type-2", Map.of("b", "override"))),
                MergeStrategy.FAIL_IF_EXISTS
        );

        final var profile = service.assembleEquipmentProfile(request);

        assertThat(profile.deviceTypes().get("type-2").deviceTypeParamValues())
                .containsEntry("b", "override");
    }

    private EquipmentProfileAssemblyRequest requestWithConflict(MergeStrategy strategy) {
        final DeviceTypeTemplate first = new DeviceTypeTemplate(
                new DeviceTypeMetadata("type-1", "Terminal", "Терминал", "desc"),
                Map.of("url", "old")
        );
        final DeviceTypeTemplate second = new DeviceTypeTemplate(
                new DeviceTypeMetadata("type-1", "Terminal", "Терминал", "desc"),
                Map.of("url", "incoming")
        );

        return new EquipmentProfileAssemblyRequest(
                List.of(
                        new EquipmentProfileDeviceTypeRequest(first, true),
                        new EquipmentProfileDeviceTypeRequest(second, true)
                ),
                List.of(),
                strategy
        );
    }
}
