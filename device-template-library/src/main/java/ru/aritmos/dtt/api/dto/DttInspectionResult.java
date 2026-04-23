package ru.aritmos.dtt.api.dto;

/**
 * Результат инспекции одного DTT-архива на уровне фасада библиотеки.
 *
 * <p>DTO предназначен для прикладных сервисов и API-адаптеров, которым нужно
 * быстро показать карточку шаблона без ручного разбора archive-модели.
 *
 * @param formatName имя формата архива (например, {@code DTT})
 * @param formatVersion версия формата архива
 * @param deviceTypeId идентификатор типа устройства из шаблона
 * @param deviceTypeName системное имя типа устройства
 * @param deviceTypeVersion версия шаблона типа устройства
 * @param deviceTypeDescription описание типа устройства
 * @param iconBase64 иконка типа устройства в Base64 (может быть {@code null})
 * @param eventHandlersCount количество event-handler скриптов
 * @param commandsCount количество command-скриптов
 */
public record DttInspectionResult(
        String formatName,
        String formatVersion,
        String deviceTypeId,
        String deviceTypeName,
        String deviceTypeVersion,
        String deviceTypeDescription,
        String iconBase64,
        int eventHandlersCount,
        int commandsCount
) {
}
