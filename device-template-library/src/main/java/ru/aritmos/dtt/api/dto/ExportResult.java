package ru.aritmos.dtt.api.dto;

/**
 * Результат экспорта одного типа устройства в DTT-архив.
 *
 * @param deviceTypeId идентификатор экспортированного типа устройства
 * @param archiveBytes бинарное содержимое DTT-архива
 * @param archiveBase64 Base64-представление DTT-архива
 */
public record ExportResult(
        String deviceTypeId,
        byte[] archiveBytes,
        String archiveBase64
) {
}
