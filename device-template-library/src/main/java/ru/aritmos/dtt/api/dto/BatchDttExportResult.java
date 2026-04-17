package ru.aritmos.dtt.api.dto;

import java.util.Map;

/**
 * Результат пакетного экспорта набора DTT-архивов.
 *
 * @param archivesByDeviceTypeId архивы, сгруппированные по id типа устройства
 */
public record BatchDttExportResult(Map<String, byte[]> archivesByDeviceTypeId) {
}
