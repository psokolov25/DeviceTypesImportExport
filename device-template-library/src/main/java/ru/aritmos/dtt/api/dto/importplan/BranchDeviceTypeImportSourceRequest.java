package ru.aritmos.dtt.api.dto.importplan;

import ru.aritmos.dtt.api.dto.DeviceTypeMetadata;
import ru.aritmos.dtt.api.dto.branch.DeviceInstanceImportRequest;

import java.util.List;
import java.util.Map;

/**
 * Описание одного источника типа устройства для подготовки branch-импорта.
 *
 * <p>Источник может ссылаться либо на Base64-представление DTT-архива,
 * либо на `.dtt` entry внутри zip-пакета. Допускаются override metadata,
 * значений параметров типа устройства, экземпляров устройств и поля {@code kind}.
 *
 * @param archiveBase64 DTT-архив в Base64
 * @param archiveEntryName имя `.dtt` entry внутри zip-пакета
 * @param deviceTypeParamValues override-значения параметров типа устройства
 * @param metadataOverride override metadata типа устройства
 * @param devices override-описания экземпляров устройств
 * @param kind override поля {@code type} в branch JSON
 */
public record BranchDeviceTypeImportSourceRequest(
        String archiveBase64,
        String archiveEntryName,
        Map<String, Object> deviceTypeParamValues,
        DeviceTypeMetadata metadataOverride,
        List<DeviceInstanceImportRequest> devices,
        String kind
) {
}
