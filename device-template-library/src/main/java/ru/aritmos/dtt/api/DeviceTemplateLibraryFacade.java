package ru.aritmos.dtt.api;

import ru.aritmos.dtt.api.dto.BatchDttExportResult;
import ru.aritmos.dtt.api.dto.EquipmentProfileAssemblyRequest;
import ru.aritmos.dtt.api.dto.MergeStrategy;
import ru.aritmos.dtt.api.dto.ProfileExportRequest;
import ru.aritmos.dtt.api.dto.ValidationResult;
import ru.aritmos.dtt.api.dto.branch.BranchEquipmentAssemblyRequest;
import ru.aritmos.dtt.api.dto.branch.BranchEquipmentExportRequest;
import ru.aritmos.dtt.archive.model.DttArchiveTemplate;
import ru.aritmos.dtt.json.branch.BranchEquipment;
import ru.aritmos.dtt.json.profile.EquipmentProfile;

import java.util.Map;
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
     * Парсит JSON профиля оборудования в типизированную модель.
     *
     * @param json profile JSON
     * @return модель профиля
     */
    EquipmentProfile parseProfileJson(String json);

    /**
     * Парсит JSON оборудования отделений в типизированную модель.
     *
     * @param json branch equipment JSON
     * @return модель branch equipment
     */
    BranchEquipment parseBranchJson(String json);

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
     * Экспортирует набор DTT-архивов из profile модели по параметрам запроса.
     *
     * @param request запрос с profile моделью и фильтром по deviceTypeId
     * @return архивы по id типа устройства
     */
    BatchDttExportResult exportDttSetFromProfile(ProfileExportRequest request);

    /**
     * Экспортирует набор DTT-архивов из profile модели в Base64-представлении.
     *
     * @param request запрос с profile моделью и фильтром по deviceTypeId
     * @return архивы по id типа устройства в Base64
     */
    Map<String, String> exportDttSetFromProfileBase64(ProfileExportRequest request);

    /**
     * Экспортирует набор DTT-архивов из строкового profile JSON с явной версией шаблона типа устройства.
     *
     * @param profileJson строковое представление карты deviceTypes
     * @param deviceTypeIds опциональный фильтр deviceTypeId
     * @param dttVersion версия шаблона типа устройства, фиксируемая в DTT
     * @return архивы по id типа устройства
     */
    BatchDttExportResult exportDttSetFromProfileJson(String profileJson, List<String> deviceTypeIds, String dttVersion);

    /**
     * Импортирует набор DTT-архивов в profile модель оборудования.
     *
     * @param archives bytes-архивы DTT
     * @param mergeStrategy стратегия разрешения конфликтов типов
     * @return собранный профиль оборудования
     */
    EquipmentProfile importDttSetToProfile(List<byte[]> archives, MergeStrategy mergeStrategy);

    /**
     * Выполняет preview-сборку profile JSON из набора DTT-архивов без сохранения результата.
     *
     * @param archives bytes-архивы DTT
     * @param mergeStrategy стратегия разрешения конфликтов типов
     * @return рассчитанный профиль оборудования
     */
    EquipmentProfile previewDttSetToProfile(List<byte[]> archives, MergeStrategy mergeStrategy);

    /**
     * Экспортирует все типы устройств из branch equipment модели в набор DTT-архивов.
     *
     * @param branchEquipment branch equipment с одной или несколькими branch
     * @param mergeStrategy стратегия разрешения конфликтов при повторении deviceTypeId между branch
     * @return архивы по id типа устройства
     */
    BatchDttExportResult exportDttSetFromBranch(BranchEquipment branchEquipment, MergeStrategy mergeStrategy);

    /**
     * Экспортирует набор DTT-архивов из branch equipment модели по параметрам запроса.
     *
     * @param request запрос с branch моделью и опциональными фильтрами
     * @return архивы по id типа устройства
     */
    BatchDttExportResult exportDttSetFromBranch(BranchEquipmentExportRequest request);

    /**
     * Экспортирует набор DTT-архивов из branch equipment модели в Base64-представлении.
     *
     * @param request запрос с branch моделью и опциональными фильтрами
     * @return архивы по id типа устройства в Base64
     */
    Map<String, String> exportDttSetFromBranchBase64(BranchEquipmentExportRequest request);

    /**
     * Экспортирует набор DTT-архивов из строкового branch equipment JSON с явной версией шаблона типа устройства.
     *
     * @param branchJson строковое представление branch equipment JSON
     * @param branchIds опциональный фильтр branchId
     * @param deviceTypeIds опциональный фильтр deviceTypeId
     * @param mergeStrategy стратегия merge конфликтов между branch
     * @param dttVersion версия шаблона типа устройства, фиксируемая в DTT
     * @return архивы по id типа устройства
     */
    BatchDttExportResult exportDttSetFromBranchJson(String branchJson,
                                                    List<String> branchIds,
                                                    List<String> deviceTypeIds,
                                                    MergeStrategy mergeStrategy,
                                                    String dttVersion);

    /**
     * Импортирует набор DTT-архивов в branch equipment модель для заданных отделений.
     *
     * @param archives bytes-архивы DTT
     * @param branchIds идентификаторы отделений, в которые нужно импортировать все переданные типы
     * @param mergeStrategy стратегия разрешения конфликтов типов внутри каждого отделения
     * @return собранный branch equipment
     */
    BranchEquipment importDttSetToBranch(List<byte[]> archives, List<String> branchIds, MergeStrategy mergeStrategy);

    /**
     * Импортирует набор DTT-архивов в уже существующую branch equipment модель.
     *
     * @param archives bytes-архивы DTT
     * @param existingBranchEquipment существующая модель оборудования отделений
     * @param branchIds идентификаторы отделений, в которые нужно импортировать типы из архивов
     * @param mergeStrategy стратегия merge при конфликте deviceTypeId внутри branch
     * @return объединённая branch equipment модель
     */
    BranchEquipment importDttSetToExistingBranch(List<byte[]> archives,
                                                 BranchEquipment existingBranchEquipment,
                                                 List<String> branchIds,
                                                 MergeStrategy mergeStrategy);

    /**
     * Выполняет preview-сборку branch equipment из набора DTT-архивов без сохранения результата.
     *
     * @param archives bytes-архивы DTT
     * @param branchIds идентификаторы отделений, в которые нужно импортировать все переданные типы
     * @param mergeStrategy стратегия разрешения конфликтов типов внутри каждого отделения
     * @return рассчитанная branch equipment модель
     */
    BranchEquipment previewDttSetToBranch(List<byte[]> archives, List<String> branchIds, MergeStrategy mergeStrategy);

    /**
     * Импортирует набор DTT-архивов в profile модель из Base64-представления.
     */
    EquipmentProfile importDttBase64SetToProfile(List<String> archivesBase64, MergeStrategy mergeStrategy);

    /**
     * Выполняет preview-сборку profile JSON из Base64-представления набора DTT-архивов.
     */
    EquipmentProfile previewDttBase64SetToProfile(List<String> archivesBase64, MergeStrategy mergeStrategy);

    /**
     * Импортирует набор DTT-архивов в branch equipment модель из Base64-представления.
     */
    BranchEquipment importDttBase64SetToBranch(List<String> archivesBase64, List<String> branchIds, MergeStrategy mergeStrategy);

    /**
     * Импортирует набор DTT-архивов в уже существующую branch equipment модель из Base64-представления.
     */
    BranchEquipment importDttBase64SetToExistingBranch(List<String> archivesBase64,
                                                       BranchEquipment existingBranchEquipment,
                                                       List<String> branchIds,
                                                       MergeStrategy mergeStrategy);

    /**
     * Выполняет preview-сборку branch equipment из Base64-представления набора DTT-архивов.
     */
    BranchEquipment previewDttBase64SetToBranch(List<String> archivesBase64, List<String> branchIds, MergeStrategy mergeStrategy);

    /**
     * Импортирует zip-архив с .dtt файлами в profile модель.
     */
    EquipmentProfile importDttZipToProfile(byte[] zipPayload, MergeStrategy mergeStrategy);

    /**
     * Выполняет preview-сборку profile JSON из zip-архива с `.dtt` файлами.
     */
    EquipmentProfile previewDttZipToProfile(byte[] zipPayload, MergeStrategy mergeStrategy);

    /**
     * Импортирует zip-архив с .dtt файлами в branch equipment модель.
     */
    BranchEquipment importDttZipToBranch(byte[] zipPayload, List<String> branchIds, MergeStrategy mergeStrategy);

    /**
     * Импортирует zip-архив с .dtt файлами в уже существующую branch equipment модель.
     */
    BranchEquipment importDttZipToExistingBranch(byte[] zipPayload,
                                                 BranchEquipment existingBranchEquipment,
                                                 List<String> branchIds,
                                                 MergeStrategy mergeStrategy);

    /**
     * Выполняет preview-сборку branch equipment из zip-архива с `.dtt` файлами.
     */
    BranchEquipment previewDttZipToBranch(byte[] zipPayload, List<String> branchIds, MergeStrategy mergeStrategy);

    /**
     * Экспортирует набор DTT-архивов профиля в zip-представление.
     */
    byte[] exportProfileToDttZip(ProfileExportRequest request);

    /**
     * Экспортирует набор DTT-архивов branch equipment в zip-представление.
     */
    byte[] exportBranchToDttZip(BranchEquipmentExportRequest request);

    /**
     * Экспортирует набор DTT-архивов профиля в zip-представление и кодирует его в Base64.
     */
    String exportProfileToDttZipBase64(ProfileExportRequest request);

    /**
     * Экспортирует набор DTT-архивов branch equipment в zip-представление и кодирует его в Base64.
     */
    String exportBranchToDttZipBase64(BranchEquipmentExportRequest request);
}
