package ru.aritmos.dtt.model.canonical;

import java.util.Map;

/**
 * Канонический набор Groovy-скриптов шаблона типа устройства.
 *
 * @param onStartEvent lifecycle script старта
 * @param onStopEvent lifecycle script остановки
 * @param onPublicStartEvent публичный script старта
 * @param onPublicFinishEvent публичный script завершения
 * @param deviceTypeFunctions набор функций типа устройства
 * @param eventHandlers обработчики событий
 * @param commands скрипты команд
 */
public record CanonicalScriptSet(
        String onStartEvent,
        String onStopEvent,
        String onPublicStartEvent,
        String onPublicFinishEvent,
        String deviceTypeFunctions,
        Map<String, String> eventHandlers,
        Map<String, String> commands
) {
}
