package ru.aritmos.dtt.export;

import ru.aritmos.dtt.archive.model.DttArchiveTemplate;

import java.io.OutputStream;

/**
 * Публичный сервис экспорта одного типа устройства в формат DTT.
 */
public interface DeviceTypeTemplateExportService {

    /**
     * Экспортирует шаблон типа устройства в поток DTT-архива.
     *
     * @param template шаблон одного типа устройства
     * @param outputStream поток для записи DTT
     */
    void exportOne(DttArchiveTemplate template, OutputStream outputStream);
}
