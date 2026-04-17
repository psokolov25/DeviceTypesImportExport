package ru.aritmos.dtt.api.dto;

import java.util.List;

/**
 * Результат валидации шаблона DTT.
 *
 * @param valid общий флаг корректности
 * @param issues список найденных диагностических проблем
 */
public record ValidationResult(boolean valid, List<ValidationIssue> issues) {
}
