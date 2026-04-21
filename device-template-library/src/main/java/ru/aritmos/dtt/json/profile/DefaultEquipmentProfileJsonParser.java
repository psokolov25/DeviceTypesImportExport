package ru.aritmos.dtt.json.profile;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import ru.aritmos.dtt.api.dto.DeviceTypeMetadata;
import ru.aritmos.dtt.api.dto.DeviceTypeTemplate;
import ru.aritmos.dtt.exception.DttFormatException;
import ru.aritmos.dtt.archive.DttIconSupport;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Реализация парсера profile JSON в типизированную модель.
 */
public class DefaultEquipmentProfileJsonParser implements EquipmentProfileJsonParser {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public EquipmentProfile parse(String json) {
        try {
            final JsonNode root = objectMapper.readTree(json);
            final Map<String, DeviceTypeTemplate> deviceTypes = new LinkedHashMap<>();
            root.fields().forEachRemaining(entry -> {
                final String typeId = entry.getKey();
                final JsonNode node = entry.getValue();
                final DeviceTypeMetadata metadata = new DeviceTypeMetadata(
                        node.path("id").asText(typeId),
                        node.path("name").asText(typeId),
                        node.path("displayName").asText(node.path("name").asText(typeId)),
                        node.path("description").asText(""),
                        node.path("version").isNull() ? null : node.path("version").asText(null),
                        DttIconSupport.resolveOrDefault(node.path("imageBase64").asText(null))
                );
                final Map<String, Object> values = objectMapper.convertValue(
                        node.path("deviceTypeParamValues"),
                        new com.fasterxml.jackson.core.type.TypeReference<Map<String, Object>>() { }
                );
                deviceTypes.put(typeId, new DeviceTypeTemplate(metadata, values == null ? Map.of() : values));
            });
            return new EquipmentProfile(deviceTypes);
        } catch (JsonProcessingException exception) {
            throw new DttFormatException("Ошибка парсинга profile JSON", exception);
        }
    }
}
