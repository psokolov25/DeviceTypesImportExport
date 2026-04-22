package ru.aritmos.dtt.model.mapping;

import org.junit.jupiter.api.Test;
import ru.aritmos.dtt.model.canonical.CanonicalDeviceTypeMetadata;
import ru.aritmos.dtt.model.canonical.CanonicalDeviceTypeTemplate;
import ru.aritmos.dtt.model.canonical.CanonicalParameterDefinition;
import ru.aritmos.dtt.model.canonical.CanonicalParameterSchema;
import ru.aritmos.dtt.model.canonical.CanonicalScriptSet;
import ru.aritmos.dtt.model.canonical.CanonicalTemplateOrigin;
import ru.aritmos.dtt.model.canonical.CanonicalTemplateValues;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class DefaultCanonicalProjectionMapperTest {

    private final CanonicalProjectionMapper mapper = new DefaultCanonicalProjectionMapper();

    @Test
    void shouldProjectCanonicalTemplateToProfileWithNestedAndArrayMetadata() {
        final CanonicalDeviceTypeTemplate template = new CanonicalDeviceTypeTemplate(
                "1.0",
                new CanonicalDeviceTypeMetadata("display", "Display", "Display", "desc"),
                new CanonicalParameterSchema(Map.of(
                        "zones", new CanonicalParameterDefinition(
                                "zones",
                                "Object",
                                Map.of(),
                                Map.of("main", new CanonicalParameterDefinition(
                                        "main",
                                        "Number",
                                        Map.of("description", "Main zone"),
                                        Map.of(),
                                        null
                                )),
                                null
                        ),
                        "sensors", new CanonicalParameterDefinition(
                                "sensors",
                                "Array",
                                Map.of("displayName", "Sensors"),
                                Map.of(),
                                new CanonicalParameterDefinition(
                                        "sensor",
                                        "Object",
                                        Map.of(),
                                        Map.of("id", new CanonicalParameterDefinition(
                                                "id",
                                                "String",
                                                Map.of("description", "Sensor id"),
                                                Map.of(),
                                                null
                                        )),
                                        null
                                )
                        )
                )),
                new CanonicalParameterSchema(Map.of()),
                new CanonicalTemplateValues(Map.of()),
                new CanonicalTemplateValues(Map.of(
                        "zones", Map.of("main", 1),
                        "sensors", List.of(Map.of("id", "A1"))
                )),
                new CanonicalTemplateValues(Map.of(
                        "zones", Map.of("main", 7),
                        "sensors", List.of(Map.of("id", "B1"))
                )),
                new CanonicalTemplateOrigin("PROFILE_JSON", "", Map.of()),
                new CanonicalScriptSet(null, null, null, null, null, Map.of(), Map.of())
        );

        final var projection = mapper.toProfileProjection(template);
        final Map<String, Object> zones = castToMap(projection.deviceTypeParamValues().get("zones"));
        final Map<String, Object> zonesValue = castToMap(zones.get("value"));
        final Map<String, Object> main = castToMap(zonesValue.get("main"));
        assertThat(main).containsEntry("description", "Main zone");
        assertThat(main).containsEntry("value", 1);
        assertThat(main).containsEntry("exampleValue", 7);

        final Map<String, Object> sensors = castToMap(projection.deviceTypeParamValues().get("sensors"));
        assertThat(sensors).containsEntry("displayName", "Sensors");
        final List<?> sensorsValue = (List<?>) sensors.get("value");
        final Map<String, Object> first = castToMap(sensorsValue.get(0));
        final Map<String, Object> firstValue = castToMap(first.get("value"));
        final Map<String, Object> id = castToMap(firstValue.get("id"));
        assertThat(id).containsEntry("description", "Sensor id");
        assertThat(id).containsEntry("value", "A1");
        assertThat(id).containsEntry("exampleValue", "B1");
    }

    @Test
    void shouldProjectCanonicalTemplateToBranchWithProvidedKind() {
        final CanonicalDeviceTypeTemplate template = new CanonicalDeviceTypeTemplate(
                "1.0",
                new CanonicalDeviceTypeMetadata("display", "Display", "Display", "desc"),
                new CanonicalParameterSchema(Map.of()),
                new CanonicalParameterSchema(Map.of()),
                new CanonicalTemplateValues(Map.of()),
                new CanonicalTemplateValues(Map.of("ip", "10.0.0.1")),
                new CanonicalTemplateValues(Map.of()),
                new CanonicalTemplateOrigin("BRANCH_EQUIPMENT_JSON", "", Map.of()),
                new CanonicalScriptSet(null, null, null, null, null, Map.of(), Map.of())
        );

        final var branchProjection = mapper.toBranchProjection(template, "display_kind");
        assertThat(branchProjection.kind()).isEqualTo("display_kind");
        assertThat(branchProjection.deviceTypeParamValues()).containsEntry("ip", "10.0.0.1");
    }

    @Test
    void shouldKeepArrayItemsSchemaWhenArrayValueIsMissing() {
        final CanonicalDeviceTypeTemplate template = new CanonicalDeviceTypeTemplate(
                "1.0",
                new CanonicalDeviceTypeMetadata("display", "Display", "Display", "desc"),
                new CanonicalParameterSchema(Map.of(
                        "sensors", new CanonicalParameterDefinition(
                                "sensors",
                                "Array",
                                Map.of("displayName", "Sensors"),
                                Map.of(),
                                new CanonicalParameterDefinition(
                                        "sensor",
                                        "Object",
                                        Map.of(),
                                        Map.of("id", new CanonicalParameterDefinition(
                                                "id",
                                                "String",
                                                Map.of("nullable", true),
                                                Map.of(),
                                                null
                                        )),
                                        null
                                )
                        )
                )),
                new CanonicalParameterSchema(Map.of()),
                new CanonicalTemplateValues(Map.of()),
                new CanonicalTemplateValues(Map.of()),
                new CanonicalTemplateValues(Map.of()),
                new CanonicalTemplateOrigin("PROFILE_JSON", "", Map.of()),
                new CanonicalScriptSet(null, null, null, null, null, Map.of(), Map.of())
        );

        final var projection = mapper.toProfileProjection(template);
        final Map<String, Object> sensors = castToMap(projection.deviceTypeParamValues().get("sensors"));
        assertThat(sensors).containsEntry("displayName", "Sensors");
        assertThat(sensors).containsKey("items");
        assertThat(sensors).containsEntry("value", null);

        final Map<String, Object> items = castToMap(sensors.get("items"));
        final Map<String, Object> nested = castToMap(items.get("parametersMap"));
        final Map<String, Object> id = castToMap(nested.get("id"));
        assertThat(id)
                .containsEntry("type", "String")
                .containsEntry("nullable", true);
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> castToMap(Object value) {
        return (Map<String, Object>) value;
    }
}
