package ru.aritmos.dtt.archive;

import ru.aritmos.dtt.archive.model.DttArchiveTemplate;

import java.io.OutputStream;

/**
 * Записывает archive DTO в бинарный DTT ZIP-архив с YAML-файлами.
 */
public interface DttArchiveWriter {

    /**
     * Сериализует шаблон в выходной поток DTT-архива.
     *
     * @param template шаблон одного типа устройства
     * @param outputStream поток для записи ZIP
     */
    void write(DttArchiveTemplate template, OutputStream outputStream);
}
