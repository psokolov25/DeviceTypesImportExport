package ru.aritmos.dtt.api.dto;

import java.util.List;

/**
 * Запрос на сборку JSON профиля оборудования из одного или нескольких DTT.
 *
 * @param deviceTypes список типов устройств для сборки карты {@code deviceTypes}
 * @param overrides явные override-значения параметров
 * @param mergeStrategy стратегия разрешения конфликтов при совпадении идентификаторов
 */
public record EquipmentProfileAssemblyRequest(
        List<EquipmentProfileDeviceTypeRequest> deviceTypes,
        List<TemplateValueOverride> overrides,
        MergeStrategy mergeStrategy
) {
}
