package ru.aritmos.dtt.demo.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

/**
 * Ответ со списком базовых метаданных типов устройств из одного DTT или zip-набора DTT.
 *
 * @param metadata список метаданных типов устройств
 */
@Schema(description = "Список базовых метаданных типов устройств из DTT/DTT-set")
public record DttMetadataBatchResponse(
        @Schema(description = "Список базовых метаданных типов устройств")
        List<DeviceTypeBasicMetadataResponse> metadata
) {
}
