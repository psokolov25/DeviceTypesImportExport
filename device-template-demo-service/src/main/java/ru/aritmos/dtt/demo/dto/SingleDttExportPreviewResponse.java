package ru.aritmos.dtt.demo.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

/**
 * Результат preview single-export для одного типа устройства.
 *
 * @param canExport флаг, что экспорт может быть выполнен без ошибок
 * @param deviceTypeId идентификатор типа устройства
 * @param archiveSizeBytes размер бинарного архива при успешном preview
 * @param issues список диагностических проблем
 */
@Schema(description = "Результат preview single-export одного DTT")
public record SingleDttExportPreviewResponse(
        @Schema(description = "Можно ли выполнить экспорт", example = "true")
        boolean canExport,
        @Schema(description = "Идентификатор типа устройства", example = "display")
        String deviceTypeId,
        @Schema(description = "Размер архива в байтах при успешном preview", example = "2048")
        Integer archiveSizeBytes,
        @Schema(description = "Список диагностических проблем")
        List<SingleDttExportPreviewIssueResponse> issues
) {
}
