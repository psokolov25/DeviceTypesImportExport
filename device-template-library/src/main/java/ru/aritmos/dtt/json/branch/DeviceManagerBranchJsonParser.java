package ru.aritmos.dtt.json.branch;

/**
 * Парсер branch equipment JSON в типизированную модель.
 */
public interface DeviceManagerBranchJsonParser {

    /**
     * @param json branch equipment JSON
     * @return типизированная модель отделений
     */
    BranchEquipment parse(String json);
}
