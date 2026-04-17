package ru.aritmos.dtt.json.branch;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import ru.aritmos.dtt.exception.DttFormatException;

import java.util.Map;

/**
 * Реализация парсера branch equipment JSON через Jackson.
 */
public class DefaultDeviceManagerBranchJsonParser implements DeviceManagerBranchJsonParser {

    private static final TypeReference<Map<String, BranchNode>> BRANCH_MAP = new TypeReference<>() { };

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public BranchEquipment parse(String json) {
        try {
            return new BranchEquipment(objectMapper.readValue(json, BRANCH_MAP));
        } catch (JsonProcessingException exception) {
            throw new DttFormatException("Ошибка парсинга branch equipment JSON", exception);
        }
    }
}
