package ru.aritmos.dtt.api.dto;

/**
 * Результат сравнения версии из внешнего запроса с версией, зафиксированной в DTT-архиве.
 *
 * @param inputVersion нормализованная версия из входного параметра
 * @param dttVersion нормализованная версия, считанная из DTT descriptor
 * @param greaterVersion большая из двух версий
 * @param greaterSource источник большей версии: {@code INPUT}, {@code DTT} или {@code EQUAL}
 */
public record DttVersionComparisonResult(
        String inputVersion,
        String dttVersion,
        String greaterVersion,
        String greaterSource
) {
}
