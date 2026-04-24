package ru.aritmos.dtt.demo.openapi;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class DttSwaggerExamplesTest {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    @Test
    void shouldKeepProfileAndBranchJsonExamplesAsObjects() throws Exception {
        assertObjectField(DttSwaggerExamples.PROFILE_EXPORT_SINGLE_OBJECT, "profileJson");
        assertObjectField(DttSwaggerExamples.BRANCH_EXPORT_SINGLE_OBJECT, "branchEquipment");
        assertObjectField(DttSwaggerExamples.BRANCH_EXPORT_JSON_STRING_FILTERED, "branchJson");
    }

    @Test
    void shouldKeepImportExamplesAsObjectBodies() throws Exception {
        assertThat(OBJECT_MAPPER.readTree(DttSwaggerExamples.IMPORT_PROFILE_REQUEST_STRUCTURED_FULL).isObject()).isTrue();
        assertThat(OBJECT_MAPPER.readTree(DttSwaggerExamples.IMPORT_BRANCH_REQUEST_STRUCTURED_FULL).isObject()).isTrue();
        assertThat(OBJECT_MAPPER.readTree(DttSwaggerExamples.PROFILE_EXPORT_ALL_OBJECT).isObject()).isTrue();
        assertThat(OBJECT_MAPPER.readTree(DttSwaggerExamples.BRANCH_EXPORT_OBJECT_AUTO_RESOLVE).isObject()).isTrue();
    }

    @Test
    void shouldContainImageBase64InExportMetadataExamples() throws Exception {
        final JsonNode profileAll = OBJECT_MAPPER.readTree(DttSwaggerExamples.PROFILE_EXPORT_ALL_OBJECT);
        assertThat(profileAll.at("/profileJson/metadata/0/imageBase64").asText()).isNotBlank();
        assertThat(profileAll.at("/profileJson/ed650d7d-6201-42fb-a4c3-b9efb93dda0c/metadata/imageBase64").asText()).isNotBlank();

        final JsonNode branchAll = OBJECT_MAPPER.readTree(DttSwaggerExamples.BRANCH_EXPORT_OBJECT_AUTO_RESOLVE);
        assertThat(branchAll.at("/branchEquipment/metadata/0/imageBase64").asText()).isNotBlank();
    }

    @Test
    void shouldKeepStructuredApplyExamplesWithMergeStrategyAndOverrides() throws Exception {
        final JsonNode profileStructured = OBJECT_MAPPER.readTree(DttSwaggerExamples.IMPORT_PROFILE_REQUEST_STRUCTURED_FULL);
        assertThat(profileStructured.path("mergeStrategy").asText()).isEqualTo("FAIL_IF_EXISTS");
        assertThat(profileStructured.at("/deviceTypes/0/deviceTypeParamValues").isObject()).isTrue();

        final JsonNode branchStructured = OBJECT_MAPPER.readTree(DttSwaggerExamples.IMPORT_BRANCH_REQUEST_STRUCTURED_FULL);
        assertThat(branchStructured.path("mergeStrategy").asText()).isEqualTo("FAIL_IF_EXISTS");
        assertThat(branchStructured.at("/branches/0/deviceTypes/0/devices/0/deviceParamValues").isObject()).isTrue();
    }

    private void assertObjectField(String json, String field) throws Exception {
        final JsonNode root = OBJECT_MAPPER.readTree(json);
        assertThat(root.path(field).isObject()).isTrue();
        assertThat(root.path(field).isTextual()).isFalse();
    }
}
