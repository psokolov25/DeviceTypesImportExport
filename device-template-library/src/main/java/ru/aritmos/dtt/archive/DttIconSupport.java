package ru.aritmos.dtt.archive;

import java.util.Base64;
import java.util.Objects;

/**
 * Утилиты работы с иконкой типа устройства в формате PNG внутри DTT.
 */
public final class DttIconSupport {

    /**
     * Имя файла иконки в корне DTT-архива.
     */
    public static final String ICON_FILE_NAME = "icon.png";

    /**
     * Встроенная PNG-иконка по умолчанию (Base64).
     */
    public static final String DEFAULT_ICON_BASE64 =
            "iVBORw0KGgoAAAANSUhEUgAAAEgAAABICAIAAADajyQQAAAAw0lEQVR4nO3YwQ3CMAxF0Y5g8P7LnY4CE0nQFF5nNQh8kLwM7aV2Uq9p7z5s8+fPjQmZb5m2f2rQw4m1gRj4Qk0iJxE2iQnETaJCcRNooJxE2iQnETaJCcRNooJxE2iQnETaJCcRNooJxE2iQnETaJCcRNooJxE2iQnETaJCcRNooJxE2iQnETaJCcRNooJxE2iQnETaJCcRNooJxE2iQnETaJCcRNooJxE2iQnETaJCcRNooJxE2iQnETaJCfQF2n1A0Yt0YwMAAAAASUVORK5CYII=";

    private static volatile String configuredDefaultIconBase64 = DEFAULT_ICON_BASE64;

    private DttIconSupport() {
    }

    /**
     * Возвращает непустую Base64-иконку: исходную или встроенную по умолчанию.
     *
     * @param iconBase64 исходная иконка Base64
     * @return валидная Base64-строка PNG
     */
    public static String resolveOrDefault(String iconBase64) {
        if (iconBase64 == null || iconBase64.isBlank()) {
            return configuredDefaultIconBase64;
        }
        return iconBase64;
    }


    /**
     * Конфигурирует PNG-иконку, которая будет использоваться по умолчанию
     * при отсутствии {@code imageBase64} в JSON и отсутствии {@code icon.png} в DTT-архиве.
     *
     * @param iconBase64 Base64-представление PNG-иконки; при пустом значении возвращается
     *                   встроенная библиотечная иконка по умолчанию
     */
    public static void configureDefaultIconBase64(String iconBase64) {
        if (iconBase64 == null || iconBase64.isBlank()) {
            configuredDefaultIconBase64 = DEFAULT_ICON_BASE64;
            return;
        }
        Base64.getDecoder().decode(iconBase64);
        configuredDefaultIconBase64 = iconBase64;
    }

    /**
     * Сбрасывает runtime-конфигурацию иконки по умолчанию до встроенного значения.
     */
    public static void resetDefaultIconBase64() {
        configuredDefaultIconBase64 = DEFAULT_ICON_BASE64;
    }

    /**
     * Декодирует Base64-иконку в бинарный PNG.
     *
     * @param iconBase64 Base64-представление PNG
     * @return байты PNG
     */
    public static byte[] decode(String iconBase64) {
        return Base64.getDecoder().decode(resolveOrDefault(iconBase64));
    }

    /**
     * Кодирует бинарный PNG в Base64.
     *
     * @param pngBytes байты PNG
     * @return Base64-строка
     */
    public static String encode(byte[] pngBytes) {
        return Base64.getEncoder().encodeToString(Objects.requireNonNull(pngBytes, "pngBytes must not be null"));
    }
}
