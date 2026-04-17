package ru.aritmos.dtt.api.dto.branch;

import ru.aritmos.dtt.api.dto.EquipmentProfileDeviceTypeRequest;

import java.util.List;

/**
 * Запрос на добавление типа устройства и его экземпляров в конкретное отделение.
 *
 * @param deviceTypeRequest запрос типа устройства из DTT
 * @param deviceInstances экземпляры устройств данного типа
 */
public record BranchDeviceTypeImportRequest(
        EquipmentProfileDeviceTypeRequest deviceTypeRequest,
        List<DeviceInstanceImportRequest> deviceInstances
) {
}
