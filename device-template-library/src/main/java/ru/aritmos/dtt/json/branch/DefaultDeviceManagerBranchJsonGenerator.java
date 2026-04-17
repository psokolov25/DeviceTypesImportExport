package ru.aritmos.dtt.json.branch;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import ru.aritmos.dtt.exception.DttFormatException;

/**
 * Реализация генератора branch equipment JSON через Jackson.
 */
public class DefaultDeviceManagerBranchJsonGenerator implements DeviceManagerBranchJsonGenerator {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public String generate(BranchEquipment branchEquipment) {
        try {
            return objectMapper.writeValueAsString(branchEquipment.branches());
        } catch (JsonProcessingException exception) {
            throw new DttFormatException("Ошибка генерации branch equipment JSON", exception);
        }
    }
}
