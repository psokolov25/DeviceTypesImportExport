package ru.aritmos.dtt.json.branch;

import java.util.Map;

/**
 * Полный branch equipment JSON: карта отделений и их конфигураций.
 *
 * @param branches отделения с типами устройств и экземплярами
 */
public record BranchEquipment(Map<String, BranchNode> branches) {
}
