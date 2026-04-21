package ru.aritmos.dtt.demo.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import ru.aritmos.dtt.api.dto.MergeStrategy;

import java.util.List;

/**
 * Запрос на одновременную сборку profile и branch JSON из набора DTT с metadata override.
 *
 * @param deviceTypes список DTT для сборки с metadata override уровня profile
 * @param branches список branch c metadata override уровня отделений
 * @param mergeStrategy merge-стратегия
 */
@Schema(description = "Запрос на одновременную сборку profile/branch с наследованием metadata")
public record ImportProfileBranchWithMetadataRequest(
        @Schema(description = "Список DTT и metadata override уровня profile", requiredMode = Schema.RequiredMode.REQUIRED)
        List<ImportProfileDeviceTypeRequest> deviceTypes,
        @Schema(description = "Список branch и metadata override уровня отделений", requiredMode = Schema.RequiredMode.REQUIRED)
        List<ImportBranchMetadataRequest> branches,
        @Schema(description = "Стратегия merge", example = "FAIL_IF_EXISTS")
        MergeStrategy mergeStrategy
) {
}
