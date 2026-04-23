package ru.aritmos.dtt.api.dto.importplan;

/**
 * Диагностическая запись preview-подготовки импорта.
 *
 * <p>Используется для показа пользователю, сколько значений параметров пришло из исходного шаблона
 * и сколько override-значений было наложено поверх шаблона перед фактической сборкой profile/branch JSON.
 *
 * @param defaultValuesCount количество default-значений/параметров, полученных из DTT
 * @param overrideValuesCount количество override-значений, переданных запросом
 */
public record ImportPreviewComputationEntry(
        int defaultValuesCount,
        int overrideValuesCount
) {
}
