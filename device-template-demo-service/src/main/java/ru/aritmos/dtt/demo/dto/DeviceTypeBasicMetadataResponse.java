package ru.aritmos.dtt.demo.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Базовые метаданные типа устройства для сценариев инспекции DTT/DTT-set.
 *
 * @param id идентификатор типа устройства
 * @param name имя типа устройства
 * @param version прикладная версия шаблона
 * @param description описание типа устройства
 * @param imageBase64 иконка типа устройства в формате PNG Base64
 */
@Schema(description = "Базовые метаданные типа устройства")
public record DeviceTypeBasicMetadataResponse(
        @Schema(description = "Идентификатор типа устройства", example = "display")
        String id,
        @Schema(description = "Название типа устройства", example = "Display")
        String name,
        @Schema(description = "Версия типа устройства", example = "1.0.0")
        String version,
        @Schema(description = "Описание типа устройства", example = "Экран электронной очереди")
        String description,
        @Schema(description = "PNG-иконка в Base64")
        String imageBase64
) {
}
