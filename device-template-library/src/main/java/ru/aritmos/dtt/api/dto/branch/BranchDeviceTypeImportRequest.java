package ru.aritmos.dtt.api.dto.branch;

import ru.aritmos.dtt.api.dto.EquipmentProfileDeviceTypeRequest;
import ru.aritmos.dtt.json.branch.BranchScript;

import java.util.List;
import java.util.Map;

/**
 * Запрос на добавление типа устройства и его экземпляров в конкретное отделение.
 *
 * @param deviceTypeRequest запрос типа устройства из DTT
 * @param deviceInstances экземпляры устройств данного типа
 * @param kind тип устройства (`type`) для канонического branch JSON
 * @param onStartEvent lifecycle-скрипт onStartEvent
 * @param onStopEvent lifecycle-скрипт onStopEvent
 * @param onPublicStartEvent lifecycle-скрипт onPublicStartEvent
 * @param onPublicFinishEvent lifecycle-скрипт onPublicFinishEvent
 * @param deviceTypeFunctions функции типа устройства
 * @param eventHandlers обработчики событий
 * @param commands команды
 */
public record BranchDeviceTypeImportRequest(
        EquipmentProfileDeviceTypeRequest deviceTypeRequest,
        List<DeviceInstanceImportRequest> deviceInstances,
        String kind,
        BranchScript onStartEvent,
        BranchScript onStopEvent,
        BranchScript onPublicStartEvent,
        BranchScript onPublicFinishEvent,
        String deviceTypeFunctions,
        Map<String, BranchScript> eventHandlers,
        Map<String, BranchScript> commands
) {
}
