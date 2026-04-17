package ru.aritmos.dtt.json.branch;

import org.junit.jupiter.api.Test;
import ru.aritmos.dtt.api.dto.DeviceTypeMetadata;
import ru.aritmos.dtt.api.dto.DeviceTypeTemplate;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

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
                                        Map.of("dev-1", new DeviceInstanceTemplate("dev-1", "n", "d", "desc", Map.of("ip", "1")))
                                )
                        )
                )
        ));

        final String json = generator.generate(branchEquipment);
        final BranchEquipment parsed = parser.parse(json);

        assertThat(parsed.branches()).containsKey("branch-1");
        assertThat(parsed.branches().get("branch-1").deviceTypes()).containsKey("type-1");
    }
}
