package ru.aritmos.dtt.api.dto.importplan;

import ru.aritmos.dtt.api.dto.MergeStrategy;

import java.util.List;

/**
 * Высокоуровневый план одновременной сборки profile и branch
 * с наследованием metadata.
 *
 * <p>Сценарий полезен, когда из одного набора DTT требуется за один вызов получить:
 * <ul>
 *     <li>profile оборудования с profile-уровнем metadata override;</li>
 *     <li>branch equipment c branch-специфичными metadata override для тех же типов устройств.</li>
 * </ul>
 *
 * @param deviceTypes DTT-источники и profile-уровень metadata override
 * @param branches branch, для которых нужно собрать branch equipment
 * @param mergeStrategy стратегия merge при совпадении типов устройств
 */
public record ProfileBranchMetadataImportPlanRequest(
        List<ProfileDeviceTypeImportSourceRequest> deviceTypes,
        List<BranchMetadataImportRequest> branches,
        MergeStrategy mergeStrategy
) {
}
