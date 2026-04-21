package ru.aritmos.dtt.json.profile;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import ru.aritmos.dtt.api.dto.DeviceTypeTemplate;
import ru.aritmos.dtt.archive.DttIconSupport;
import ru.aritmos.dtt.exception.DttFormatException;

/**
 * Реализация генератора JSON профиля оборудования.
 */
public class DefaultEquipmentProfileJsonGenerator implements EquipmentProfileJsonGenerator {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public String generate(EquipmentProfile profile) {
        try {
            final ObjectNode root = objectMapper.createObjectNode();
            profile.deviceTypes().forEach((typeId, template) -> root.set(typeId, toCanonicalDeviceTypeNode(template)));
            return objectMapper.writeValueAsString(root);
        } catch (JsonProcessingException exception) {
            throw new DttFormatException("Ошибка генерации profile JSON", exception);
        }
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

    private java.util.Map<String, Object> toCanonicalParameterValues(java.util.Map<String, Object> values) {
        if (values == null || values.isEmpty()) {
            return java.util.Map.of();
        }
        final java.util.Map<String, Object> canonical = new java.util.LinkedHashMap<>();
        values.forEach((key, value) -> canonical.put(key, toCanonicalParameterValue(key, value)));
        return canonical;
    }

    private Object toCanonicalParameterValue(String key, Object value) {
        if (value instanceof java.util.Map<?, ?> mapValue && mapValue.containsKey("value")) {
            return value;
        }
        final java.util.Map<String, Object> wrapped = new java.util.LinkedHashMap<>();
        wrapped.put("value", value);
        wrapped.put("name", key);
        return wrapped;
    }
}
