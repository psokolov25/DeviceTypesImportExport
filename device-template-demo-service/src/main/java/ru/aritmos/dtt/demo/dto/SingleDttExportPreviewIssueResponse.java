package ru.aritmos.dtt.demo.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Диагностическая запись preview single-export сценария.
 *
 * @param code код проблемы
 * @param message пояснение проблемы
 */
@Schema(description = "Диагностическая запись preview single-export")
public record SingleDttExportPreviewIssueResponse(
        @Schema(description = "Код проблемы", example = "MERGE_CONFLICT")
        String code,
        @Schema(description = "Описание проблемы", example = "Конфликт deviceTypeId display при merge FAIL_IF_EXISTS")
        String message
) {
}
