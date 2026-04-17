package ru.aritmos.dtt.archive.model;

/**
 * Дескриптор манифеста DTT-архива с ключевыми сведениями о формате и типе устройства.
 *
 * @param formatName имя формата, ожидается {@code DTT}
 * @param formatVersion версия формата архива
 * @param deviceTypeId идентификатор типа устройства в архиве
 */
public record DttArchiveDescriptor(String formatName, String formatVersion, String deviceTypeId) {
}
