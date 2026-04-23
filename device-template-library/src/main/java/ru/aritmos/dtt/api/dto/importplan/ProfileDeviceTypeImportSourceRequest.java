package ru.aritmos.dtt.api.dto.importplan;

import ru.aritmos.dtt.api.dto.DeviceTypeMetadata;

import java.util.Map;

/**
 * Описание одного источника типа устройства для подготовки profile-импорта.
 *
 * <p>Источник может ссылаться либо на Base64-представление отдельного DTT-архива,
 * либо на имя entry внутри ранее загруженного zip-пакета с несколькими `.dtt`.
 * Дополнительно допускаются override-значения параметров и metadata.
 *
 * @param archiveBase64 DTT-архив в Base64; используется в обычном JSON/API режиме
 * @param archiveEntryName имя `.dtt` entry внутри zip-пакета; используется в upload-download сценариях
 * @param deviceTypeParamValues override-значения параметров типа устройства
 * @param metadataOverride override metadata типа устройства
 */
public record ProfileDeviceTypeImportSourceRequest(
        String archiveBase64,
        String archiveEntryName,
        Map<String, Object> deviceTypeParamValues,
        DeviceTypeMetadata metadataOverride
) {
}
