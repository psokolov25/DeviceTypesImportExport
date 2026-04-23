package ru.aritmos.dtt.api.dto;

/**
 * Представление результата сборки/импорта profile JSON для прикладного слоя.
 *
 * <p>DTO используется как транспортный формат между фасадом библиотеки и
 * внешними адаптерами (например, REST service), чтобы не дублировать
 * вычисление количества типов устройств и сериализацию в JSON.
 *
 * @param profileJson сериализованный profile JSON
 * @param deviceTypesCount количество записей в карте {@code deviceTypes}
 */
public record ProfileAssemblyView(
        String profileJson,
        int deviceTypesCount
) {
}
