package ru.aritmos.dtt.demo.dto;

import com.fasterxml.jackson.databind.JsonNode;
import io.swagger.v3.oas.annotations.media.Schema;
import ru.aritmos.dtt.api.dto.importplan.ImportPreviewComputationEntry;

import java.util.Map;

/**
 * Детальный preview-ответ сборки branch equipment JSON с рассчитанными defaults/overrides.
 *
 * @param branchJson preview branch JSON
 * @param computationsByTarget сводка defaults/overrides по ключу {@code branchId:deviceTypeId}
 */
@Schema(description = "Детальный preview branch equipment JSON с расчётом defaults/overrides")
public record BranchPreviewDetailedResponse(
        @Schema(description = "Preview branch equipment JSON как объект")
        JsonNode branchJson,
        @Schema(description = "Сводка defaults/overrides по ключу branchId:deviceTypeId")
        Map<String, ImportPreviewComputationEntry> computationsByTarget
) {
}
