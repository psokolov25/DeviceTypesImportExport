package ru.aritmos.dtt.json.branch;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import ru.aritmos.dtt.api.dto.DeviceTypeMetadata;
import ru.aritmos.dtt.api.dto.DeviceTypeTemplate;
import ru.aritmos.dtt.exception.DttFormatException;
import ru.aritmos.dtt.archive.DttIconSupport;

import java.util.LinkedHashMap;
import java.util.List;
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
            final List<BranchDeviceTypeMetadata> metadataList = objectMapper.convertValue(
                    root.path("metadata"),
                    new TypeReference<List<BranchDeviceTypeMetadata>>() { }
            );
            root.fields().forEachRemaining(branchEntry -> {
                final String branchId = branchEntry.getKey();
                if ("metadata".equals(branchId)) {
                    return;
                }
                final JsonNode branchNode = branchEntry.getValue();
                final String resolvedBranchId = branchNode.path("id").asText(branchId);
                final String displayName = branchNode.path("displayName").asText(resolvedBranchId);
                final Map<String, BranchDeviceType> deviceTypes = new LinkedHashMap<>();
                final JsonNode deviceTypesNode = branchNode.path("deviceTypes");
                deviceTypesNode.fields().forEachRemaining(typeEntry -> {
                    final String typeId = typeEntry.getKey();
                    final JsonNode typeNode = typeEntry.getValue();
                    final DeviceTypeMetadata typeMetadata = new DeviceTypeMetadata(
                            typeNode.path("id").asText(typeId),
                            typeNode.path("name").asText(typeId),
                            typeNode.path("displayName").asText(typeNode.path("name").asText(typeId)),
                            typeNode.path("description").asText(""),
                            typeNode.path("version").isNull() ? null : typeNode.path("version").asText(null),
                            DttIconSupport.resolveOrDefault(typeNode.path("imageBase64").asText(null))
                    );
                    final Map<String, Object> values = objectMapper.convertValue(
                            typeNode.path("deviceTypeParamValues"),
                            new TypeReference<Map<String, Object>>() { }
                    );
                    final DeviceTypeTemplate template = new DeviceTypeTemplate(typeMetadata, values == null ? Map.of() : values);
                    final Map<String, DeviceInstanceTemplate> devices = objectMapper.convertValue(
                            typeNode.path("devices"),
                            new TypeReference<Map<String, DeviceInstanceTemplate>>() { }
                    );
                    final Map<String, BranchScript> eventHandlers = objectMapper.convertValue(
                            typeNode.path("eventHandlers"),
                            new TypeReference<Map<String, BranchScript>>() { }
                    );
                    final Map<String, BranchScript> commands = objectMapper.convertValue(
                            typeNode.path("commands"),
                            new TypeReference<Map<String, BranchScript>>() { }
                    );
                    final BranchScript onStartEvent = toBranchScript(typeNode.path("onStartEvent"));
                    final BranchScript onStopEvent = toBranchScript(typeNode.path("onStopEvent"));
                    final BranchScript onPublicStartEvent = toBranchScript(typeNode.path("onPublicStartEvent"));
                    final BranchScript onPublicFinishEvent = toBranchScript(typeNode.path("onPublicFinishEvent"));
                    final String deviceTypeFunctions = readDeviceTypeFunctions(typeNode.path("deviceTypeFunctions"));
                    final JsonNode kindNode = typeNode.path("type");
                    if (kindNode.isMissingNode() || kindNode.isNull() || !kindNode.isTextual() || kindNode.asText().isBlank()) {
                        throw new DttFormatException("Некорректный формат поля type: ожидается непустая строка");
                    }
                    final String kind = kindNode.asText();
                    deviceTypes.put(typeId, new BranchDeviceType(
                            template,
                            devices == null ? Map.of() : devices,
                            kind,
                            onStartEvent,
                            onStopEvent,
                            onPublicStartEvent,
                            onPublicFinishEvent,
                            deviceTypeFunctions,
                            eventHandlers == null ? Map.of() : eventHandlers,
                            commands == null ? Map.of() : commands
                    ));
                });
                branches.put(branchId, new BranchNode(resolvedBranchId, displayName, deviceTypes));
            });
            return new BranchEquipment(branches, metadataList == null ? List.of() : metadataList);
        } catch (JsonProcessingException exception) {
            throw new DttFormatException("Ошибка парсинга branch equipment JSON", exception);
        }
    }

    private String readDeviceTypeFunctions(JsonNode node) {
        if (node == null || node.isMissingNode() || node.isNull()) {
            return null;
        }
        if (node.isTextual()) {
            return node.asText();
        }
        throw new DttFormatException("Некорректный формат deviceTypeFunctions: ожидается строка или null");
    }

    private BranchScript toBranchScript(JsonNode node) {
        if (node == null || node.isMissingNode() || node.isNull()) {
            return null;
        }
        if (!node.isObject()) {
            throw new DttFormatException("Некорректная script-секция branch JSON: ожидается объект");
        }
        final Map<String, Object> inputParameters = objectMapper.convertValue(
                node.path("inputParameters"),
                new TypeReference<Map<String, Object>>() { }
        );
        final List<Object> outputParameters = objectMapper.convertValue(
                node.path("outputParameters"),
                new TypeReference<List<Object>>() { }
        );
        final String scriptCode = node.path("scriptCode").isMissingNode() ? null : node.path("scriptCode").asText(null);
        return new BranchScript(
                inputParameters == null ? Map.of() : inputParameters,
                outputParameters == null ? List.of() : outputParameters,
                scriptCode
        );
    }
}
