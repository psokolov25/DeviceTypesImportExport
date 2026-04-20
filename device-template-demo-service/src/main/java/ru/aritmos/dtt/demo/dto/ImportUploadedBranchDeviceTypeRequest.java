package ru.aritmos.dtt.demo.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;
import java.util.Map;

/**
 * Описание одного DTT-файла внутри загруженного zip-архива для сборки branch equipment JSON.
 *
 * @param archiveEntryName имя .dtt файла внутри zip-архива
 * @param deviceTypeParamValues override-значения параметров типа устройства
 * @param devices override-описания устройств данного типа
 * @param kind override поля {@code type} в branch JSON
 */
@Schema(description = "Описание одного DTT файла из zip-архива для сборки branch equipment JSON")
public record ImportUploadedBranchDeviceTypeRequest(
        @Schema(description = "Имя .dtt файла внутри zip-архива", requiredMode = Schema.RequiredMode.REQUIRED,
                example = "Display WD3264.dtt")
        String archiveEntryName,
        @Schema(description = "Override-значения параметров типа устройства")
        Map<String, Object> deviceTypeParamValues,
        @Schema(description = "Override-описания устройств данного типа")
        List<ImportBranchDeviceRequest> devices,
        @Schema(description = "Override поля type для branch JSON")
        String kind
) {
}
