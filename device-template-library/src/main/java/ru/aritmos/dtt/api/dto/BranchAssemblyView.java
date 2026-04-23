package ru.aritmos.dtt.api.dto;

import java.util.List;

/**
 * Представление результата сборки/импорта branch equipment JSON для прикладного слоя.
 *
 * <p>Содержит JSON, агрегированную мета-информацию и количество branch, чтобы
 * API-адаптеры не дублировали сервисную оркестрацию после вызова фасада.
 *
 * @param branchJson сериализованный branch equipment JSON
 * @param branchesCount количество branch в результате
 * @param deviceTypeMetadata список метаданных типов устройств из корневой секции metadata
 */
public record BranchAssemblyView(
        String branchJson,
        int branchesCount,
        List<DeviceTypeMetadata> deviceTypeMetadata
) {
}
