package ru.aritmos.dtt.api;

import ru.aritmos.dtt.api.dto.EquipmentProfileAssemblyRequest;
import ru.aritmos.dtt.api.dto.MergeStrategy;
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
     * Выполняет preview-сборку JSON профиля оборудования без сохранения результата.
     *
     * @param request параметры сборки, merge-стратегия и override-значения
     * @return рассчитанный профиль оборудования
     */
    EquipmentProfile previewEquipmentProfile(EquipmentProfileAssemblyRequest request);

    /**
     * Собирает branch equipment JSON (карту отделений) из одного или нескольких DTT.
     *
     * @param request параметры сборки отделений
     * @return результирующая модель branch equipment
     */
    BranchEquipment assembleBranchEquipment(BranchEquipmentAssemblyRequest request);

    /**
     * Выполняет preview-сборку branch equipment JSON без сохранения результата.
     *
     * @param request параметры сборки отделений
     * @return рассчитанная модель branch equipment
     */
    BranchEquipment previewBranchEquipment(BranchEquipmentAssemblyRequest request);

    /**
     * Объединяет существующую branch equipment модель с новой моделью по merge-стратегии.
     *
     * <p>Метод применяется для сценариев patch/merge в уже существующий JSON уровня
     * {@code DeviceManager.json}, когда в целевые branch импортируется набор DTT.</p>
     *
     * @param existing существующая branch equipment модель
     * @param incoming новая branch equipment модель, собранная из DTT
     * @param mergeStrategy стратегия merge для конфликтов по {@code deviceTypeId}
     * @return объединённая модель branch equipment
     */
    BranchEquipment mergeBranchEquipment(BranchEquipment existing, BranchEquipment incoming, MergeStrategy mergeStrategy);
}
