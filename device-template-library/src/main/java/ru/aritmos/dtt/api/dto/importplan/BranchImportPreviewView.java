package ru.aritmos.dtt.api.dto.importplan;

import ru.aritmos.dtt.api.dto.DeviceTypeMetadata;

import java.util.List;
import java.util.Map;

/**
 * Прикладное представление детального preview branch-импорта.
 *
 * <p>DTO содержит сериализованный branch equipment JSON, агрегированные счётчики,
 * metadata типов устройств и диагностику defaults/overrides в одном контракте фасада.
 *
 * @param branchJson сериализованный preview branch equipment JSON
 * @param branchesCount количество branch в результирующей preview-модели
 * @param deviceTypeMetadata метаданные типов устройств из корневой секции metadata
 * @param computationsByTarget сводка defaults/overrides по ключу {@code branchId:deviceTypeId}
 */
public record BranchImportPreviewView(
        String branchJson,
        int branchesCount,
        List<DeviceTypeMetadata> deviceTypeMetadata,
        Map<String, ImportPreviewComputationEntry> computationsByTarget
) {
}
