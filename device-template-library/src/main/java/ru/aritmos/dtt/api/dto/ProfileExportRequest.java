package ru.aritmos.dtt.api.dto;

import ru.aritmos.dtt.json.profile.EquipmentProfile;

import java.util.List;

/**
 * Запрос на экспорт набора DTT-архивов из profile JSON модели.
 *
 * @param profile исходная модель профиля оборудования (карта deviceTypes)
 * @param deviceTypeIds опциональный список deviceTypeId для выборочного экспорта;
 *                      если null/пустой — экспортируются все типы
 * @param dttVersion опциональная версия шаблона, которая фиксируется в DTT и добавляется в описание типа
 */
public record ProfileExportRequest(
        EquipmentProfile profile,
        List<String> deviceTypeIds,
        String dttVersion
) {
}
