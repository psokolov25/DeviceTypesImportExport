package ru.aritmos.dtt.archive;

import ru.aritmos.dtt.api.dto.DeviceTypeMetadata;
import ru.aritmos.dtt.archive.model.DttArchiveTemplate;

import java.text.Normalizer;
import java.util.Objects;

/**
 * Утилиты для формирования человеко-читаемых имён файлов DTT-архивов.
 */
public final class DttFileNames {

    private DttFileNames() {
    }

    /**
     * Возвращает базовое имя файла для DTT по метаданным типа устройства.
     *
     * @param template шаблон DTT
     * @return имя файла без расширения
     */
    public static String resolveBaseName(DttArchiveTemplate template) {
        Objects.requireNonNull(template, "template is required");
        return resolveBaseName(template.metadata(), template.descriptor().deviceTypeId());
    }

    /**
     * Возвращает базовое имя файла по метаданным типа устройства.
     *
     * @param metadata метаданные типа устройства
     * @param fallback резервное значение
     * @return имя файла без расширения
     */
    public static String resolveBaseName(DeviceTypeMetadata metadata, String fallback) {
        if (metadata != null) {
            final String fromName = sanitizeBaseName(metadata.name());
            if (!fromName.isBlank()) {
                return fromName;
            }
            final String fromDisplayName = sanitizeBaseName(metadata.displayName());
            if (!fromDisplayName.isBlank()) {
                return fromDisplayName;
            }
            final String fromId = sanitizeBaseName(metadata.id());
            if (!fromId.isBlank()) {
                return fromId;
            }
        }
        final String sanitizedFallback = sanitizeBaseName(fallback);
        return sanitizedFallback.isBlank() ? "device-type" : sanitizedFallback;
    }

    /**
     * Нормализует человеко-читаемое имя файла, сохраняя Unicode-символы.
     *
     * @param candidate исходное имя
     * @return безопасное имя файла без расширения
     */
    public static String sanitizeBaseName(String candidate) {
        if (candidate == null || candidate.isBlank()) {
            return "";
        }
        String normalized = Normalizer.normalize(candidate, Normalizer.Form.NFKC)
                .replaceAll("[\\\\/:*?\"<>|]", "_")
                .replaceAll("[\\p{Cntrl}]", " ")
                .replaceAll("\\s+", " ")
                .trim();
        normalized = normalized.replaceAll("^[. ]+|[. ]+$", "");
        return normalized;
    }

    /**
     * Строит ASCII fallback для HTTP filename, когда клиент не умеет UTF-8 в заголовках.
     *
     * @param baseName желаемое Unicode-имя
     * @param fallback резервное значение
     * @return ASCII-safe имя без расширения
     */
    public static String toAsciiFallbackBaseName(String baseName, String fallback) {
        final String primary = sanitizeBaseName(baseName);
        final String reserved = sanitizeBaseName(fallback);
        String ascii = Normalizer.normalize(primary, Normalizer.Form.NFKD)
                .replaceAll("[^\\p{ASCII}]", "")
                .replaceAll("[\\\\/:*?\"<>|]", "_")
                .replaceAll("\\s+", "-")
                .replaceAll("[^A-Za-z0-9._()-]", "_")
                .replaceAll("_+", "_")
                .replaceAll("-+", "-")
                .replaceAll("^[._-]+|[._-]+$", "");
        if (!ascii.isBlank()) {
            return ascii;
        }
        final String fallbackAscii = reserved
                .replaceAll("\\s+", "-")
                .replaceAll("[^A-Za-z0-9._()-]", "_")
                .replaceAll("_+", "_")
                .replaceAll("-+", "-")
                .replaceAll("^[._-]+|[._-]+$", "");
        return fallbackAscii.isBlank() ? "device-type" : fallbackAscii;
    }
}
