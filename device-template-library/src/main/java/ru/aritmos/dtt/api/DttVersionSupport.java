package ru.aritmos.dtt.api;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Утилиты сравнения прикладных версий шаблонов DTT.
 */
public final class DttVersionSupport {

    private DttVersionSupport() {
    }

    /**
     * Сравнивает две версии.
     *
     * @param left левая версия
     * @param right правая версия
     * @return {@code >0}, если left больше right; {@code <0}, если меньше; {@code 0}, если равны
     */
    public static int compare(String left, String right) {
        final List<String> leftTokens = tokenize(left);
        final List<String> rightTokens = tokenize(right);
        final int max = Math.max(leftTokens.size(), rightTokens.size());
        for (int index = 0; index < max; index++) {
            final String leftToken = index < leftTokens.size() ? leftTokens.get(index) : "0";
            final String rightToken = index < rightTokens.size() ? rightTokens.get(index) : "0";
            final int tokenComparison = compareToken(leftToken, rightToken);
            if (tokenComparison != 0) {
                return tokenComparison;
            }
        }
        return 0;
    }

    /**
     * Возвращает большую из двух версий.
     *
     * @param first первая версия
     * @param second вторая версия
     * @return большая версия
     */
    public static String max(String first, String second) {
        final String normalizedFirst = normalize(first);
        final String normalizedSecond = normalize(second);
        return compare(normalizedFirst, normalizedSecond) >= 0 ? normalizedFirst : normalizedSecond;
    }

    /**
     * Нормализует версию: null/blank -> {@code 1.0}.
     *
     * @param version входная версия
     * @return нормализованная версия
     */
    public static String normalize(String version) {
        if (version == null || version.isBlank()) {
            return "1.0";
        }
        return version.trim();
    }

    private static List<String> tokenize(String version) {
        final String normalized = normalize(version).toLowerCase(Locale.ROOT);
        final String[] rawTokens = normalized.split("[._\\-]");
        final List<String> tokens = new ArrayList<>(rawTokens.length);
        for (String rawToken : rawTokens) {
            if (!rawToken.isBlank()) {
                tokens.add(rawToken);
            }
        }
        return tokens.isEmpty() ? List.of("0") : tokens;
    }

    private static int compareToken(String left, String right) {
        final boolean leftNumeric = left.chars().allMatch(Character::isDigit);
        final boolean rightNumeric = right.chars().allMatch(Character::isDigit);
        if (leftNumeric && rightNumeric) {
            return Integer.compare(Integer.parseInt(left), Integer.parseInt(right));
        }
        if (leftNumeric) {
            return 1;
        }
        if (rightNumeric) {
            return -1;
        }
        return left.compareTo(right);
    }
}
