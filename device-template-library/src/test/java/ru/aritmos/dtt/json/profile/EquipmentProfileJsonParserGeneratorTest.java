package ru.aritmos.dtt.json.profile;

import org.junit.jupiter.api.Test;
import ru.aritmos.dtt.api.dto.DeviceTypeMetadata;
import ru.aritmos.dtt.api.dto.DeviceTypeTemplate;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class EquipmentProfileJsonParserGeneratorTest {

    private final EquipmentProfileJsonGenerator generator = new DefaultEquipmentProfileJsonGenerator();
    private final EquipmentProfileJsonParser parser = new DefaultEquipmentProfileJsonParser();

    @Test
    void shouldGenerateAndParseProfileJson() {
        final EquipmentProfile profile = new EquipmentProfile(Map.of(
                "type-1",
                new DeviceTypeTemplate(new DeviceTypeMetadata("type-1", "n", "d", "desc"), Map.of("ip", "1"))
        ));

        final String json = generator.generate(profile);
        final EquipmentProfile parsed = parser.parse(json);

        assertThat(parsed.deviceTypes()).containsKey("type-1");
        assertThat(parsed.deviceTypes().get("type-1").deviceTypeParamValues()).containsEntry("ip", "1");
    }
}
