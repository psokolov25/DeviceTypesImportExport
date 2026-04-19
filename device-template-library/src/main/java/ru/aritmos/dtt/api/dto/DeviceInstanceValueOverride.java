package ru.aritmos.dtt.api.dto;

import java.util.Map;

/**
 * Override-значения параметров конкретного экземпляра устройства.
 *
 * <p>DTO используется в сценариях сборки profile/branch, когда необходимо
 * передать явные пользовательские значения для конкретного устройства.</p>
 *
 * @param deviceTypeId идентификатор типа устройства
 * @param deviceId идентификатор экземпляра устройства
 * @param values карта override-значений параметров устройства
 */
public record DeviceInstanceValueOverride(
        String deviceTypeId,
        String deviceId,
        Map<String, Object> values
) {
}
