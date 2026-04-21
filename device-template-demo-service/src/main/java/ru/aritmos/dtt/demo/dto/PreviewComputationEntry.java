package ru.aritmos.dtt.demo.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Сводка расчёта defaults/overrides для одного типа устройства в preview-режиме.
 *
 * @param defaultsAppliedCount количество рассчитанных значений по умолчанию
 * @param overridesAppliedCount количество явно переданных override-значений
 */
@Schema(description = "Сводка рассчитанных defaults/overrides для одного типа устройства")
public record PreviewComputationEntry(
        @Schema(description = "Количество defaults", example = "5")
        int defaultsAppliedCount,
        @Schema(description = "Количество overrides", example = "2")
        int overridesAppliedCount
) {
}
