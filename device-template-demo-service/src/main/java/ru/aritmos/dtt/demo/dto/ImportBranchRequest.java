package ru.aritmos.dtt.demo.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

/**
 * Запрос на сборку одного отделения из одного или нескольких DTT с индивидуальными override-значениями.
 *
 * @param branchId идентификатор отделения
 * @param displayName отображаемое имя отделения
 * @param deviceTypes список импортируемых типов устройств
 */
@Schema(description = "Запрос на сборку одного отделения из набора DTT с override-значениями")
public record ImportBranchRequest(
        @Schema(description = "Идентификатор отделения", requiredMode = Schema.RequiredMode.REQUIRED)
        String branchId,
        @Schema(description = "Отображаемое имя отделения")
        String displayName,
        @Schema(description = "Список типов устройств для импорта в отделение", requiredMode = Schema.RequiredMode.REQUIRED)
        List<ImportBranchDeviceTypeRequest> deviceTypes
) {
}
