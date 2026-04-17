package ru.aritmos.dtt.importing;

import ru.aritmos.dtt.archive.model.DttArchiveTemplate;

import java.io.InputStream;

/**
 * Публичный сервис импорта одного DTT-архива в archive DTO модель.
 */
public interface DeviceTypeTemplateImportService {

    /**
     * Импортирует один DTT-архив.
     *
     * @param inputStream входной поток ZIP-архива
     * @return восстановленный шаблон
     */
    DttArchiveTemplate importOne(InputStream inputStream);
}
