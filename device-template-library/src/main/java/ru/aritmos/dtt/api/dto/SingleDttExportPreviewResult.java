package ru.aritmos.dtt.api.dto;

/**
 * Результат preview-проверки single-export операции в `.dtt`.
 *
 * <p>Используется для сценариев, где нужно заранее проверить, можно ли выгрузить
 * один тип устройства в DTT без формирования HTTP-ответа или фактической загрузки файла клиенту.
 *
 * @param success успешность preview-вычисления
 * @param deviceTypeId идентификатор типа устройства
 * @param archiveSizeBytes размер экспортированного архива в байтах (если {@code success=true})
 * @param issueCode код ошибки preview (если {@code success=false})
 * @param issueMessage текст диагностики preview (если {@code success=false})
 */
public record SingleDttExportPreviewResult(
        boolean success,
        String deviceTypeId,
        Integer archiveSizeBytes,
        String issueCode,
        String issueMessage
) {
}

