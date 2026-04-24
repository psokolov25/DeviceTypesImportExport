package ru.aritmos.dtt.demo.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import ru.aritmos.dtt.api.dto.MergeStrategy;
import ru.aritmos.dtt.api.dto.importplan.BranchImportSourceRequest;

import java.util.List;

/**
 * JSON-метаданные для multipart upload импорта zip-архива с .dtt файлами в branch equipment JSON.
 *
 * @param branchIds legacy-режим: импортировать все .dtt в перечисленные отделения
 * @param mergeStrategy стратегия merge
 * @param branches структурированное описание отделений и используемых .dtt файлов
 */
@Schema(description = "JSON-метаданные для multipart upload импорта zip-архива в branch equipment JSON")
public record ImportDttZipToBranchUploadRequest(
        @Schema(description = "Legacy-режим: список идентификаторов отделений, в которые будут импортированы все .dtt из zip")
        List<String> branchIds,
        @Schema(description = "Стратегия merge", example = "FAIL_IF_EXISTS")
        MergeStrategy mergeStrategy,
        @Schema(description = "Структурированное описание отделений и используемых .dtt файлов")
        List<BranchImportSourceRequest> branches
) {
}
