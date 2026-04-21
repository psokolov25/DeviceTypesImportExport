package ru.aritmos.dtt.json.branch;

/**
 * Метаданные типа устройства в корневой секции {@code metadata} branch equipment JSON.
 *
 * @param deviceTypeId идентификатор типа устройства
 * @param name имя типа устройства
 * @param version прикладная версия типа устройства
 * @param description описание типа устройства
 * @param imageBase64 иконка типа устройства (PNG Base64)
 */
public record BranchDeviceTypeMetadata(
        String deviceTypeId,
        String name,
        String version,
        String description,
        String imageBase64
) {
}
