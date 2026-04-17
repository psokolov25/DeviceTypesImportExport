package ru.aritmos.dtt.api;

import ru.aritmos.dtt.api.dto.EquipmentProfileAssemblyRequest;
import ru.aritmos.dtt.api.dto.branch.BranchEquipmentAssemblyRequest;
import ru.aritmos.dtt.json.branch.BranchEquipment;
import ru.aritmos.dtt.json.profile.EquipmentProfile;

/**
 * Сервис сборки целевых JSON-представлений из одного или нескольких DTT-шаблонов.
 */
public interface TemplateAssemblyService {

    /**
     * Собирает JSON профиля оборудования (карту {@code deviceTypes}) из набора DTT.
     *
     * @param request параметры сборки, merge-стратегия и override-значения
     * @return результирующий профиль оборудования
     */
    EquipmentProfile assembleEquipmentProfile(EquipmentProfileAssemblyRequest request);

    /**
     * Собирает branch equipment JSON (карту отделений) из одного или нескольких DTT.
     *
     * @param request параметры сборки отделений
     * @return результирующая модель branch equipment
     */
    BranchEquipment assembleBranchEquipment(BranchEquipmentAssemblyRequest request);
}

