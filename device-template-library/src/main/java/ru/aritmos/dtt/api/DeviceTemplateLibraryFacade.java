package ru.aritmos.dtt.api;

import ru.aritmos.dtt.api.dto.BatchDttExportResult;
import ru.aritmos.dtt.api.dto.EquipmentProfileAssemblyRequest;
import ru.aritmos.dtt.api.dto.MergeStrategy;
import ru.aritmos.dtt.api.dto.ValidationResult;
import ru.aritmos.dtt.api.dto.branch.BranchEquipmentAssemblyRequest;
import ru.aritmos.dtt.archive.model.DttArchiveTemplate;
import ru.aritmos.dtt.json.branch.BranchEquipment;
import ru.aritmos.dtt.json.profile.EquipmentProfile;

import java.util.List;

/**
 * Фасад библиотеки DTT для использования как внешней зависимости в приложениях.
 *
 * Объединяет ключевые сценарии чтения/записи архива, валидации и сборки profile/branch моделей через единый API.
 */
public interface DeviceTemplateLibraryFacade {

    /**
     * Читает бинарный DTT архив.
     *
     * @param archiveBytes bytes ZIP-архива
     * @return archive DTO шаблона
     */
    DttArchiveTemplate readDtt(byte[] archiveBytes);

    /**
     * Пишет DTT шаблон в бинарный архив.
     *
     * @param template archive DTO шаблона
     * @return bytes ZIP-архива
     */
    byte[] writeDtt(DttArchiveTemplate template);

    /**
     * Валидирует шаблон DTT.
     *
     * @param template archive DTO шаблона
     * @return результат валидации
     */
    ValidationResult validate(DttArchiveTemplate template);

    /**
     * Валидирует бинарный DTT архив.
     *
     * @param archiveBytes bytes ZIP-архива
     * @return результат валидации
     */
    ValidationResult validate(byte[] archiveBytes);

    /**
     * Собирает профиль оборудования из DTT.
     *
     * @param request параметры сборки профиля
     * @return модель профиля
     */
    EquipmentProfile assembleProfile(EquipmentProfileAssemblyRequest request);

    /**
     * Собирает branch equipment из DTT.
     *
     * @param request параметры сборки отделений
     * @return модель branch equipment
     */
    BranchEquipment assembleBranch(BranchEquipmentAssemblyRequest request);

    /**
     * Генерирует JSON профиля оборудования.
     *
     * @param profile модель профиля
     * @return JSON
     */
    String toProfileJson(EquipmentProfile profile);

    /**
     * Генерирует JSON branch equipment.
     *
     * @param branchEquipment модель branch equipment
     * @return JSON
     */
    String toBranchJson(BranchEquipment branchEquipment);

    /**
     * Экспортирует все типы устройств из profile модели в набор DTT-архивов.
     *
     * @param profile профиль оборудования
     * @return архивы по id типа устройства
     */
    BatchDttExportResult exportDttSetFromProfile(EquipmentProfile profile);

    /**
     * Импортирует набор DTT-архивов в profile модель оборудования.
     *
     * @param archives bytes-архивы DTT
     * @param mergeStrategy стратегия разрешения конфликтов типов
     * @return собранный профиль оборудования
     */
    EquipmentProfile importDttSetToProfile(List<byte[]> archives, MergeStrategy mergeStrategy);
}

