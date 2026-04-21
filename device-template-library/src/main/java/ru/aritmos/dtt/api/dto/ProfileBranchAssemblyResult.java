package ru.aritmos.dtt.api.dto;

import ru.aritmos.dtt.json.branch.BranchEquipment;
import ru.aritmos.dtt.json.profile.EquipmentProfile;

/**
 * Результат совместной сборки profile JSON и branch equipment JSON из одного или нескольких DTT.
 *
 * @param profile собранный профиль оборудования (карта {@code deviceTypes})
 * @param branchEquipment собранный JSON оборудования отделений
 */
public record ProfileBranchAssemblyResult(
        EquipmentProfile profile,
        BranchEquipment branchEquipment
) {
}
