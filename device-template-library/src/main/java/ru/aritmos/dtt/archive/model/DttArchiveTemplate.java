package ru.aritmos.dtt.archive.model;

import ru.aritmos.dtt.api.dto.DeviceTypeMetadata;

import java.util.Map;

/**
 * Полная archive DTO-модель одного DTT шаблона типа устройства.
 *
 * @param descriptor дескриптор манифеста DTT
 * @param metadata метаданные типа устройства
 * @param deviceTypeParametersSchema схема параметров типа устройства
 * @param deviceParametersSchema схема параметров дочерних устройств
 * @param bindingHints подсказки для биндинга значений
 * @param defaultValues значения по умолчанию
 * @param exampleValues примерные значения
 * @param templateOrigin метаданные происхождения шаблона
 * @param onStartEvent скрипт жизненного цикла запуска
 * @param onStopEvent скрипт жизненного цикла остановки
 * @param onPublicStartEvent публичный скрипт старта
 * @param onPublicFinishEvent публичный скрипт завершения
 * @param deviceTypeFunctions набор функций типа устройства
 * @param eventHandlers скрипты обработчиков событий
 * @param commands скрипты команд
 */
public record DttArchiveTemplate(
        DttArchiveDescriptor descriptor,
        DeviceTypeMetadata metadata,
        Map<String, Object> deviceTypeParametersSchema,
        Map<String, Object> deviceParametersSchema,
        Map<String, Object> bindingHints,
        Map<String, Object> defaultValues,
        Map<String, Object> exampleValues,
        Map<String, Object> templateOrigin,
        String onStartEvent,
        String onStopEvent,
        String onPublicStartEvent,
        String onPublicFinishEvent,
        String deviceTypeFunctions,
        Map<String, String> eventHandlers,
        Map<String, String> commands
) {
}
