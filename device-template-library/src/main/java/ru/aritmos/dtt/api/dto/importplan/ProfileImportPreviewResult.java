package ru.aritmos.dtt.api.dto.importplan;

import ru.aritmos.dtt.json.profile.EquipmentProfile;

import java.util.Map;

/**
 * Детальный preview profile-импорта на уровне high-level import-plan.
 *
 * <p>Содержит одновременно:
 * <ul>
 *   <li>собранную preview-модель {@link EquipmentProfile};</li>
 *   <li>диагностику вычисленных defaults/overrides по каждому {@code deviceTypeId}.</li>
 * </ul>
 *
 * <p>DTO полезен для прикладных служб, которым нужен один вызов фасада вместо отдельной
 * последовательности {@code assembleProfile(...)} + {@code computeProfileImportPreview(...)}.
 *
 * @param profile preview-модель профиля оборудования
 * @param computationsByDeviceType сводка defaults/overrides по каждому {@code deviceTypeId}
 */
public record ProfileImportPreviewResult(
        EquipmentProfile profile,
        Map<String, ImportPreviewComputationEntry> computationsByDeviceType
) {
}
