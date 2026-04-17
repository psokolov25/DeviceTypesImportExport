package ru.aritmos.dtt.json.branch;

import java.util.Map;

/**
 * Модель одного отделения в branch equipment JSON.
 *
 * @param id идентификатор отделения
 * @param displayName отображаемое имя
 * @param deviceTypes карта типов устройств отделения
 */
public record BranchNode(String id, String displayName, Map<String, BranchDeviceType> deviceTypes) {
}
