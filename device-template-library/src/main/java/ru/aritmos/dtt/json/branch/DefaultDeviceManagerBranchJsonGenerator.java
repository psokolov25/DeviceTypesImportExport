package ru.aritmos.dtt.json.branch;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import ru.aritmos.dtt.exception.DttFormatException;

/**
 * Реализация генератора branch equipment JSON через Jackson.
 */
public class DefaultDeviceManagerBranchJsonGenerator implements DeviceManagerBranchJsonGenerator {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public String generate(BranchEquipment branchEquipment) {
        try {
            final ObjectNode root = objectMapper.createObjectNode();
            branchEquipment.branches().forEach((branchId, branchNode) -> root.set(branchId, toCanonicalBranchNode(branchNode)));
            return objectMapper.writeValueAsString(root);
        } catch (JsonProcessingException exception) {
            throw new DttFormatException("Ошибка генерации branch equipment JSON", exception);
        }
    }

    private JsonNode toCanonicalBranchNode(BranchNode branchNode) {
        final ObjectNode branch = objectMapper.createObjectNode();
        branch.put("id", branchNode.id());
        branch.put("displayName", branchNode.displayName());

        final ObjectNode deviceTypes = objectMapper.createObjectNode();
        branchNode.deviceTypes().forEach((typeId, branchDeviceType) -> {
            final ObjectNode typeNode = objectMapper.createObjectNode();
            typeNode.put("id", branchDeviceType.template().metadata().id());
            typeNode.put("name", branchDeviceType.template().metadata().name());
            typeNode.put("description", branchDeviceType.template().metadata().description());
            typeNode.put("displayName", branchDeviceType.template().metadata().displayName());
            typeNode.set("deviceTypeParamValues", objectMapper.valueToTree(toCanonicalParameterValues(branchDeviceType.template().deviceTypeParamValues())));
            typeNode.set("devices", objectMapper.valueToTree(branchDeviceType.devices() == null ? java.util.Map.of() : branchDeviceType.devices()));
            deviceTypes.set(typeId, typeNode);
        });
        branch.set("deviceTypes", deviceTypes);
        return branch;
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
