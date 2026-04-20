package ru.aritmos.dtt.demo.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

/**
 * Запрос на сборку одного отделения из zip-архива с DTT-файлами.
 *
 * @param branchId идентификатор отделения
 * @param displayName отображаемое имя отделения
 * @param deviceTypes список типов устройств, ссылающихся на конкретные .dtt файлы внутри zip
 */
@Schema(description = "Запрос на сборку одного отделения из загруженного zip-архива с DTT файлами")
public record ImportUploadedBranchRequest(
        @Schema(description = "Идентификатор отделения", requiredMode = Schema.RequiredMode.REQUIRED)
        String branchId,
        @Schema(description = "Отображаемое имя отделения")
        String displayName,
        @Schema(description = "Список типов устройств с привязкой к .dtt файлам внутри zip",
                requiredMode = Schema.RequiredMode.REQUIRED)
        List<ImportUploadedBranchDeviceTypeRequest> deviceTypes
) {
}
