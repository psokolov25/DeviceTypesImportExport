package ru.aritmos.dtt.model.canonical;

/**
 * Канонические метаданные типа устройства, используемые как внутренняя точка консолидации между DTT/profile/branch.
 *
 * @param id идентификатор типа устройства
 * @param name системное имя
 * @param displayName отображаемое имя
 * @param description описание
 */
public record CanonicalDeviceTypeMetadata(String id, String name, String displayName, String description) {
}
