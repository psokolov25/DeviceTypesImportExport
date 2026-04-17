package ru.aritmos.dtt.archive;

import ru.aritmos.dtt.archive.model.DttArchiveTemplate;

import java.io.InputStream;

/**
 * Читает DTT ZIP-архив и восстанавливает archive DTO одного типа устройства.
 */
public interface DttArchiveReader {

    /**
     * Десериализует входной DTT-архив.
     *
     * @param inputStream поток входного ZIP-архива
     * @return восстановленный шаблон
     */
    DttArchiveTemplate read(InputStream inputStream);
}
