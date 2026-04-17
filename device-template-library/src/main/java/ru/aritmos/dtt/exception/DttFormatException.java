package ru.aritmos.dtt.exception;

/**
 * Исключение ошибок формата DTT-архива: отсутствие обязательных файлов, broken YAML или неконсистентные данные.
 */
public class DttFormatException extends RuntimeException {

    /**
     * Создаёт исключение формата с сообщением.
     *
     * @param message диагностичное описание причины
     */
    public DttFormatException(String message) {
        super(message);
    }

    /**
     * Создаёт исключение формата с сообщением и первопричиной.
     *
     * @param message диагностичное описание причины
     * @param cause исходная причина ошибки
     */
    public DttFormatException(String message, Throwable cause) {
        super(message, cause);
    }
}
