package ru.aritmos.dtt.json.branch;

/**
 * Генератор JSON-представления branch equipment модели.
 */
public interface DeviceManagerBranchJsonGenerator {

    /**
     * @param branchEquipment модель branch equipment
     * @return JSON строка
     */
    String generate(BranchEquipment branchEquipment);
}
