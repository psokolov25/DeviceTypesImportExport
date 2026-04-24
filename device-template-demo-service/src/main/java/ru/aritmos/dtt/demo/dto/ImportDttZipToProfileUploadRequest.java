package ru.aritmos.dtt.demo.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import ru.aritmos.dtt.api.dto.MergeStrategy;
import ru.aritmos.dtt.api.dto.importplan.ProfileDeviceTypeImportSourceRequest;

import java.util.List;

/**
 * JSON-метаданные для multipart upload импорта zip-архива с .dtt файлами в профиль оборудования.
 *
 * @param mergeStrategy стратегия merge
 * @param deviceTypes список .dtt файлов из zip и override-значений параметров типа устройства
 */
@Schema(description = "JSON-метаданные для multipart upload импорта zip-архива в профиль оборудования")
public record ImportDttZipToProfileUploadRequest(
        @Schema(description = "Стратегия merge", example = "FAIL_IF_EXISTS")
        MergeStrategy mergeStrategy,
        @Schema(description = "Список .dtt файлов из zip и override-значений параметров типа устройства")
        List<ProfileDeviceTypeImportSourceRequest> deviceTypes
) {
}
