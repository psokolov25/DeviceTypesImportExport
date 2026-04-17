package ru.aritmos.dtt.importing;

import ru.aritmos.dtt.archive.DttArchiveReader;
import ru.aritmos.dtt.archive.model.DttArchiveTemplate;

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
        return archiveReader.read(inputStream);
    }
}
