package ru.aritmos.dtt.exception;

/**
 * Исключение ошибок сборки профиля оборудования или branch equipment JSON из DTT.
 */
public class TemplateAssemblyException extends RuntimeException {

    /**
     * Создаёт исключение с диагностичным сообщением.
     *
     * @param message текст ошибки
     */
    public TemplateAssemblyException(String message) {
        super(message);
    }
}
