package ru.aritmos.dtt.json.profile;

/**
 * Генератор JSON для модели профиля оборудования (карта deviceTypes).
 */
public interface EquipmentProfileJsonGenerator {

    /**
     * @param profile профиль оборудования
     * @return JSON строка карты deviceTypes
     */
    String generate(EquipmentProfile profile);
}
