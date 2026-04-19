package ru.aritmos.dtt.exception;

/**
 * Исключение экспорта DTT-шаблона.
 *
 * <p>Используется в сценариях сериализации шаблона в формат архива (`.dtt`) и
 * выгрузки набора шаблонов, когда операция экспорта завершается с ошибкой.</p>
 */
public class TemplateExportException extends RuntimeException {

    /**
     * Создаёт исключение экспорта с диагностическим сообщением.
     *
     * @param message описание причины ошибки
     */
    public TemplateExportException(String message) {
        super(message);
    }

    /**
     * Создаёт исключение экспорта с диагностическим сообщением и причиной.
     *
     * @param message описание причины ошибки
     * @param cause исходная причина ошибки
     */
    public TemplateExportException(String message, Throwable cause) {
        super(message, cause);
    }
}
