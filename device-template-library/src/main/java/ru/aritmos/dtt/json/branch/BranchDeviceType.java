package ru.aritmos.dtt.json.branch;

import ru.aritmos.dtt.api.dto.DeviceTypeTemplate;

import java.util.Map;

/**
 * Модель типа устройства в отделении с шаблонными параметрами и дочерними устройствами.
 *
 * @param template шаблон типа устройства
 * @param devices экземпляры устройств
 * @param kind тип устройства из канонического branch JSON (`type`)
 * @param onStartEvent секция скрипта onStartEvent в исходной модели branch
 * @param onStopEvent секция скрипта onStopEvent в исходной модели branch
 * @param onPublicStartEvent секция скрипта onPublicStartEvent в исходной модели branch
 * @param onPublicFinishEvent секция скрипта onPublicFinishEvent в исходной модели branch
 * @param deviceTypeFunctions скрипт функций типа устройства
 * @param eventHandlers секция обработчиков событий
 * @param commands секция команд
 */
public record BranchDeviceType(
        DeviceTypeTemplate template,
        Map<String, DeviceInstanceTemplate> devices,
        String kind,
        BranchScript onStartEvent,
        BranchScript onStopEvent,
        BranchScript onPublicStartEvent,
        BranchScript onPublicFinishEvent,
        String deviceTypeFunctions,
        Map<String, BranchScript> eventHandlers,
        Map<String, BranchScript> commands
) { }
