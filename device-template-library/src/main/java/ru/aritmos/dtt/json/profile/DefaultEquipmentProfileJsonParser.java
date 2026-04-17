package ru.aritmos.dtt.json.profile;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import ru.aritmos.dtt.api.dto.DeviceTypeTemplate;
import ru.aritmos.dtt.exception.DttFormatException;

import java.util.Map;

/**
 * Реализация парсера profile JSON в типизированную модель.
 */
public class DefaultEquipmentProfileJsonParser implements EquipmentProfileJsonParser {

    private static final TypeReference<Map<String, DeviceTypeTemplate>> PROFILE_MAP = new TypeReference<>() { };

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public EquipmentProfile parse(String json) {
        try {
            return new EquipmentProfile(objectMapper.readValue(json, PROFILE_MAP));
        } catch (JsonProcessingException exception) {
            throw new DttFormatException("Ошибка парсинга profile JSON", exception);
        }
    }
}
