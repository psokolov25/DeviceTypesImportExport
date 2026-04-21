package ru.aritmos.dtt.demo.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Переопределение метаданных типа устройства при импорте одного и того же DTT как нескольких разных типов.
 *
 * @param id целевой идентификатор типа устройства; если не задан, может быть вычислен из имени/отображаемого имени
 * @param name целевое системное имя типа устройства
 * @param displayName целевое отображаемое имя типа устройства
 * @param description целевое описание типа устройства
 */
@Schema(description = "Переопределение метаданных типа устройства при импорте DTT")
public record ImportDeviceTypeMetadataOverrideRequest(
        @Schema(description = "Целевой идентификатор типа устройства", example = "display-wd3264-red-window")
        String id,
        @Schema(description = "Целевое системное имя типа устройства", example = "Display WD3264 Красное окно")
        String name,
        @Schema(description = "Целевое отображаемое имя типа устройства", example = "Display WD3264 Красное окно")
        String displayName,
        @Schema(description = "Целевое описание типа устройства", example = "Шаблон красного окна")
        String description
) {
}
