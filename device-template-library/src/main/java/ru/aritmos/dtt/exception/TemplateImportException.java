package ru.aritmos.dtt.exception;

/**
 * Исключение импорта DTT-шаблона.
 *
 * <p>Используется в сценариях чтения архива (`.dtt`) и преобразования его в
 * внутренние/публичные модели библиотеки, когда входные данные или состояние
 * пайплайна импорта не позволяют корректно завершить операцию.</p>
 */
public class TemplateImportException extends RuntimeException {

    /**
     * Создаёт исключение импорта с диагностическим сообщением.
     *
     * @param message описание причины ошибки
     */
    public TemplateImportException(String message) {
        super(message);
    }

    /**
     * Создаёт исключение импорта с диагностическим сообщением и причиной.
     *
     * @param message описание причины ошибки
     * @param cause исходная причина ошибки
     */
    public TemplateImportException(String message, Throwable cause) {
        super(message, cause);
    }
}
