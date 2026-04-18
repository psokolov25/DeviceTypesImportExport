package ru.aritmos.dtt.json.branch;

import org.junit.jupiter.api.Test;
import ru.aritmos.dtt.api.dto.DeviceTypeMetadata;
import ru.aritmos.dtt.api.dto.DeviceTypeTemplate;
import ru.aritmos.dtt.exception.DttFormatException;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class BranchJsonParserGeneratorTest {

    private final DeviceManagerBranchJsonGenerator generator = new DefaultDeviceManagerBranchJsonGenerator();
    private final DeviceManagerBranchJsonParser parser = new DefaultDeviceManagerBranchJsonParser();

    @Test
    void shouldGenerateAndParseBranchJson() {
        final BranchEquipment branchEquipment = new BranchEquipment(Map.of(
                "branch-1",
                new BranchNode(
                        "branch-1",
                        "Main",
                        Map.of(
                                "type-1",
                                new BranchDeviceType(
                                        new DeviceTypeTemplate(new DeviceTypeMetadata("type-1", "name", "display", "desc"), Map.of("url", "x")),
                                        Map.of("dev-1", new DeviceInstanceTemplate("dev-1", "n", "d", "desc", Map.of("ip", "1"))),
                                        "display",
                                        new BranchScript(Map.of(), java.util.List.of(), "println 'start'"),
                                        null,
                                        null,
                                        new BranchScript(Map.of(), java.util.List.of(), null),
                                        "println 'functions'",
                                        Map.of("EVENT", new BranchScript(Map.of(), java.util.List.of(), "println 'event'")),
                                        Map.of("CMD", new BranchScript(Map.of(), java.util.List.of(), "println 'command'"))
                                )
                        )
                )
        ));

        final String json = generator.generate(branchEquipment);
        assertThat(json).contains("\"deviceTypes\":{\"type-1\":{\"id\":\"type-1\"");
        assertThat(json).doesNotContain("\"template\"");
        assertThat(json).contains("\"url\":{\"value\":\"x\",\"name\":\"url\"}");
        assertThat(json).contains("\"type\":\"display\"");
        assertThat(json).contains("\"eventHandlers\":{\"EVENT\":{\"inputParameters\":{},\"outputParameters\":[],\"scriptCode\":\"println 'event'\"}}");
        assertThat(json).contains("\"onPublicFinishEvent\":{\"inputParameters\":{},\"outputParameters\":[]}");
        assertThat(json).doesNotContain("\"onPublicFinishEvent\":{\"inputParameters\":{},\"outputParameters\":[],\"scriptCode\":null}");
        final BranchEquipment parsed = parser.parse(json);

        assertThat(parsed.branches()).containsKey("branch-1");
        assertThat(parsed.branches().get("branch-1").deviceTypes()).containsKey("type-1");
        assertThat(parsed.branches().get("branch-1").deviceTypes().get("type-1").kind()).isEqualTo("display");
        assertThat(parsed.branches().get("branch-1").deviceTypes().get("type-1").onStartEvent().scriptCode()).isEqualTo("println 'start'");
        final Map<String, Object> params = parsed.branches().get("branch-1").deviceTypes().get("type-1").template().deviceTypeParamValues();
        assertThat((Map<String, Object>) params.get("url"))
                .containsEntry("value", "x")
                .containsEntry("name", "url");
        assertThat(parsed.branches().get("branch-1").deviceTypes().get("type-1").eventHandlers()).containsKey("EVENT");
        assertThat(parsed.branches().get("branch-1").deviceTypes().get("type-1").commands()).containsKey("CMD");
    }

    @Test
    void shouldFailWhenLifecycleScriptIsNotObject() {
        final String json = """
                {
                  "branch-1": {
                    "id": "branch-1",
                    "displayName": "Main",
                    "deviceTypes": {
                      "type-1": {
                        "id": "type-1",
                        "name": "Type 1",
                        "displayName": "Type 1",
                        "description": "desc",
                        "type": "kiosk",
                        "deviceTypeParamValues": {},
                        "onStartEvent": "println 'legacy'",
                        "devices": {}
                      }
                    }
                  }
                }
                """;

        assertThatThrownBy(() -> parser.parse(json))
                .isInstanceOf(DttFormatException.class)
                .hasMessageContaining("Некорректная script-секция branch JSON");
    }

    @Test
    void shouldFailWhenDeviceTypeFunctionsIsNotString() {
        final String json = """
                {
                  "branch-1": {
                    "id": "branch-1",
                    "displayName": "Main",
                    "deviceTypes": {
                      "type-1": {
                        "id": "type-1",
                        "name": "Type 1",
                        "displayName": "Type 1",
                        "description": "desc",
                        "type": "kiosk",
                        "deviceTypeParamValues": {},
                        "deviceTypeFunctions": {
                          "scriptCode": "println 'fn'"
                        },
                        "devices": {}
                      }
                    }
                  }
                }
                """;

        assertThatThrownBy(() -> parser.parse(json))
                .isInstanceOf(DttFormatException.class)
                .hasMessageContaining("Некорректный формат deviceTypeFunctions");
    }

    @Test
    void shouldFailWhenTypeIsMissing() {
        final String json = """
                {
                  "branch-1": {
                    "id": "branch-1",
                    "displayName": "Main",
                    "deviceTypes": {
                      "type-1": {
                        "id": "type-1",
                        "name": "Type 1",
                        "displayName": "Type 1",
                        "description": "desc",
                        "deviceTypeParamValues": {},
                        "devices": {}
                      }
                    }
                  }
                }
                """;

        assertThatThrownBy(() -> parser.parse(json))
                .isInstanceOf(DttFormatException.class)
                .hasMessageContaining("Некорректный формат поля type");
    }
}
