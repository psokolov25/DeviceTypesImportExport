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
    }

    private void assertObjectField(String json, String field) throws Exception {
        final JsonNode root = OBJECT_MAPPER.readTree(json);
        assertThat(root.path(field).isObject()).isTrue();
        assertThat(root.path(field).isTextual()).isFalse();
    }
}
