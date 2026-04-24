package ru.aritmos.dtt.api.dto;

import java.util.Map;

/**
 * Прикладное представление batch-экспорта набора DTT-архивов.
 *
 * <p>DTO используется адаптерами API, которым нужен готовый ответ вида
 * {@code deviceTypeId -> archiveBase64} с агрегированным количеством архивов
 * без дополнительной ручной пост-обработки.
 *
 * @param archivesBase64ByDeviceTypeId экспортированные DTT-архивы в Base64 по ключу {@code deviceTypeId}
 * @param archivesCount количество экспортированных архивов
 */
public record BatchDttExportView(
        Map<String, String> archivesBase64ByDeviceTypeId,
        int archivesCount
) {
}
