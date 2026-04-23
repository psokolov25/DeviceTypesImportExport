package ru.aritmos.dtt.api;

import ru.aritmos.dtt.api.dto.BatchDttExportResult;
import ru.aritmos.dtt.api.dto.DttVersionComparisonResult;
import ru.aritmos.dtt.api.dto.EquipmentProfileAssemblyRequest;
import ru.aritmos.dtt.api.dto.MergeStrategy;
import ru.aritmos.dtt.api.dto.ProfileExportRequest;
import ru.aritmos.dtt.api.dto.ProfileBranchAssemblyResult;
import ru.aritmos.dtt.api.dto.SingleDttExportPreviewResult;
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
     * Извлекает metadata типа устройства из одного `.dtt` или zip-набора `.dtt`.
     *
     * <p>Если payload является одиночным `.dtt`, возвращается список из одного элемента.
     * Если payload является zip-набором, возвращаются metadata всех `.dtt` entry в порядке обхода zip.
     *
     * @param payload bytes одиночного `.dtt` или zip-набора `.dtt`
     * @return список metadata типов устройств
     */
    List<ru.aritmos.dtt.api.dto.DeviceTypeMetadata> extractDeviceTypeMetadataFromDttOrZip(byte[] payload);

    /**
     * Возвращает базовое имя файла для DTT-архива на основе metadata типа устройства.
     *
     * @param archiveBytes bytes ZIP-архива `.dtt`
     * @param fallbackName fallback-имя, если metadata не содержит пригодного имени
     * @return базовое имя файла без расширения `.dtt`
     */
    String resolveDeviceTypeArchiveBaseName(byte[] archiveBytes, String fallbackName);

    /**
     * Сравнивает входную версию и версию шаблона, извлечённую из DTT-архива.
     *
     * @param archiveBytes bytes ZIP-архива
     * @param inputVersion версия из внешнего контекста (например, query-параметра API)
     * @return результат сравнения нормализованных версий
     */
    DttVersionComparisonResult compareDttVersion(byte[] archiveBytes, String inputVersion);

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
     * Объединяет две branch equipment модели по выбранной merge-стратегии.
     *
     * @param existing существующая branch equipment модель
     * @param incoming входящая branch equipment модель
     * @param mergeStrategy стратегия merge при конфликте deviceTypeId внутри branch
     * @return объединённая branch equipment модель
     */
    BranchEquipment mergeBranchEquipment(BranchEquipment existing, BranchEquipment incoming, MergeStrategy mergeStrategy);


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
     * Выполняет preview single-export из profile-модели в `.dtt`.
     *
     * @param profile профиль оборудования
     * @param deviceTypeId идентификатор типа устройства для экспорта
     * @param dttVersion опциональная версия шаблона
     * @return диагностичный результат preview без выброса исключения наружу
     */
    SingleDttExportPreviewResult previewSingleDttExportFromProfile(EquipmentProfile profile, String deviceTypeId, String dttVersion);

    /**
     * Выполняет preview single-export из branch equipment модели в `.dtt`.
     *
     * @param branchEquipment branch equipment модель
     * @param branchIds опциональный фильтр branchId
     * @param deviceTypeId идентификатор типа устройства для экспорта
     * @param mergeStrategy стратегия merge конфликтов между branch
     * @param dttVersion опциональная версия шаблона
     * @return диагностичный результат preview без выброса исключения наружу
     */
    SingleDttExportPreviewResult previewSingleDttExportFromBranch(BranchEquipment branchEquipment,
                                                                   List<String> branchIds,
                                                                   String deviceTypeId,
                                                                   MergeStrategy mergeStrategy,
                                                                   String dttVersion);

    /**
     * Выполняет preview single-export из строкового profile JSON в `.dtt`.
     *
     * @param profileJson строковое представление profile JSON
     * @param deviceTypeId идентификатор типа устройства
     * @param dttVersion опциональная версия шаблона
     * @return диагностичный результат preview
     */
    SingleDttExportPreviewResult previewSingleDttExportFromProfileJson(String profileJson, String deviceTypeId, String dttVersion);

    /**
     * Выполняет preview single-export из строкового branch equipment JSON в `.dtt`.
     *
     * @param branchJson строковое представление branch equipment JSON
     * @param branchIds опциональный фильтр branchId
     * @param deviceTypeId идентификатор типа устройства
     * @param mergeStrategy стратегия merge конфликтов между branch
     * @param dttVersion опциональная версия шаблона
     * @return диагностичный результат preview
     */
    SingleDttExportPreviewResult previewSingleDttExportFromBranchJson(String branchJson,
                                                                       List<String> branchIds,
                                                                       String deviceTypeId,
                                                                       MergeStrategy mergeStrategy,
                                                                       String dttVersion);

    /**
     * Экспортирует один тип устройства из profile-модели в бинарный `.dtt`.
     *
     * @param profile профиль оборудования
     * @param deviceTypeId идентификатор типа устройства
     * @param dttVersion опциональная версия шаблона
     * @return бинарный `.dtt`
     */
    byte[] exportSingleDttFromProfile(EquipmentProfile profile, String deviceTypeId, String dttVersion);

    /**
     * Экспортирует один тип устройства из строкового profile JSON в бинарный `.dtt`.
     *
     * @param profileJson строковое представление profile JSON
     * @param deviceTypeId идентификатор типа устройства
     * @param dttVersion опциональная версия шаблона
     * @return бинарный `.dtt`
     */
    byte[] exportSingleDttFromProfileJson(String profileJson, String deviceTypeId, String dttVersion);

    /**
     * Экспортирует один тип устройства из branch equipment модели в бинарный `.dtt`.
     *
     * @param branchEquipment branch equipment модель
     * @param branchIds опциональный фильтр branchId
     * @param deviceTypeId идентификатор типа устройства
     * @param mergeStrategy стратегия merge конфликтов между branch
     * @param dttVersion опциональная версия шаблона
     * @return бинарный `.dtt`
     */
    byte[] exportSingleDttFromBranch(BranchEquipment branchEquipment,
                                     List<String> branchIds,
                                     String deviceTypeId,
                                     MergeStrategy mergeStrategy,
                                     String dttVersion);

    /**
     * Экспортирует один тип устройства из строкового branch equipment JSON в бинарный `.dtt`.
     *
     * @param branchJson строковое представление branch equipment JSON
     * @param branchIds опциональный фильтр branchId
     * @param deviceTypeId идентификатор типа устройства
     * @param mergeStrategy стратегия merge конфликтов между branch
     * @param dttVersion опциональная версия шаблона
     * @return бинарный `.dtt`
     */
    byte[] exportSingleDttFromBranchJson(String branchJson,
                                         List<String> branchIds,
                                         String deviceTypeId,
                                         MergeStrategy mergeStrategy,
                                         String dttVersion);

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
     * Импортирует набор DTT-архивов в уже существующий branch equipment JSON из Base64-представления,
     * где существующая branch-модель передаётся строковым JSON.
     */
    BranchEquipment importDttBase64SetToExistingBranchJson(List<String> archivesBase64,
                                                           String existingBranchJson,
                                                           List<String> branchIds,
                                                           MergeStrategy mergeStrategy);

    /**
     * Выполняет preview-сборку branch equipment из Base64-представления набора DTT-архивов.
     */
    BranchEquipment previewDttBase64SetToBranch(List<String> archivesBase64, List<String> branchIds, MergeStrategy mergeStrategy);

    /**
     * Читает zip-пакет и извлекает из него все `.dtt` файлы с сохранением исходных имён entry.
     *
     * @param zipPayload zip-пакет с одним или несколькими `.dtt`
     * @return map `entryName -> archiveBytes` в порядке обхода entry внутри zip
     */
    Map<String, byte[]> readDttFilesFromZipByEntryName(byte[] zipPayload);

    /**
     * Разрешает конкретный `.dtt` entry из ранее распакованного zip-пакета.
     *
     * <p>Поддерживается как точное имя entry, так и поиск по нормализованному имени файла
     * (без пути и без расширения `.dtt`, регистронезависимо).
     *
     * @param archivesByEntryName map `entryName -> archiveBytes`
     * @param archiveEntryName имя требуемого entry
     * @return содержимое найденного `.dtt` архива
     */
    byte[] resolveDttArchiveEntry(Map<String, byte[]> archivesByEntryName, String archiveEntryName);

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
     * Импортирует zip-архив с `.dtt` в уже существующий branch equipment JSON,
     * где существующая branch-модель передаётся строковым JSON.
     */
    BranchEquipment importDttZipToExistingBranchJson(byte[] zipPayload,
                                                     String existingBranchJson,
                                                     List<String> branchIds,
                                                     MergeStrategy mergeStrategy);

    /**
     * Выполняет preview-сборку branch equipment из zip-архива с `.dtt` файлами.
     */
    BranchEquipment previewDttZipToBranch(byte[] zipPayload, List<String> branchIds, MergeStrategy mergeStrategy);


    /**
     * Импортирует один или несколько DTT одновременно в profile JSON и branch equipment JSON c поддержкой metadata override.
     *
     * <p>Metadata для branch наследуются от metadata profile и могут быть переопределены
     * через {@code branchMetadataOverridesByBranchIdAndDeviceTypeId}.
     *
     * @param archives bytes-архивы DTT
     * @param branchIds список branch, для которых строится branch equipment
     * @param profileMetadataOverridesByDeviceTypeId metadata override уровня profile по ключу deviceTypeId
     * @param branchMetadataOverridesByBranchIdAndDeviceTypeId metadata override уровня branch
     *                                                         по ключам branchId -> deviceTypeId
     * @param mergeStrategy стратегия merge
     * @return профиль и branch equipment в одном результате
     */
    ProfileBranchAssemblyResult importDttSetToProfileAndBranchWithMetadata(List<byte[]> archives,
                                                                            List<String> branchIds,
                                                                            Map<String, ru.aritmos.dtt.api.dto.DeviceTypeMetadata> profileMetadataOverridesByDeviceTypeId,
                                                                            Map<String, Map<String, ru.aritmos.dtt.api.dto.DeviceTypeMetadata>> branchMetadataOverridesByBranchIdAndDeviceTypeId,
                                                                            MergeStrategy mergeStrategy);

    /**
     * Экспортирует набор DTT-архивов профиля в zip-представление.
     */
    byte[] exportProfileToDttZip(ProfileExportRequest request);

    /**
     * Экспортирует набор DTT-архивов из строкового profile JSON в zip-представление.
     *
     * @param profileJson строковое представление profile JSON
     * @param deviceTypeIds опциональный фильтр deviceTypeId
     * @param dttVersion опциональная версия шаблона
     * @return zip-представление набора `.dtt`
     */
    byte[] exportProfileToDttZip(String profileJson, List<String> deviceTypeIds, String dttVersion);

    /**
     * Экспортирует набор DTT-архивов branch equipment в zip-представление.
     */
    byte[] exportBranchToDttZip(BranchEquipmentExportRequest request);

    /**
     * Экспортирует набор DTT-архивов из строкового branch equipment JSON в zip-представление.
     *
     * @param branchJson строковое представление branch equipment JSON
     * @param branchIds опциональный фильтр branchId
     * @param deviceTypeIds опциональный фильтр deviceTypeId
     * @param mergeStrategy стратегия merge конфликтов между branch
     * @param dttVersion опциональная версия шаблона
     * @return zip-представление набора `.dtt`
     */
    byte[] exportBranchToDttZip(String branchJson,
                                List<String> branchIds,
                                List<String> deviceTypeIds,
                                MergeStrategy mergeStrategy,
                                String dttVersion);

    /**
     * Экспортирует набор DTT-архивов профиля в zip-представление и кодирует его в Base64.
     */
    String exportProfileToDttZipBase64(ProfileExportRequest request);

    /**
     * Экспортирует набор DTT-архивов branch equipment в zip-представление и кодирует его в Base64.
     */
    String exportBranchToDttZipBase64(BranchEquipmentExportRequest request);
}
