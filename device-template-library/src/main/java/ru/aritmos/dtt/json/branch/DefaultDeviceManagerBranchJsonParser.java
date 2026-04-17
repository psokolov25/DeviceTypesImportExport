package ru.aritmos.dtt.json.branch;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import ru.aritmos.dtt.api.dto.DeviceTypeMetadata;
import ru.aritmos.dtt.api.dto.DeviceTypeTemplate;
import ru.aritmos.dtt.exception.DttFormatException;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Реализация парсера branch equipment JSON через Jackson.
 */
public class DefaultDeviceManagerBranchJsonParser implements DeviceManagerBranchJsonParser {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public BranchEquipment parse(String json) {
        try {
            final JsonNode root = objectMapper.readTree(json);
            final Map<String, BranchNode> branches = new LinkedHashMap<>();
            root.fields().forEachRemaining(branchEntry -> {
                final String branchId = branchEntry.getKey();
                final JsonNode branchNode = branchEntry.getValue();
                final String resolvedBranchId = branchNode.path("id").asText(branchId);
                final String displayName = branchNode.path("displayName").asText(resolvedBranchId);
                final Map<String, BranchDeviceType> deviceTypes = new LinkedHashMap<>();
                final JsonNode deviceTypesNode = branchNode.path("deviceTypes");
                deviceTypesNode.fields().forEachRemaining(typeEntry -> {
                    final String typeId = typeEntry.getKey();
                    final JsonNode typeNode = typeEntry.getValue();
                    final DeviceTypeMetadata metadata = new DeviceTypeMetadata(
                            typeNode.path("id").asText(typeId),
                            typeNode.path("name").asText(typeId),
                            typeNode.path("displayName").asText(typeNode.path("name").asText(typeId)),
                            typeNode.path("description").asText("")
                    );
                    final Map<String, Object> values = objectMapper.convertValue(
                            typeNode.path("deviceTypeParamValues"),
                            new TypeReference<Map<String, Object>>() { }
                    );
                    final DeviceTypeTemplate template = new DeviceTypeTemplate(metadata, values == null ? Map.of() : values);
                    final Map<String, DeviceInstanceTemplate> devices = objectMapper.convertValue(
                            typeNode.path("devices"),
                            new TypeReference<Map<String, DeviceInstanceTemplate>>() { }
                    );
                    deviceTypes.put(typeId, new BranchDeviceType(template, devices == null ? Map.of() : devices));
                });
                branches.put(branchId, new BranchNode(resolvedBranchId, displayName, deviceTypes));
            });
            return new BranchEquipment(branches);
        } catch (JsonProcessingException exception) {
            throw new DttFormatException("Ошибка парсинга branch equipment JSON", exception);
        }
    }
}
