package ru.aritmos.dtt.demo.dto;

import com.fasterxml.jackson.databind.JsonNode;

/**
 * Результат одновременной сборки profile и branch JSON из набора DTT.
 *
 * @param profileJson собранный profile JSON
 * @param branchJson собранный branch equipment JSON
 */
public record ImportProfileBranchWithMetadataResponse(JsonNode profileJson, JsonNode branchJson) {
}
