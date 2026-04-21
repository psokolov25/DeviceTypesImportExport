package ru.aritmos.dtt.demo.openapi;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.BooleanNode;
import com.fasterxml.jackson.databind.node.MissingNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Iterator;
import java.util.Map;

/**
 * Постпроцессор OpenAPI-спеки demo-service.
 *
 * <p>Часть JSON-примеров Micronaut OpenAPI сериализует как строковые scalar-блоки.
 * Этот постпроцессор переводит такие примеры обратно в JSON-объекты для application/json,
 * чинит схемы JsonNode-полей и подставляет нормальные JSON-строки в multipart metadataJson.
 */
public final class OpenApiExamplesPostProcessor {

    private static final ObjectMapper YAML = new ObjectMapper(new YAMLFactory());
    private static final ObjectMapper JSON = new ObjectMapper();

    private OpenApiExamplesPostProcessor() {
    }

    public static void main(String[] args) throws IOException {
        final Path specPath = resolveSpecPath(args);
        if (!Files.exists(specPath)) {
            return;
        }

        final ObjectNode root = (ObjectNode) YAML.readTree(Files.readString(specPath));
        patchJsonContentExamples(root);
        patchKnownRequestExamples(root);
        patchMultipartStringExamples(root);
        patchJsonNodeSchemas(root);
        YAML.writerWithDefaultPrettyPrinter().writeValue(specPath.toFile(), root);
    }

    private static Path resolveSpecPath(String[] args) {
        if (args != null && args.length > 0 && args[0] != null && !args[0].isBlank()) {
            return Paths.get(args[0]);
        }
        return Paths.get("target/classes/META-INF/swagger/device-template-demo.yml");
    }

    private static void patchJsonContentExamples(ObjectNode root) {
        final JsonNode pathsNode = root.path("paths");
        if (!pathsNode.isObject()) {
            return;
        }
        final Iterator<Map.Entry<String, JsonNode>> pathIterator = pathsNode.fields();
        while (pathIterator.hasNext()) {
            final Map.Entry<String, JsonNode> pathEntry = pathIterator.next();
            final JsonNode operationsNode = pathEntry.getValue();
            if (!operationsNode.isObject()) {
                continue;
            }
            final Iterator<Map.Entry<String, JsonNode>> operationIterator = operationsNode.fields();
            while (operationIterator.hasNext()) {
                final JsonNode operationNode = operationIterator.next().getValue();
                patchJsonExamplesInContent(nodeAt(operationNode, "requestBody", "content", "application/json"));
                final JsonNode responsesNode = operationNode.path("responses");
                if (responsesNode.isObject()) {
                    final Iterator<Map.Entry<String, JsonNode>> responseIterator = responsesNode.fields();
                    while (responseIterator.hasNext()) {
                        patchJsonExamplesInContent(nodeAt(responseIterator.next().getValue(), "content", "application/json"));
                    }
                }
            }
        }
    }

    private static void patchJsonExamplesInContent(JsonNode contentNode) {
        if (!(contentNode instanceof ObjectNode objectNode)) {
            return;
        }
        final JsonNode examplesNode = objectNode.path("examples");
        if (examplesNode.isObject()) {
            final Iterator<Map.Entry<String, JsonNode>> iterator = examplesNode.fields();
            while (iterator.hasNext()) {
                final JsonNode exampleNode = iterator.next().getValue();
                if (exampleNode instanceof ObjectNode exampleObject) {
                    final JsonNode valueNode = exampleObject.get("value");
                    if (valueNode != null && valueNode.isTextual()) {
                        final JsonNode parsed = tryParseJsonText(valueNode.asText());
                        if (parsed != null) {
                            exampleObject.set("value", parsed);
                        }
                    }
                }
            }
        }
        final JsonNode exampleNode = objectNode.get("example");
        if (exampleNode != null && exampleNode.isTextual()) {
            final JsonNode parsed = tryParseJsonText(exampleNode.asText());
            if (parsed != null) {
                objectNode.set("example", parsed);
            }
        }
    }

    private static JsonNode tryParseJsonText(String raw) {
        if (raw == null) {
            return null;
        }
        final String trimmed = raw.trim();
        if (trimmed.isEmpty() || (!trimmed.startsWith("{") && !trimmed.startsWith("["))) {
            return null;
        }
        try {
            return JSON.readTree(trimmed);
        } catch (Exception ignore) {
            try {
                return JSON.readTree(sanitizeInvalidEscapes(trimmed));
            } catch (Exception ignoredAgain) {
                return null;
            }
        }
    }

    private static String sanitizeInvalidEscapes(String raw) {
        final StringBuilder builder = new StringBuilder(raw.length() + 16);
        for (int index = 0; index < raw.length(); index++) {
            final char current = raw.charAt(index);
            if (current != '\\') {
                builder.append(current);
                continue;
            }
            if (index + 1 >= raw.length()) {
                builder.append("\\");
                continue;
            }
            final char next = raw.charAt(index + 1);
            if (next == '"' || next == '\\' || next == '/' || next == 'b' || next == 'f'
                    || next == 'n' || next == 'r' || next == 't' || next == 'u') {
                builder.append(current);
            } else {
                builder.append("\\\\");
            }
        }
        return builder.toString();
    }

    private static void patchKnownRequestExamples(ObjectNode root) throws IOException {
        setRequestExample(root, "/api/dtt/export/branch/all", "autoResolveMostComplete", buildExportAllBranchExample(true));
        setRequestExample(root, "/api/dtt/export/branch/all", "failIfExists", buildExportAllBranchExample(false));
        setRequestExample(root, "/api/dtt/export/branch/all", "jsonStringFiltered", buildExportAllBranchFilteredExample());

        setRequestExample(root, "/api/dtt/export/branch/all/download", "autoResolveMostComplete", buildExportAllBranchExample(true));
        setRequestExample(root, "/api/dtt/export/branch/all/download", "failIfExists", buildExportAllBranchExample(false));
        setRequestExample(root, "/api/dtt/export/branch/all/download", "jsonStringFiltered", buildExportAllBranchFilteredExample());

        setRequestExample(root, "/api/dtt/import/branch/merge", "example", buildImportBranchMergeExample());
    }

    private static void setRequestExample(ObjectNode root, String path, String exampleName, JsonNode value) {
        final JsonNode node = nodeAt(root, "paths", path, "post", "requestBody", "content", "application/json", "examples", exampleName);
        if (node instanceof ObjectNode exampleNode) {
            exampleNode.set("value", value);
        }
    }

    private static JsonNode buildExportAllBranchExample(boolean autoResolveMostComplete) {
        final ObjectNode request = JSON.createObjectNode();
        final ObjectNode branchEquipment = request.putObject("branchEquipment");
        final ObjectNode branches = branchEquipment.putObject("branches");

        final ObjectNode firstBranch = branches.putObject("ec8d252d-deb9-4ebb-accf-0ef7994bf17b");
        firstBranch.put("id", "ec8d252d-deb9-4ebb-accf-0ef7994bf17b");
        firstBranch.put("displayName", "test kate");
        firstBranch.put("prefix", "KAT");
        final ObjectNode firstBranchTypes = firstBranch.putObject("deviceTypes");
        final ObjectNode reception = firstBranchTypes.putObject("ffde364f-5f5a-45e6-86b7-1215a28ae96c");
        reception.put("id", "ffde364f-5f5a-45e6-86b7-1215a28ae96c");
        reception.put("name", "Reception");
        reception.put("displayName", "Приёмная");
        reception.put("description", "Приёмная");
        reception.put("type", "reception");
        final ObjectNode receptionValues = reception.putObject("deviceTypeParamValues");
        receptionValues.putObject("multiService").put("value", true);
        receptionValues.putObject("showQueues").put("value", true);
        final ObjectNode receptionDevices = reception.putObject("devices");
        final ObjectNode receptionDevice = receptionDevices.putObject("14124427-9723-4faf-9a67-105dd4431a16");
        receptionDevice.put("id", "14124427-9723-4faf-9a67-105dd4431a16");
        receptionDevice.put("name", "reception 1");
        receptionDevice.put("displayName", "reception 1");
        final ObjectNode onPublicFinishEvent = reception.putObject("onPublicFinishEvent");
        onPublicFinishEvent.putObject("inputParameters");
        onPublicFinishEvent.putArray("outputParameters");

        final ObjectNode secondBranch = branches.putObject("37493d1c-8282-4417-a729-dceac1f3e2b4");
        secondBranch.put("id", "37493d1c-8282-4417-a729-dceac1f3e2b4");
        secondBranch.put("displayName", "Отделение на Тверской");
        final ObjectNode secondBranchTypes = secondBranch.putObject("deviceTypes");
        final ObjectNode secondReception = secondBranchTypes.putObject("ffde364f-5f5a-45e6-86b7-1215a28ae96c");
        secondReception.put("id", "ffde364f-5f5a-45e6-86b7-1215a28ae96c");
        secondReception.put("name", "Reception");
        secondReception.put("displayName", "Reception");
        secondReception.put("description", "Устройство озвучивания");
        secondReception.put("type", "reception");
        final ObjectNode secondValues = secondReception.putObject("deviceTypeParamValues");
        secondValues.putObject("phrase").put("value", "\\eFПосетитель с номером{ticketId}");
        secondValues.putObject("URL").put("value", "http://192.168.1.8:8080/unnamed/rest/play");
        secondReception.putObject("devices");

        final ArrayNode branchIds = request.putArray("branchIds");
        branchIds.add("ec8d252d-deb9-4ebb-accf-0ef7994bf17b");
        branchIds.add("37493d1c-8282-4417-a729-dceac1f3e2b4");
        request.put("mergeStrategy", autoResolveMostComplete ? "MERGE_NON_NULLS" : "FAIL_IF_EXISTS");
        request.put("dttVersion", "2.1.0");
        return request;
    }

    private static JsonNode buildExportAllBranchFilteredExample() {
        final ObjectNode request = JSON.createObjectNode();
        final ObjectNode branchJson = request.putObject("branchJson");
        final ObjectNode branch = branchJson.putObject("ec8d252d-deb9-4ebb-accf-0ef7994bf17b");
        branch.put("id", "ec8d252d-deb9-4ebb-accf-0ef7994bf17b");
        branch.put("displayName", "test kate");
        branch.put("prefix", "KAT");
        final ObjectNode deviceTypes = branch.putObject("deviceTypes");
        final ObjectNode terminal = deviceTypes.putObject("ed650d7d-6201-42fb-a4c3-b9efb93dda0c");
        terminal.put("id", "ed650d7d-6201-42fb-a4c3-b9efb93dda0c");
        terminal.put("name", "Terminal");
        terminal.put("displayName", "Терминал (Киоск)");
        terminal.put("description", "Терминал (Киоск)");
        terminal.put("type", "entry_point");
        final ObjectNode terminalValues = terminal.putObject("deviceTypeParamValues");
        terminalValues.putObject("printerServiceURL").put("value", "http://192.168.7.20:8084");
        terminalValues.putObject("prefix").put("value", "SSS");
        terminalValues.putObject("translatorURL").put("value", "http://192.168.7.20:8104?prefix=SSS");
        terminalValues.putObject("templateID").put("value", "90ef9bba-e5b6-4d5f-a928-3bf9c7a9e297");
        terminalValues.putObject("translatorTicket").put("value", "http://192.168.7.20:8114/printing");
        final ObjectNode devices = terminal.putObject("devices");
        final ObjectNode terminalDevice = devices.putObject("811fb688-546f-495d-be86-58a63c5d560d");
        terminalDevice.put("id", "811fb688-546f-495d-be86-58a63c5d560d");
        terminalDevice.put("name", "entry_point 1");
        terminalDevice.put("displayName", "entry_point 1");
        request.putArray("branchIds").add("ec8d252d-deb9-4ebb-accf-0ef7994bf17b");
        request.putArray("deviceTypeIds").add("ed650d7d-6201-42fb-a4c3-b9efb93dda0c");
        request.put("mergeStrategy", "MERGE_NON_NULLS");
        request.put("dttVersion", "2.1.0");
        return request;
    }

    private static JsonNode buildImportBranchMergeExample() throws IOException {
        final ObjectNode existing = JSON.createObjectNode();
        final ObjectNode existingBranch = existing.putObject("ec8d252d-deb9-4ebb-accf-0ef7994bf17b");
        existingBranch.put("id", "ec8d252d-deb9-4ebb-accf-0ef7994bf17b");
        existingBranch.put("displayName", "test kate");
        final ObjectNode existingTypes = existingBranch.putObject("deviceTypes");
        final ObjectNode terminal = existingTypes.putObject("terminal");
        terminal.put("id", "terminal");
        terminal.put("name", "Terminal");
        terminal.putObject("deviceTypeParamValues").putObject("prefix").put("value", "KAT");
        terminal.putObject("devices");

        final ObjectNode request = JSON.createObjectNode();
        request.put("existingBranchJson", JSON.writerWithDefaultPrettyPrinter().writeValueAsString(existing));
        request.put("mergeStrategy", "MERGE_NON_NULLS");
        final ArrayNode branches = request.putArray("branches");
        final ObjectNode branch = branches.addObject();
        branch.put("branchId", "ec8d252d-deb9-4ebb-accf-0ef7994bf17b");
        branch.put("displayName", "test kate");
        final ArrayNode deviceTypes = branch.putArray("deviceTypes");
        final ObjectNode display = deviceTypes.addObject();
        display.put("archiveBase64", DttSwaggerExamples.SAMPLE_DTT_DISPLAY_BASE64);
        display.put("kind", "display");
        display.putObject("deviceTypeParamValues").put("TicketZone", "9");
        final ArrayNode devices = display.putArray("devices");
        final ObjectNode device = devices.addObject();
        device.put("id", "display-1");
        device.put("name", "display-1");
        device.put("displayName", "Display 1");
        device.putObject("deviceParamValues").put("IP", "10.10.10.10").put("Port", 22224);
        return request;
    }

    private static void patchMultipartStringExamples(ObjectNode root) throws IOException {
        setMultipartMetadataExample(root, "/api/dtt/import/profile/upload/multipart", buildProfileUploadMetadata());
        setMultipartMetadataExample(root, "/api/dtt/preview/profile/upload/multipart", buildProfileUploadMetadata());
        setMultipartMetadataExample(root, "/api/dtt/import/branch/upload/multipart", buildBranchUploadMetadata());
        setMultipartMetadataExample(root, "/api/dtt/preview/branch/upload/multipart", buildBranchUploadMetadata());
        setMultipartMetadataExample(root, "/api/dtt/import/branch/merge/upload/multipart", buildExistingBranchUploadMetadata());
    }

    private static void setMultipartMetadataExample(ObjectNode root, String path, String example) {
        final JsonNode node = nodeAt(root, "paths", path, "post", "requestBody", "content", "multipart/form-data", "schema", "properties", "metadataJson");
        if (node instanceof ObjectNode propertyNode) {
            propertyNode.put("example", example);
        }
    }

    private static String buildProfileUploadMetadata() throws IOException {
        final ObjectNode root = JSON.createObjectNode();
        root.put("mergeStrategy", "FAIL_IF_EXISTS");
        final ArrayNode deviceTypes = root.putArray("deviceTypes");
        final ObjectNode terminal = deviceTypes.addObject();
        terminal.put("archiveEntryName", "Terminal.dtt");
        terminal.putObject("deviceTypeParamValues")
                .put("prefix", "OVR")
                .put("printerServiceURL", "http://10.10.10.10:8084");
        return JSON.writerWithDefaultPrettyPrinter().writeValueAsString(root);
    }

    private static String buildBranchUploadMetadata() throws IOException {
        final ObjectNode root = JSON.createObjectNode();
        root.put("mergeStrategy", "FAIL_IF_EXISTS");
        final ArrayNode branches = root.putArray("branches");
        final ObjectNode branch = branches.addObject();
        branch.put("branchId", "branch-custom");
        branch.put("displayName", "Отделение custom");
        final ArrayNode deviceTypes = branch.putArray("deviceTypes");
        final ObjectNode display = deviceTypes.addObject();
        display.put("archiveEntryName", "Display WD3264.dtt");
        display.put("kind", "display");
        display.putObject("deviceTypeParamValues")
                .put("TicketZone", "9")
                .put("ServicePointNameZone", "1");
        final ObjectNode device = display.putArray("devices").addObject();
        device.put("id", "display-1");
        device.put("name", "display-1");
        device.put("displayName", "Display 1");
        device.put("description", "Demo display");
        device.putObject("deviceParamValues")
                .put("IP", "10.10.10.10")
                .put("Port", 22224)
                .put("ServicePointDisplayName", "OKHO 1");
        final ObjectNode terminal = deviceTypes.addObject();
        terminal.put("archiveEntryName", "Terminal.dtt");
        terminal.putObject("deviceTypeParamValues")
                .put("prefix", "OVR")
                .put("printerServiceURL", "http://10.10.10.10:8084");
        return JSON.writerWithDefaultPrettyPrinter().writeValueAsString(root);
    }

    private static String buildExistingBranchUploadMetadata() throws IOException {
        final ObjectNode existing = JSON.createObjectNode();
        final ObjectNode branchJson = existing.putObject("branch-custom");
        branchJson.put("id", "branch-custom");
        branchJson.put("displayName", "Отделение custom");
        branchJson.putObject("deviceTypes");

        final ObjectNode root = JSON.createObjectNode();
        root.put("existingBranchJson", JSON.writerWithDefaultPrettyPrinter().writeValueAsString(existing));
        root.put("mergeStrategy", "MERGE_NON_NULLS");
        final ArrayNode branches = root.putArray("branches");
        final ObjectNode branch = branches.addObject();
        branch.put("branchId", "branch-custom");
        branch.put("displayName", "Отделение custom");
        final ArrayNode deviceTypes = branch.putArray("deviceTypes");
        final ObjectNode display = deviceTypes.addObject();
        display.put("archiveEntryName", "Display WD3264.dtt");
        display.putObject("deviceTypeParamValues").put("TicketZone", "9");
        final ObjectNode device = display.putArray("devices").addObject();
        device.put("id", "display-1");
        device.put("name", "display-1");
        device.put("displayName", "Display 1");
        device.putObject("deviceParamValues").put("IP", "10.10.10.10").put("Port", 22224);
        return JSON.writerWithDefaultPrettyPrinter().writeValueAsString(root);
    }

    private static void patchJsonNodeSchemas(ObjectNode root) {
        forceObjectSchema(root, "ExportAllDttFromBranchRequest", "branchJson", "Branch equipment JSON как объект (альтернативно branchEquipment)");
        forceObjectSchema(root, "ExportSingleDttFromBranchRequest", "branchJson", "Branch equipment JSON как объект (альтернатива branchEquipment)");
        forceObjectSchema(root, "ExportAllDttFromProfileRequest", "profileJson", "Profile JSON как объект (альтернативно profile)");
        forceObjectSchema(root, "ExportSingleDttFromProfileRequest", "profileJson", "Profile JSON как объект (альтернатива profile)");
        forceObjectSchema(root, "ImportDttSetToBranchResponse", "branchJson", "Собранный branch equipment JSON (карта branchId -> branch node) как объект");
        forceObjectSchema(root, "ImportDttSetToProfileResponse", "profileJson", "Собранный JSON карты deviceTypes как объект");
    }

    private static void forceObjectSchema(ObjectNode root, String schemaName, String propertyName, String description) {
        final JsonNode node = nodeAt(root, "components", "schemas", schemaName, "properties", propertyName);
        if (node instanceof ObjectNode propertyNode) {
            propertyNode.removeAll();
            propertyNode.put("type", "object");
            propertyNode.put("description", description);
            propertyNode.set("additionalProperties", BooleanNode.TRUE);
        }
    }

    private static JsonNode nodeAt(JsonNode node, String... path) {
        JsonNode current = node;
        for (String part : path) {
            if (current == null || current.isMissingNode()) {
                return MissingNode.getInstance();
            }
            current = current.path(part);
        }
        return current;
    }
}
