package ru.aritmos.dtt.api.dto.importplan;

import ru.aritmos.dtt.api.dto.DeviceTypeMetadata;
import ru.aritmos.dtt.api.dto.branch.DeviceInstanceImportRequest;

import java.util.List;
import java.util.Map;

/**
 * Переопределение конкретного типа устройства для branch в сценарии
 * совместной сборки profile+branch из производных типов.
 *
 * <p>В отличие от {@link BranchDeviceTypeImportSourceRequest}, эта DTO
 * не указывает архив DTT напрямую, а ссылается на уже собранный тип
 * устройства уровня profile по идентификатору {@code profileDeviceTypeId}.</p>
 *
 * @param profileDeviceTypeId идентификатор типа устройства из profile-части
 * @param deviceTypeParamValues branch-уровневые override-значения параметров типа
 * @param metadataOverride branch-уровневый override metadata
 * @param devices override-описания экземпляров устройств
 * @param kind override поля {@code type} в branch JSON
 */
public record BranchDerivedDeviceTypeImportRequest(
        String profileDeviceTypeId,
        Map<String, Object> deviceTypeParamValues,
        DeviceTypeMetadata metadataOverride,
        List<DeviceInstanceImportRequest> devices,
        String kind
) {
}
