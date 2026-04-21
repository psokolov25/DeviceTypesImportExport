package ru.aritmos.dtt.api.dto;

/**
 * Метаданные шаблона типа устройства в переносимом DTT-представлении.
 *
 * @param id стабильный идентификатор типа устройства
 * @param name системное имя типа
 * @param displayName отображаемое имя
 * @param description описание назначения типа устройства
 * @param version прикладная версия типа устройства
 * @param iconBase64 иконка типа устройства в формате PNG (Base64)
 */
public record DeviceTypeMetadata(String id,
                                 String name,
                                 String displayName,
                                 String description,
                                 String version,
                                 String iconBase64) {

    /**
     * Упрощённый конструктор для обратной совместимости с существующими вызовами.
     *
     * @param id стабильный идентификатор типа устройства
     * @param name системное имя типа
     * @param displayName отображаемое имя
     * @param description описание назначения типа устройства
     */
    public DeviceTypeMetadata(String id, String name, String displayName, String description) {
        this(id, name, displayName, description, null, null);
    }
}
