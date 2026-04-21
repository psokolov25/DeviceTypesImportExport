package ru.aritmos.dtt.demo.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Краткая инспекция содержимого DTT-архива для быстрого просмотра в demo API.
 *
 * @param formatName имя формата
 * @param formatVersion версия формата
 * @param deviceTypeId идентификатор типа устройства
 * @param deviceTypeName имя типа устройства
 * @param deviceTypeVersion версия типа устройства
 * @param deviceTypeDescription описание типа устройства
 * @param imageBase64 иконка типа устройства в PNG Base64
 * @param eventHandlersCount количество event handlers
 * @param commandsCount количество commands
 */
@Schema(description = "Краткая информация о DTT-архиве")
public record DttInspectionResponse(
        String formatName,
        String formatVersion,
        String deviceTypeId,
        String deviceTypeName,
        String deviceTypeVersion,
        String deviceTypeDescription,
        String imageBase64,
        int eventHandlersCount,
        int commandsCount
) {
}
