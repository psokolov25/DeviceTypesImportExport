package ru.aritmos.dtt.exception;

/**
 * Исключение фатальной ошибки валидации шаблона, когда невозможно построить корректный результат проверки.
 */
public class TemplateValidationException extends RuntimeException {

    /**
     * @param message диагностичное сообщение
     */
    public TemplateValidationException(String message) {
        super(message);
    }

    /**
     * @param message диагностичное сообщение
     * @param cause исходная причина
     */
    public TemplateValidationException(String message, Throwable cause) {
        super(message, cause);
    }
}
