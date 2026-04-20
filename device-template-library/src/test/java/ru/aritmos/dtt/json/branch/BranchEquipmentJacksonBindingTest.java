package ru.aritmos.dtt.json.branch;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class BranchEquipmentJacksonBindingTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void shouldDeserializeRawDeviceManagerJsonIntoCanonicalBranchEquipmentModel() throws Exception {
        final String json = """
                {
                  "branches": {
                    "branch-1": {
                      "id": "branch-1",
                      "displayName": "Main",
                      "deviceTypes": {
                        "display": {
                          "id": "display",
                          "name": "Display",
                          "displayName": "Display",
                          "description": "desc",
                          "type": "display",
                          "deviceTypeParamValues": {
                            "mode": { "value": "normal" }
                          },
                          "devices": {
                            "dev-1": {
                              "id": "dev-1",
                              "name": "Display 1",
                              "displayName": "Display 1"
                            }
                          }
                        }
                      }
                    }
                  }
                }
                """;

        final BranchEquipment branchEquipment = objectMapper.readValue(json, BranchEquipment.class);

        assertThat(branchEquipment).isNotNull();
        assertThat(branchEquipment.branches()).containsKey("branch-1");
        final BranchDeviceType branchDeviceType = branchEquipment.branches()
                .get("branch-1")
                .deviceTypes()
                .get("display");
        assertThat(branchDeviceType).isNotNull();
        assertThat(branchDeviceType.template()).isNotNull();
        assertThat(branchDeviceType.template().metadata()).isNotNull();
        assertThat(branchDeviceType.template().metadata().id()).isEqualTo("display");
        assertThat(branchDeviceType.template().metadata().name()).isEqualTo("Display");
        assertThat(branchDeviceType.kind()).isEqualTo("display");
        assertThat(branchDeviceType.devices()).containsKey("dev-1");
    }
}
