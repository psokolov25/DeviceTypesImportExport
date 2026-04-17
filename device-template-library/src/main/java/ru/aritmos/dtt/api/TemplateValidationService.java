package ru.aritmos.dtt.api;

import ru.aritmos.dtt.api.dto.ValidationResult;
import ru.aritmos.dtt.archive.model.DttArchiveTemplate;

/**
 * Публичный сервис валидации DTT-шаблонов.
 */
public interface TemplateValidationService {

    /**
     * Выполняет валидацию структуры и Groovy-скриптов шаблона.
     *
     * @param template шаблон одного типа устройства
     * @return результат валидации с ошибками/предупреждениями
     */
    ValidationResult validate(DttArchiveTemplate template);
}
