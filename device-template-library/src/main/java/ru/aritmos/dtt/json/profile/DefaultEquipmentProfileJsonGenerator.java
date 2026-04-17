package ru.aritmos.dtt.json.profile;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import ru.aritmos.dtt.exception.DttFormatException;

/**
 * Реализация генератора JSON профиля оборудования.
 */
public class DefaultEquipmentProfileJsonGenerator implements EquipmentProfileJsonGenerator {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public String generate(EquipmentProfile profile) {
        try {
            return objectMapper.writeValueAsString(profile.deviceTypes());
        } catch (JsonProcessingException exception) {
            throw new DttFormatException("Ошибка генерации profile JSON", exception);
        }
    }
}
