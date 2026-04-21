package ru.aritmos.dtt.demo.dto;

import com.fasterxml.jackson.databind.JsonNode;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.Map;

/**
 * Детальный preview-ответ сборки profile JSON с рассчитанными defaults/overrides.
 *
 * @param profileJson preview profile JSON
 * @param computationsByDeviceType расчёт defaults/overrides по deviceTypeId
 */
@Schema(description = "Детальный preview profile JSON с расчётом defaults/overrides")
public record ProfilePreviewDetailedResponse(
        @Schema(description = "Preview profile JSON как объект")
        JsonNode profileJson,
        @Schema(description = "Сводка по defaults/overrides для каждого типа устройства")
        Map<String, PreviewComputationEntry> computationsByDeviceType
) {
}
