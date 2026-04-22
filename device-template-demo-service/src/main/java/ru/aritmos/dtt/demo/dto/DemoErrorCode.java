package ru.aritmos.dtt.demo.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Машинно-читаемые коды ошибок demo API.
 *
 * <p>Код используется для стабильной интеграции клиентских приложений,
 * чтобы не зависеть от текстового сообщения исключения.</p>
 */
@Schema(description = "Машинно-читаемый код ошибки demo API")
public enum DemoErrorCode {

    /** Некорректные входные аргументы запроса. */
    INVALID_ARGUMENT,

    /** Ошибка формата DTT/JSON payload. */
    DTT_FORMAT_ERROR,

    /** Ошибка валидации шаблона. */
    TEMPLATE_VALIDATION_ERROR,

    /** Ошибка импорта шаблона. */
    TEMPLATE_IMPORT_ERROR,

    /** Ошибка экспорта шаблона. */
    TEMPLATE_EXPORT_ERROR,

    /** Ошибка сборки/merge шаблонов. */
    TEMPLATE_ASSEMBLY_ERROR,

    /** Непредвиденная внутренняя ошибка. */
    INTERNAL_ERROR
}
