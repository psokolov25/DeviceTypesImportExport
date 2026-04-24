package ru.aritmos.dtt.api.dto.importplan;

/**
 * Прикладное представление детального preview profile-импорта.
 *
 * <p>DTO объединяет сериализованный profile JSON, агрегированные счётчики и
 * карту вычислений defaults/overrides в одном контракте фасада.
 * Такой формат позволяет внешним адаптерам (например, REST-контроллерам)
 * не дублировать оркестрацию вида {@code preview -> toJson -> count -> map}.
 *
 * @param profileJson сериализованный preview profile JSON
 * @param deviceTypesCount количество записей в карте {@code deviceTypes}
 * @param computationsByDeviceType сводка defaults/overrides по каждому {@code deviceTypeId}
 */
public record ProfileImportPreviewView(
        String profileJson,
        int deviceTypesCount,
        java.util.Map<String, ImportPreviewComputationEntry> computationsByDeviceType
) {
}
