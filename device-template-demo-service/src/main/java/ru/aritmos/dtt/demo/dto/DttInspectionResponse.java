package ru.aritmos.dtt.demo.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Краткая инспекция содержимого DTT-архива для быстрого просмотра в demo API.
 *
 * @param formatName имя формата
 * @param formatVersion версия формата
 * @param deviceTypeId идентификатор типа устройства
 * @param deviceTypeName имя типа устройства
 * @param eventHandlersCount количество event handlers
 * @param commandsCount количество commands
 */
@Schema(description = "Краткая информация о DTT-архиве")
public record DttInspectionResponse(
        String formatName,
        String formatVersion,
        String deviceTypeId,
        String deviceTypeName,
        int eventHandlersCount,
        int commandsCount
) {
}
