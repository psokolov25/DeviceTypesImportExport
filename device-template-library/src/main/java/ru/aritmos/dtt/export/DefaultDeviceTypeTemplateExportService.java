package ru.aritmos.dtt.export;

import ru.aritmos.dtt.archive.DttArchiveWriter;
import ru.aritmos.dtt.archive.model.DttArchiveTemplate;
import ru.aritmos.dtt.exception.TemplateExportException;

import java.io.OutputStream;
import java.util.Objects;

/**
 * Реализация сервиса экспорта, делегирующая запись бинарного архива в {@link DttArchiveWriter}.
 */
public class DefaultDeviceTypeTemplateExportService implements DeviceTypeTemplateExportService {

    private final DttArchiveWriter archiveWriter;

    /**
     * @param archiveWriter writer DTT-архива
     */
    public DefaultDeviceTypeTemplateExportService(DttArchiveWriter archiveWriter) {
        this.archiveWriter = Objects.requireNonNull(archiveWriter, "archiveWriter is required");
    }

    @Override
    public void exportOne(DttArchiveTemplate template, OutputStream outputStream) {
        try {
            archiveWriter.write(template, outputStream);
        } catch (RuntimeException exception) {
            throw new TemplateExportException("Ошибка экспорта DTT-шаблона", exception);
        }
    }
}
