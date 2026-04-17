package ru.aritmos.dtt.api.dto;

/**
 * Диагностическая запись о проблеме валидации шаблона.
 *
 * @param code машинно-читабельный код проблемы
 * @param path путь до секции/файла/скрипта, где выявлена проблема
 * @param message человеко-читаемое описание
 */
public record ValidationIssue(String code, String path, String message) {
}
