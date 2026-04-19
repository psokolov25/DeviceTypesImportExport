package ru.aritmos.dtt.api.dto;

import java.util.Map;

/**
 * Набор Groovy-скриптов шаблона типа устройства.
 *
 * <p>DTO агрегирует lifecycle-скрипты, функции типа устройства, обработчики событий
 * и команды в единой структуре публичного API.</p>
 *
 * @param onStartEvent код lifecycle-скрипта onStartEvent
 * @param onStopEvent код lifecycle-скрипта onStopEvent
 * @param onPublicStartEvent код lifecycle-скрипта onPublicStartEvent
 * @param onPublicFinishEvent код lifecycle-скрипта onPublicFinishEvent
 * @param deviceTypeFunctions код блока функций типа устройства
 * @param eventHandlers карта обработчиков событий (eventName -> scriptCode)
 * @param commands карта команд (commandName -> scriptCode)
 */
public record TemplateScriptSet(
        String onStartEvent,
        String onStopEvent,
        String onPublicStartEvent,
        String onPublicFinishEvent,
        String deviceTypeFunctions,
        Map<String, String> eventHandlers,
        Map<String, String> commands
) {
}
