package ru.aritmos.dtt.json.profile;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import ru.aritmos.dtt.api.dto.DeviceTypeTemplate;
import ru.aritmos.dtt.archive.DttIconSupport;
import ru.aritmos.dtt.exception.DttFormatException;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Реализация генератора JSON профиля оборудования.
 */
public class DefaultEquipmentProfileJsonGenerator implements EquipmentProfileJsonGenerator {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public String generate(EquipmentProfile profile) {
        try {
            final ObjectNode root = objectMapper.createObjectNode();
            root.set("metadata", objectMapper.valueToTree(buildMetadata(profile)));
            profile.deviceTypes().forEach((typeId, template) -> root.set(typeId, toCanonicalDeviceTypeNode(template)));
            return objectMapper.writeValueAsString(root);
        } catch (JsonProcessingException exception) {
            throw new DttFormatException("Ошибка генерации profile JSON", exception);
        }
    }

    private List<ProfileDeviceTypeMetadata> buildMetadata(EquipmentProfile profile) {
        final Map<String, ProfileDeviceTypeMetadata> metadata = new LinkedHashMap<>();
        profile.deviceTypes().forEach((typeId, template) -> metadata.putIfAbsent(
                typeId,
                new ProfileDeviceTypeMetadata(
                        typeId,
                        template.metadata().name(),
                        template.metadata().displayName(),
                        template.metadata().version(),
                        template.metadata().description(),
                        DttIconSupport.resolveOrDefault(template.metadata().iconBase64())
                )
        ));
        return List.copyOf(metadata.values());
    }

    private JsonNode toCanonicalDeviceTypeNode(DeviceTypeTemplate template) {
        final ObjectNode node = objectMapper.createObjectNode();
        node.put("id", template.metadata().id());
        node.put("name", template.metadata().name());
        node.put("description", template.metadata().description());
        node.put("displayName", template.metadata().displayName());
        if (template.metadata().version() == null) {
            node.putNull("version");
        } else {
            node.put("version", template.metadata().version());
        }
        node.put("imageBase64", DttIconSupport.resolveOrDefault(template.metadata().iconBase64()));
        node.set("deviceTypeParamValues", objectMapper.valueToTree(toCanonicalParameterValues(template.deviceTypeParamValues())));
        return node;
    }

    private Map<String, Object> toCanonicalParameterValues(Map<String, Object> values) {
        if (values == null || values.isEmpty()) {
            return Map.of();
        }
        final Map<String, Object> canonical = new LinkedHashMap<>();
        values.forEach((key, value) -> canonical.put(key, toCanonicalParameterValue(key, value)));
        return canonical;
    }

    private Object toCanonicalParameterValue(String key, Object value) {
        if (value instanceof Map<?, ?> mapValue && mapValue.containsKey("value")) {
            return value;
        }
        final Map<String, Object> wrapped = new LinkedHashMap<>();
        wrapped.put("value", value);
        wrapped.put("name", key);
        return wrapped;
    }
}
