package ru.aritmos.dtt.demo.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.Map;

/**
 * Override-описание конкретного устройства при импорте DTT в branch equipment JSON.
 *
 * @param id идентификатор устройства; если совпадает с устройством из DTT, значения будут объединены
 * @param name системное имя устройства
 * @param displayName отображаемое имя
 * @param description описание устройства
 * @param deviceParamValues override-значения параметров устройства
 */
@Schema(description = "Override-описание устройства при импорте DTT в отделение")
public record ImportBranchDeviceRequest(
        @Schema(description = "Идентификатор устройства")
        String id,
        @Schema(description = "Системное имя устройства")
        String name,
        @Schema(description = "Отображаемое имя устройства")
        String displayName,
        @Schema(description = "Описание устройства")
        String description,
        @Schema(description = "Override-значения параметров устройства")
        Map<String, Object> deviceParamValues
) {
}
