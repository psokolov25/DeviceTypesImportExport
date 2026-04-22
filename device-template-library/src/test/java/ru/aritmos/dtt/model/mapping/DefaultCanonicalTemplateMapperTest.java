package ru.aritmos.dtt.model.mapping;

import org.junit.jupiter.api.Test;
import ru.aritmos.dtt.api.dto.DeviceTypeMetadata;
import ru.aritmos.dtt.archive.model.DttArchiveDescriptor;
import ru.aritmos.dtt.archive.model.DttArchiveTemplate;
import ru.aritmos.dtt.model.canonical.CanonicalDeviceTypeMetadata;
import ru.aritmos.dtt.model.canonical.CanonicalDeviceTypeTemplate;
import ru.aritmos.dtt.model.canonical.CanonicalParameterSchema;
import ru.aritmos.dtt.model.canonical.CanonicalScriptSet;
import ru.aritmos.dtt.model.canonical.CanonicalTemplateOrigin;

import java.util.LinkedHashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class DefaultCanonicalTemplateMapperTest {

    private final CanonicalTemplateMapper mapper = new DefaultCanonicalTemplateMapper();

    @Test
    void shouldMapArchiveToCanonicalAndBackWithoutLosingScripts() {
        final DttArchiveTemplate archive = new DttArchiveTemplate(
                new DttArchiveDescriptor("DTT", "1.2", "display", null),
                new DeviceTypeMetadata("display", "Display", "Дисплей", "desc"),
                Map.of("p", Map.of("type", "String")),
                Map.of("ip", Map.of("type", "String")),
                Map.of("h", "v"),
                Map.of("a", "1"),
                Map.of("a", "2"),
                Map.of("sourceKind", "PROFILE_JSON"),
                "println 'start'",
                "println 'stop'",
                "println 'publicStart'",
                "println 'publicFinish'",
                "def f() { 1 }",
                Map.of("VISIT", "println 'visit'"),
                Map.of("RESET", "println 'reset'")
        );

        final var canonical = mapper.toCanonical(archive);
        final var restored = mapper.toArchive(canonical);

        assertThat(canonical.templateOrigin().sourceKind()).isEqualTo("PROFILE_JSON");
        assertThat(canonical.deviceTypeParameterSchema().parameters()).containsKey("p");
        assertThat(restored.metadata().id()).isEqualTo("display");
        assertThat(restored.descriptor().formatVersion()).isEqualTo("1.2");
        assertThat(restored.onStartEvent()).isEqualTo("println 'start'");
        assertThat(restored.eventHandlers()).containsEntry("VISIT", "println 'visit'");
        assertThat(restored.commands()).containsEntry("RESET", "println 'reset'");
        assertThat(restored.templateOrigin()).containsEntry("sourceKind", "PROFILE_JSON");
    }

    @Test
    void shouldPreserveNestedSchemaAndMetadataOnRoundTrip() {
        final DttArchiveTemplate archive = new DttArchiveTemplate(
                new DttArchiveDescriptor("DTT", "1.0", "display", "2.0.0"),
                new DeviceTypeMetadata("display", "Display", "Display", "desc"),
                Map.of(
                        "zones", Map.of(
                                "name", "zones",
                                "type", "Object",
                                "displayName", "Zones",
                                "parametersMap", Map.of(
                                        "main", Map.of(
                                                "name", "main",
                                                "type", "Number",
                                                "description", "Main zone number",
                                                "exampleValue", 1
                                        )
                                )
                        ),
                        "sensors", Map.of(
                                "name", "sensors",
                                "type", "Array",
                                "items", Map.of(
                                        "name", "sensorItem",
                                        "type", "String",
                                        "displayName", "Sensor code",
                                        "exampleValue", "A1"
                                )
                        )
                ),
                Map.of(),
                Map.of("hint", "value"),
                Map.of("ip", "127.0.0.1"),
                Map.of("ip", "192.168.1.10"),
                Map.of("sourceKind", "PROFILE_JSON", "sourceSummary", "profile-export", "sourceId", "profile-1"),
                null,
                null,
                null,
                null,
                null,
                Map.of(),
                Map.of()
        );

        final var canonical = mapper.toCanonical(archive);
        final var restored = mapper.toArchive(canonical);

        assertThat(canonical.deviceTypeParameterSchema().parameters()).containsKeys("zones", "sensors");
        final Map<String, Object> zones = castToMap(restored.deviceTypeParametersSchema().get("zones"));
        final Map<String, Object> zonesNested = castToMap(zones.get("parametersMap"));
        final Map<String, Object> main = castToMap(zonesNested.get("main"));
        assertThat(main)
                .containsEntry("description", "Main zone number")
                .containsEntry("exampleValue", 1);

        final Map<String, Object> sensors = castToMap(restored.deviceTypeParametersSchema().get("sensors"));
        final Map<String, Object> items = castToMap(sensors.get("items"));
        assertThat(items)
                .containsEntry("displayName", "Sensor code")
                .containsEntry("exampleValue", "A1");
        assertThat(restored.templateOrigin())
                .containsEntry("sourceKind", "PROFILE_JSON")
                .containsEntry("sourceSummary", "profile-export")
                .containsEntry("sourceId", "profile-1");
    }

    @Test
    void shouldHandleNullCanonicalValueContainersWhenConvertingToArchive() {
        final CanonicalDeviceTypeTemplate canonical = new CanonicalDeviceTypeTemplate(
                "1.0",
                new CanonicalDeviceTypeMetadata("display", "Display", "Display", "desc"),
                new CanonicalParameterSchema(Map.of()),
                new CanonicalParameterSchema(Map.of()),
                null,
                null,
                null,
                new CanonicalTemplateOrigin("UNSPECIFIED", "", Map.of()),
                new CanonicalScriptSet(null, null, null, null, null, Map.of(), Map.of())
        );

        final DttArchiveTemplate archive = mapper.toArchive(canonical);
        assertThat(archive.bindingHints()).isEmpty();
        assertThat(archive.defaultValues()).isEmpty();
        assertThat(archive.exampleValues()).isEmpty();
    }

    @Test
    void shouldPreserveNullableAndNestedArrayObjectMetadataOnRoundTrip() {
        final DttArchiveTemplate archive = new DttArchiveTemplate(
                new DttArchiveDescriptor("DTT", "1.1", "display", null),
                new DeviceTypeMetadata("display", "Display", "Display", "desc"),
                Map.of(
                        "optionalComment", Map.of(
                                "name", "optionalComment",
                                "type", "String",
                                "nullable", true,
                                "description", "Optional text value"
                        ),
                        "zones", Map.of(
                                "name", "zones",
                                "type", "Array",
                                "items", Map.of(
                                        "name", "zoneItem",
                                        "type", "Object",
                                        "parametersMap", Map.of(
                                                "zoneId", Map.of(
                                                        "name", "zoneId",
                                                        "type", "Number",
                                                        "nullable", false
                                                ),
                                                "zoneLabel", Map.of(
                                                        "name", "zoneLabel",
                                                        "type", "String",
                                                        "nullable", true,
                                                        "exampleValue", "A-zone"
                                                )
                                        )
                                )
                        )
                ),
                Map.of(),
                Map.of(),
                mapWithNull("optionalComment"),
                mapWithNull("optionalComment"),
                Map.of("sourceKind", "BRANCH_JSON"),
                null,
                null,
                null,
                null,
                null,
                Map.of(),
                Map.of()
        );

        final var canonical = mapper.toCanonical(archive);
        final var restored = mapper.toArchive(canonical);

        final Map<String, Object> optionalComment = castToMap(restored.deviceTypeParametersSchema().get("optionalComment"));
        assertThat(optionalComment)
                .containsEntry("type", "String")
                .containsEntry("nullable", true);

        final Map<String, Object> zones = castToMap(restored.deviceTypeParametersSchema().get("zones"));
        final Map<String, Object> items = castToMap(zones.get("items"));
        final Map<String, Object> nested = castToMap(items.get("parametersMap"));
        final Map<String, Object> zoneLabel = castToMap(nested.get("zoneLabel"));
        assertThat(zoneLabel)
                .containsEntry("type", "String")
                .containsEntry("nullable", true)
                .containsEntry("exampleValue", "A-zone");
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> castToMap(Object value) {
        return (Map<String, Object>) value;
    }

    private Map<String, Object> mapWithNull(String key) {
        final Map<String, Object> result = new LinkedHashMap<>();
        result.put(key, null);
        return result;
    }
}
