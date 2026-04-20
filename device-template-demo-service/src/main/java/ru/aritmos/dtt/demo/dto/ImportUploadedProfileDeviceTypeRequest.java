package ru.aritmos.dtt.demo.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.Map;

/**
 * Описание одного DTT-файла внутри загруженного zip-архива для сборки профиля оборудования.
 *
 * @param archiveEntryName имя .dtt файла внутри zip-архива
 * @param deviceTypeParamValues override-значения параметров типа устройства
 */
@Schema(description = "Описание одного DTT файла из zip-архива для сборки профиля оборудования")
public record ImportUploadedProfileDeviceTypeRequest(
        @Schema(description = "Имя .dtt файла внутри zip-архива", requiredMode = Schema.RequiredMode.REQUIRED,
                example = "Terminal.dtt")
        String archiveEntryName,
        @Schema(description = "Override-значения параметров типа устройства")
        Map<String, Object> deviceTypeParamValues
) {
}
