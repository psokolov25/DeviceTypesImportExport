package ru.aritmos.dtt.importing;

import ru.aritmos.dtt.archive.DttArchiveReader;
import ru.aritmos.dtt.archive.model.DttArchiveTemplate;
import ru.aritmos.dtt.exception.TemplateImportException;

import java.io.InputStream;
import java.util.Objects;

/**
 * Реализация сервиса импорта, делегирующая парсинг архива в {@link DttArchiveReader}.
 */
public class DefaultDeviceTypeTemplateImportService implements DeviceTypeTemplateImportService {

    private final DttArchiveReader archiveReader;

    /**
     * @param archiveReader reader DTT-архива
     */
    public DefaultDeviceTypeTemplateImportService(DttArchiveReader archiveReader) {
        this.archiveReader = Objects.requireNonNull(archiveReader, "archiveReader is required");
    }

    @Override
    public DttArchiveTemplate importOne(InputStream inputStream) {
        try {
            return archiveReader.read(inputStream);
        } catch (RuntimeException exception) {
            throw new TemplateImportException("Ошибка импорта DTT-шаблона", exception);
        }
    }
}
