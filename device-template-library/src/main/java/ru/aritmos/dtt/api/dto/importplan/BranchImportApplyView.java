package ru.aritmos.dtt.api.dto.importplan;

import ru.aritmos.dtt.api.dto.DeviceTypeMetadata;

import java.util.List;
import java.util.Map;

/**
 * Прикладное представление результата apply-импорта branch equipment JSON.
 *
 * <p>DTO объединяет финальный сериализованный branch equipment JSON, агрегированные счётчики,
 * metadata типов устройств и диагностику расчёта defaults/overrides по каждому таргету импорта.
 *
 * @param branchJson сериализованный branch equipment JSON после apply
 * @param branchesCount количество branch в результирующем JSON
 * @param deviceTypeMetadata метаданные типов устройств из корневой секции metadata
 * @param computationsByTarget сводка defaults/overrides по ключу {@code branchId:deviceTypeId}
 */
public record BranchImportApplyView(
        String branchJson,
        int branchesCount,
        List<DeviceTypeMetadata> deviceTypeMetadata,
        Map<String, ImportPreviewComputationEntry> computationsByTarget
) {
}
