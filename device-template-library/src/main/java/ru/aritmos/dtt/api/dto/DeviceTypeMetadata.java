package ru.aritmos.dtt.api.dto;

/**
 * Метаданные шаблона типа устройства в переносимом DTT-представлении.
 *
 * @param id стабильный идентификатор типа устройства
 * @param name системное имя типа
 * @param displayName отображаемое имя
 * @param description описание назначения типа устройства
 */
public record DeviceTypeMetadata(String id, String name, String displayName, String description) {
}
