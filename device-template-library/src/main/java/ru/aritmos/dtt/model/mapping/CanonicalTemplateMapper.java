package ru.aritmos.dtt.model.mapping;

import ru.aritmos.dtt.archive.model.DttArchiveTemplate;
import ru.aritmos.dtt.model.canonical.CanonicalDeviceTypeTemplate;

/**
 * Маппер преобразований между archive DTO и канонической внутренней моделью.
 */
public interface CanonicalTemplateMapper {

    /**
     * @param archiveTemplate archive DTO одного DTT
     * @return каноническая модель
     */
    CanonicalDeviceTypeTemplate toCanonical(DttArchiveTemplate archiveTemplate);

    /**
     * @param canonicalTemplate каноническая модель
     * @return archive DTO одного DTT
     */
    DttArchiveTemplate toArchive(CanonicalDeviceTypeTemplate canonicalTemplate);
}
