package ru.aritmos.dtt.json.branch;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import ru.aritmos.dtt.api.dto.DeviceTypeMetadata;
import ru.aritmos.dtt.api.dto.DeviceTypeTemplate;
import ru.aritmos.dtt.exception.DttFormatException;

import java.util.List;
import java.util.Map;

/**
 * Модель типа устройства в отделении с шаблонными параметрами и дочерними устройствами.
 *
 * @param template шаблон типа устройства
 * @param devices экземпляры устройств
 * @param kind тип устройства из канонического branch JSON (`type`)
 * @param onStartEvent секция скрипта onStartEvent в исходной модели branch
 * @param onStopEvent секция скрипта onStopEvent в исходной модели branch
 * @param onPublicStartEvent секция скрипта onPublicStartEvent в исходной модели branch
 * @param onPublicFinishEvent секция скрипта onPublicFinishEvent в исходной модели branch
 * @param deviceTypeFunctions скрипт функций типа устройства
 * @param eventHandlers секция обработчиков событий
 * @param commands секция команд
 */
public record BranchDeviceType(
        DeviceTypeTemplate template,
        Map<String, DeviceInstanceTemplate> devices,
        String kind,
        BranchScript onStartEvent,
        BranchScript onStopEvent,
        BranchScript onPublicStartEvent,
        BranchScript onPublicFinishEvent,
        String deviceTypeFunctions,
        Map<String, BranchScript> eventHandlers,
        Map<String, BranchScript> commands
) {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    /**
     * Поддерживает прямую десериализацию raw DeviceManager JSON в модель BranchDeviceType.
     *
     * <p>Это необходимо для REST-режима, когда branch equipment передаётся как JSON-объект
     * внутри поля {@code branchEquipment}, а не строкой {@code branchJson}. В исходном JSON
     * поля типа устройства имеют вид {@code id/name/displayName/description/type/...}, тогда как
     * внутренняя каноническая модель хранит их внутри поля {@code template}. Без этого фабричного
     * метода Jackson создаёт объект с {@code template == null}, что позже приводит к NPE при экспорте.
     *
     * @param raw raw JSON-объект типа устройства из DeviceManager.json
     * @return канонический BranchDeviceType
     */
    @JsonCreator(mode = JsonCreator.Mode.DELEGATING)
    public static BranchDeviceType fromJson(Map<String, Object> raw) {
        if (raw == null) {
            return null;
        }
        final String id = asString(raw.get("id"));
        final String name = asString(raw.get("name"));
        final String displayName = asString(raw.get("displayName"));
        final String description = asString(raw.get("description"));
        final String kind = asString(raw.get("type"));
        if (kind == null || kind.isBlank()) {
            throw new DttFormatException("Некорректный формат поля type: ожидается непустая строка");
        }
        final String resolvedId = (id == null || id.isBlank()) ? name : id;
        final String resolvedName = (name == null || name.isBlank()) ? resolvedId : name;
        final String resolvedDisplayName = (displayName == null || displayName.isBlank()) ? resolvedName : displayName;
        final DeviceTypeMetadata metadata = new DeviceTypeMetadata(
                resolvedId,
                resolvedName,
                resolvedDisplayName,
                description == null ? "" : description
        );
        final Map<String, Object> deviceTypeParamValues = convertMap(raw.get("deviceTypeParamValues"));
        final DeviceTypeTemplate template = new DeviceTypeTemplate(metadata, deviceTypeParamValues);
        final Map<String, DeviceInstanceTemplate> devices = OBJECT_MAPPER.convertValue(
                raw.get("devices"),
                new TypeReference<Map<String, DeviceInstanceTemplate>>() { }
        );
        final Map<String, BranchScript> eventHandlers = OBJECT_MAPPER.convertValue(
                raw.get("eventHandlers"),
                new TypeReference<Map<String, BranchScript>>() { }
        );
        final Map<String, BranchScript> commands = OBJECT_MAPPER.convertValue(
                raw.get("commands"),
                new TypeReference<Map<String, BranchScript>>() { }
        );
        final BranchScript onStartEvent = OBJECT_MAPPER.convertValue(raw.get("onStartEvent"), BranchScript.class);
        final BranchScript onStopEvent = OBJECT_MAPPER.convertValue(raw.get("onStopEvent"), BranchScript.class);
        final BranchScript onPublicStartEvent = OBJECT_MAPPER.convertValue(raw.get("onPublicStartEvent"), BranchScript.class);
        final BranchScript onPublicFinishEvent = OBJECT_MAPPER.convertValue(raw.get("onPublicFinishEvent"), BranchScript.class);
        final String deviceTypeFunctions = asString(raw.get("deviceTypeFunctions"));
        return new BranchDeviceType(
                template,
                devices == null ? Map.of() : devices,
                kind,
                onStartEvent,
                onStopEvent,
                onPublicStartEvent,
                onPublicFinishEvent,
                deviceTypeFunctions,
                eventHandlers == null ? Map.of() : eventHandlers,
                commands == null ? Map.of() : commands
        );
    }

    private static Map<String, Object> convertMap(Object value) {
        final Map<String, Object> result = OBJECT_MAPPER.convertValue(
                value,
                new TypeReference<Map<String, Object>>() { }
        );
        return result == null ? Map.of() : result;
    }

    private static String asString(Object value) {
        return value == null ? null : value.toString();
    }
}
