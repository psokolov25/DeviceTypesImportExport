package ru.aritmos.dtt.json.branch;

import java.util.List;
import java.util.Map;

/**
 * Каноническое описание script-секции в branch equipment JSON.
 *
 * <p>Используется для lifecycle-скриптов, обработчиков событий и команд в формате,
 * согласованном с {@code DeviceManager.json}: {@code inputParameters}, {@code outputParameters}, {@code scriptCode}.
 *
 * @param inputParameters входные параметры скрипта
 * @param outputParameters выходные параметры скрипта
 * @param scriptCode Groovy-код скрипта
 */
public record BranchScript(
        Map<String, Object> inputParameters,
        List<Object> outputParameters,
        String scriptCode
) {
}
