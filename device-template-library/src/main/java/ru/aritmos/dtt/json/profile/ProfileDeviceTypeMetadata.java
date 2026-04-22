package ru.aritmos.dtt.json.profile;

/**
 * Метаданные типа устройства в корневой секции {@code metadata} profile JSON.
 *
 * @param id идентификатор типа устройства
 * @param name имя типа устройства
 * @param displayName отображаемое имя типа устройства
 * @param version прикладная версия типа устройства
 * @param description описание типа устройства
 * @param imageBase64 иконка типа устройства (PNG Base64)
 */
public record ProfileDeviceTypeMetadata(
        String id,
        String name,
        String displayName,
        String version,
        String description,
        String imageBase64
) {
}
