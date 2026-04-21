package ru.aritmos.dtt.json.branch;

import java.util.Map;

/**
 * Полный branch equipment JSON: карта отделений и их конфигураций.
 *
 * @param branches отделения с типами устройств и экземплярами
 * @param metadata агрегированный список метаданных типов устройств
 */
public record BranchEquipment(Map<String, BranchNode> branches, java.util.List<BranchDeviceTypeMetadata> metadata) {

    /**
     * Конструктор обратной совместимости для существующих вызовов.
     *
     * @param branches карта отделений
     */
    public BranchEquipment(Map<String, BranchNode> branches) {
        this(branches, java.util.List.of());
    }
}
