package ru.aritmos.dtt.api.dto;

/**
 * Результат импорта одного DTT-архива.
 *
 * @param template импортированный шаблон типа устройства
 * @param validationResult результат валидации импортированного шаблона
 */
public record ImportResult(
        DeviceTypeTemplate template,
        ValidationResult validationResult
) {
}
