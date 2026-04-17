package ru.aritmos.dtt.json.profile;

/**
 * Парсер JSON профиля оборудования (карта deviceTypes) в типизированную модель.
 */
public interface EquipmentProfileJsonParser {

    /**
     * @param json JSON профиля оборудования
     * @return типизированный профиль
     */
    EquipmentProfile parse(String json);
}
