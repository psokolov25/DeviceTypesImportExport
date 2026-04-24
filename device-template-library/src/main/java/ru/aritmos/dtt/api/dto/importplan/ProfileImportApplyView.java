package ru.aritmos.dtt.api.dto.importplan;

import java.util.Map;

/**
 * Прикладное представление результата apply-импорта profile JSON.
 *
 * <p>DTO объединяет финальный сериализованный profile JSON, агрегированный счётчик типов устройств
 * и диагностику расчёта defaults/overrides по каждому типу устройства.
 *
 * @param profileJson сериализованный профиль оборудования после apply
 * @param deviceTypesCount количество типов устройств в результирующем profile JSON
 * @param computationsByDeviceType сводка defaults/overrides по {@code deviceTypeId}
 */
public record ProfileImportApplyView(
        String profileJson,
        int deviceTypesCount,
        Map<String, ImportPreviewComputationEntry> computationsByDeviceType
) {
}
