package ru.aritmos.dtt.demo.openapi;

/**
 * Подготовленные примеры для Swagger UI, основанные на структуре export-fixed.json.
 */
public final class DttSwaggerExamples {

    private DttSwaggerExamples() {
    }

    public static final String IMPORT_BRANCH_REQUEST_MERGE_NON_NULLS = """
            {
              "archivesBase64": [
                "UEsDB...Display-WD3264...",
                "UEsDB...Terminal..."
              ],
              "branchIds": [
                "ec8d252d-deb9-4ebb-accf-0ef7994bf17b",
                "37493d1c-8282-4417-a729-dceac1f3e2b4"
              ],
              "mergeStrategy": "MERGE_NON_NULLS"
            }
            """;

    public static final String IMPORT_BRANCH_REQUEST_CREATE_COPY = """
            {
              "archivesBase64": [
                "UEsDB...Reception..."
              ],
              "branchIds": [
                "37493d1c-8282-4417-a729-dceac1f3e2b4"
              ],
              "mergeStrategy": "CREATE_COPY_WITH_SUFFIX"
            }
            """;

    public static final String IMPORT_BRANCH_RESPONSE_EXAMPLE = """
            {
              "branchesCount": 2,
              "branchJson": "{\n  \"ec8d252d-deb9-4ebb-accf-0ef7994bf17b\": {\n    \"id\": \"ec8d252d-deb9-4ebb-accf-0ef7994bf17b\",\n    \"displayName\": \"test kate\",\n    \"deviceTypes\": {\n      \"63967ac2-c6bd-4c8d-b7eb-f3ea8f94194a\": {\n        \"id\": \"63967ac2-c6bd-4c8d-b7eb-f3ea8f94194a\",\n        \"name\": \"Display WD3264\",\n        \"deviceTypeParamValues\": {\n          \"TicketZone\": {\"value\": \"3\"},\n          \"ServicePointNameZone\": {\"value\": \"1\"}\n        },\n        \"devices\": {\n          \"b7232760-cff0-425b-b53c-c75d65a659c6\": {\n            \"deviceParamValues\": {\n              \"IP\": {\"value\": \"192.168.3.211\"},\n              \"Port\": {\"value\": 22224},\n              \"ServicePointDisplayName\": {\"value\": \"OKHO 1\"}\n            }\n          }\n        }\n      }\n    }\n  },\n  \"37493d1c-8282-4417-a729-dceac1f3e2b4\": {\n    \"id\": \"37493d1c-8282-4417-a729-dceac1f3e2b4\",\n    \"displayName\": \"Отделение на Тверской\"\n  }\n}"
            }
            """;

    public static final String IMPORT_BRANCH_MERGE_REQUEST_EXAMPLE = """
            {
              "existingBranchJson": "{\n  \"ec8d252d-deb9-4ebb-accf-0ef7994bf17b\": {\n    \"id\": \"ec8d252d-deb9-4ebb-accf-0ef7994bf17b\",\n    \"displayName\": \"test kate\",\n    \"deviceTypes\": {\n      \"ed650d7d-6201-42fb-a4c3-b9efb93dda0c\": {\n        \"id\": \"ed650d7d-6201-42fb-a4c3-b9efb93dda0c\",\n        \"name\": \"Terminal\",\n        \"displayName\": \"Терминал (Киоск)\",\n        \"deviceTypeParamValues\": {\n          \"prefix\": {\"value\": \"SSS\"},\n          \"translatorURL\": {\"value\": \"http://192.168.7.20:8104?prefix=SSS\"}\n        },\n        \"devices\": {\n          \"811fb688-546f-495d-be86-58a63c5d560d\": {\n            \"id\": \"811fb688-546f-495d-be86-58a63c5d560d\",\n            \"name\": \"entry_point 1\"\n          }\n        }\n      }\n    }\n  }\n}",
              "archivesBase64": [
                "UEsDB...Terminal...",
                "UEsDB...Display-WD3264..."
              ],
              "branchIds": [
                "ec8d252d-deb9-4ebb-accf-0ef7994bf17b"
              ],
              "mergeStrategy": "MERGE_NON_NULLS"
            }
            """;

    public static final String BRANCH_EXPORT_OBJECT_AUTO_RESOLVE = """
            {
              "branchEquipment": {
                "branches": {
                  "ec8d252d-deb9-4ebb-accf-0ef7994bf17b": {
                    "id": "ec8d252d-deb9-4ebb-accf-0ef7994bf17b",
                    "displayName": "test kate",
                    "deviceTypes": {
                      "ffde364f-5f5a-45e6-86b7-1215a28ae96c": {
                        "id": "ffde364f-5f5a-45e6-86b7-1215a28ae96c",
                        "name": "Reception",
                        "displayName": "Приёмная",
                        "description": "Приёмная",
                        "type": "reception",
                        "deviceTypeParamValues": {
                          "multiService": {"value": true},
                          "showQueues": {"value": true}
                        },
                        "devices": {
                          "14124427-9723-4faf-9a67-105dd4431a16": {
                            "id": "14124427-9723-4faf-9a67-105dd4431a16",
                            "name": "reception 1",
                            "displayName": "reception 1"
                          }
                        },
                        "onPublicFinishEvent": {"inputParameters": {}, "outputParameters": []}
                      }
                    },
                    "prefix": "KAT"
                  },
                  "37493d1c-8282-4417-a729-dceac1f3e2b4": {
                    "id": "37493d1c-8282-4417-a729-dceac1f3e2b4",
                    "displayName": "Отделение на Тверской",
                    "deviceTypes": {
                      "ffde364f-5f5a-45e6-86b7-1215a28ae96c": {
                        "id": "ffde364f-5f5a-45e6-86b7-1215a28ae96c",
                        "name": "Reception",
                        "displayName": "Reception",
                        "description": "Устройство озвучивания",
                        "type": "reception",
                        "deviceTypeParamValues": {
                          "phrase": {"value": "\\eFПосетитель с номером{ticketId}"},
                          "URL": {"value": "http://192.168.1.8:8080/unnamed/rest/play"}
                        },
                        "eventHandlers": {
                          "VISIT_CALLED": {
                            "inputParameters": {},
                            "outputParameters": [],
                            "scriptCode": "import org.slf4j.LoggerFactory\\nprintln 'voice call'"
                          }
                        },
                        "devices": {}
                      }
                    }
                  }
                }
              },
              "branchIds": [
                "ec8d252d-deb9-4ebb-accf-0ef7994bf17b",
                "37493d1c-8282-4417-a729-dceac1f3e2b4"
              ],
              "dttVersion": "2.1.0"
            }
            """;

    public static final String BRANCH_EXPORT_OBJECT_FAIL_IF_EXISTS = """
            {
              "branchEquipment": {
                "branches": {
                  "ec8d252d-deb9-4ebb-accf-0ef7994bf17b": {
                    "id": "ec8d252d-deb9-4ebb-accf-0ef7994bf17b",
                    "displayName": "test kate",
                    "deviceTypes": {
                      "ffde364f-5f5a-45e6-86b7-1215a28ae96c": {
                        "id": "ffde364f-5f5a-45e6-86b7-1215a28ae96c",
                        "name": "Reception",
                        "displayName": "Приёмная",
                        "description": "Приёмная",
                        "type": "reception",
                        "deviceTypeParamValues": {
                          "multiService": {"value": true}
                        },
                        "devices": {}
                      }
                    }
                  },
                  "37493d1c-8282-4417-a729-dceac1f3e2b4": {
                    "id": "37493d1c-8282-4417-a729-dceac1f3e2b4",
                    "displayName": "Отделение на Тверской",
                    "deviceTypes": {
                      "ffde364f-5f5a-45e6-86b7-1215a28ae96c": {
                        "id": "ffde364f-5f5a-45e6-86b7-1215a28ae96c",
                        "name": "Reception",
                        "displayName": "Reception",
                        "description": "Устройство озвучивания",
                        "type": "reception",
                        "deviceTypeParamValues": {
                          "phrase": {"value": "\\eFПосетитель с номером{ticketId}"},
                          "URL": {"value": "http://192.168.1.8:8080/unnamed/rest/play"}
                        },
                        "devices": {}
                      }
                    }
                  }
                }
              },
              "mergeStrategy": "FAIL_IF_EXISTS"
            }
            """;

    public static final String BRANCH_EXPORT_JSON_STRING_FILTERED = """
            {
              "branchJson": "{\n  \"ec8d252d-deb9-4ebb-accf-0ef7994bf17b\": {\n    \"id\": \"ec8d252d-deb9-4ebb-accf-0ef7994bf17b\",\n    \"displayName\": \"test kate\",\n    \"deviceTypes\": {\n      \"ed650d7d-6201-42fb-a4c3-b9efb93dda0c\": {\n        \"id\": \"ed650d7d-6201-42fb-a4c3-b9efb93dda0c\",\n        \"name\": \"Terminal\",\n        \"displayName\": \"Терминал (Киоск)\",\n        \"description\": \"Терминал (Киоск)\",\n        \"type\": \"entry_point\",\n        \"deviceTypeParamValues\": {\n          \"prefix\": {\"value\": \"SSS\"},\n          \"translatorTicket\": {\"value\": \"http://192.168.7.20:8114/printing\"}\n        },\n        \"devices\": {\n          \"811fb688-546f-495d-be86-58a63c5d560d\": {\n            \"id\": \"811fb688-546f-495d-be86-58a63c5d560d\",\n            \"name\": \"entry_point 1\",\n            \"displayName\": \"entry_point 1\"\n          }\n        }\n      }\n    },\n    \"prefix\": \"KAT\"\n  }\n}",
              "branchIds": [
                "ec8d252d-deb9-4ebb-accf-0ef7994bf17b"
              ],
              "deviceTypeIds": [
                "ed650d7d-6201-42fb-a4c3-b9efb93dda0c"
              ],
              "mergeStrategy": "MERGE_NON_NULLS",
              "dttVersion": "2.1.0"
            }
            """;
}
