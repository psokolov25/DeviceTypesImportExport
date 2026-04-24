package ru.aritmos.dtt.demo.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.inject.Singleton;
import ru.aritmos.dtt.api.DeviceTemplateLibraryFacade;
import ru.aritmos.dtt.api.dto.DeviceTypeMetadata;
import ru.aritmos.dtt.api.dto.MergeStrategy;
import ru.aritmos.dtt.api.dto.ProfileBranchAssemblyResult;
import ru.aritmos.dtt.api.dto.ProfileExportRequest;
import ru.aritmos.dtt.api.dto.branch.BranchEquipmentExportRequest;
import ru.aritmos.dtt.archive.DttIconSupport;
import ru.aritmos.dtt.demo.dto.BranchPreviewDetailedResponse;
import ru.aritmos.dtt.demo.dto.DeviceTypeBasicMetadataResponse;
import ru.aritmos.dtt.demo.dto.DttInspectionResponse;
import ru.aritmos.dtt.demo.dto.DttMetadataBatchResponse;
import ru.aritmos.dtt.demo.dto.DttValidationIssueResponse;
import ru.aritmos.dtt.demo.dto.DttValidationResponse;
import ru.aritmos.dtt.demo.dto.DttVersionComparisonResponse;
import ru.aritmos.dtt.demo.dto.ExportAllDttFromBranchResponse;
import ru.aritmos.dtt.demo.dto.ExportAllDttFromProfileResponse;
import ru.aritmos.dtt.demo.dto.ExportSingleDttResponse;
import ru.aritmos.dtt.demo.dto.ImportDttSetToBranchRequest;
import ru.aritmos.dtt.demo.dto.ImportDttSetToBranchResponse;
import ru.aritmos.dtt.demo.dto.ImportDttSetToExistingBranchRequest;
import ru.aritmos.dtt.demo.dto.ImportDttSetToProfileRequest;
import ru.aritmos.dtt.demo.dto.ImportDttSetToProfileResponse;
import ru.aritmos.dtt.demo.dto.ImportDttZipToBranchUploadRequest;
import ru.aritmos.dtt.demo.dto.ImportDttZipToExistingBranchUploadRequest;
import ru.aritmos.dtt.demo.dto.ImportDttZipToProfileUploadRequest;
import ru.aritmos.dtt.demo.dto.ImportProfileBranchWithMetadataRequest;
import ru.aritmos.dtt.demo.dto.ImportProfileBranchWithMetadataResponse;
import ru.aritmos.dtt.demo.dto.ImportUploadedBranchDeviceTypeRequest;
import ru.aritmos.dtt.demo.dto.ImportUploadedBranchRequest;
import ru.aritmos.dtt.demo.dto.ImportUploadedProfileDeviceTypeRequest;
import ru.aritmos.dtt.demo.dto.ProfilePreviewDetailedResponse;
import ru.aritmos.dtt.demo.dto.SingleDttExportPreviewIssueResponse;
import ru.aritmos.dtt.demo.dto.SingleDttExportPreviewResponse;
import ru.aritmos.dtt.json.branch.BranchEquipment;
import ru.aritmos.dtt.json.profile.EquipmentProfile;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Сервис demo-модуля для валидации и инспекции DTT-архивов.
 */
@Singleton
public class DttDemoService {

    private final DeviceTemplateLibraryFacade facade;
    private final ImportPlanRequestMapper importPlanRequestMapper;
    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * Создаёт demo-сервис с явно переданным фасадом библиотеки.
     *
     * @param facade фасад библиотеки DTT
     */
    public DttDemoService(DeviceTemplateLibraryFacade facade) {
        this(facade, new ImportPlanRequestMapper());
    }

    /**
     * Создаёт demo-сервис с явно переданным фасадом и mapper-адаптером import-plan запросов.
     *
     * @param facade фасад библиотеки DTT
     * @param importPlanRequestMapper mapper transport DTO -> library import-plan DTO
     */
    public DttDemoService(DeviceTemplateLibraryFacade facade, ImportPlanRequestMapper importPlanRequestMapper) {
        this.facade = facade;
        this.importPlanRequestMapper = importPlanRequestMapper;
    }

    /**
     * @param archiveBytes бинарное содержимое DTT-архива
     * @return результат валидации
     */
    public DttValidationResponse validate(byte[] archiveBytes) {
        final var result = facade.validate(archiveBytes);
        final List<DttValidationIssueResponse> issues = result.issues().stream()
                .map(issue -> new DttValidationIssueResponse(issue.code(), issue.path(), issue.message()))
                .toList();
        return new DttValidationResponse(result.valid(), issues);
    }

    /**
     * @param archiveBytes бинарное содержимое DTT-архива
     * @return краткая инспекция архива
     */
    public DttInspectionResponse inspect(byte[] archiveBytes) {
        final var inspection = facade.inspectDtt(archiveBytes);
        return new DttInspectionResponse(
                inspection.formatName(),
                inspection.formatVersion(),
                inspection.deviceTypeId(),
                inspection.deviceTypeName(),
                inspection.deviceTypeVersion(),
                inspection.deviceTypeDescription(),
                DttIconSupport.resolveOrDefault(inspection.iconBase64()),
                inspection.eventHandlersCount(),
                inspection.commandsCount()
        );
    }

    /**
     * Извлекает базовые метаданные типов устройств из DTT-файла или zip-набора DTT.
     *
     * @param payload бинарный DTT или zip с DTT-файлами
     * @return список базовых метаданных типов устройств
     */
    public DttMetadataBatchResponse extractMetadata(byte[] payload) {
        final List<DeviceTypeBasicMetadataResponse> metadata = facade.normalizeDeviceTypeMetadata(
                        facade.extractDeviceTypeMetadataFromDttOrZip(payload)
                ).stream()
                .map(item -> new DeviceTypeBasicMetadataResponse(
                        item.id(),
                        item.name(),
                        item.displayName(),
                        item.version(),
                        item.description(),
                        item.iconBase64()
                ))
                .toList();
        return new DttMetadataBatchResponse(metadata);
    }

    /**
     * Сравнивает версию, переданную параметром, и версию из DTT-архива.
     *
     * @param archiveBytes бинарный DTT-архив
     * @param inputVersion версия из входного параметра
     * @return результат сравнения, включая источник большей версии
     */
    public DttVersionComparisonResponse compareDttVersion(byte[] archiveBytes, String inputVersion) {
        final var comparison = facade.compareDttVersion(archiveBytes, inputVersion);
        return new DttVersionComparisonResponse(
                comparison.inputVersion(),
                comparison.dttVersion(),
                comparison.greaterVersion(),
                comparison.greaterSource()
        );
    }


    /**
     * Возвращает человеко-читаемое имя типа устройства из DTT-архива.
     *
     * @param archiveBytes бинарное содержимое DTT-архива
     * @param fallbackName резервное имя
     * @return базовое имя файла без расширения
     */
    public String resolveDeviceTypeArchiveBaseName(byte[] archiveBytes, String fallbackName) {
        return facade.resolveDeviceTypeArchiveBaseName(archiveBytes, fallbackName);
    }

    /**
     * Импортирует набор DTT-архивов в profile JSON.
     *
     * @param archives список DTT-архивов
     * @param mergeStrategy стратегия разрешения конфликтов
     * @return результат импорта с JSON и фактическим количеством device types
     */
    public ImportDttSetToProfileResponse importDttSetToProfile(List<byte[]> archives, MergeStrategy mergeStrategy) {
        final var view = facade.importDttSetToProfileView(archives, mergeStrategy);
        return new ImportDttSetToProfileResponse(toJsonNode(view.profileJson()), view.deviceTypesCount());
    }

    /**
     * Импортирует набор DTT-архивов в profile JSON из Base64-представления.
     *
     * @param archivesBase64 список DTT-архивов в Base64
     * @param mergeStrategy стратегия разрешения конфликтов
     * @return результат импорта с JSON и фактическим количеством device types
     */
    public ImportDttSetToProfileResponse importDttSetToProfileBase64(List<String> archivesBase64, MergeStrategy mergeStrategy) {
        final var view = facade.importDttBase64SetToProfileView(archivesBase64, mergeStrategy);
        return new ImportDttSetToProfileResponse(toJsonNode(view.profileJson()), view.deviceTypesCount());
    }

    public ImportDttSetToProfileResponse importDttSetToProfile(ImportDttSetToProfileRequest request) {
        final var view = facade.assembleProfileApplyView(importPlanRequestMapper.toLibraryProfileImportPlan(request));
        return new ImportDttSetToProfileResponse(toJsonNode(view.profileJson()), view.deviceTypesCount());
    }

    public ImportDttSetToProfileResponse previewDttSetToProfile(ImportDttSetToProfileRequest request) {
        final var view = facade.previewProfileImportView(importPlanRequestMapper.toLibraryProfileImportPlan(request));
        return new ImportDttSetToProfileResponse(toJsonNode(view.profileJson()), view.deviceTypesCount());
    }

    public ProfilePreviewDetailedResponse previewProfileDetailed(ImportDttSetToProfileRequest request) {
        final var preview = facade.previewProfileImportView(importPlanRequestMapper.toLibraryProfileImportPlan(request));
        return new ProfilePreviewDetailedResponse(
                toJsonNode(preview.profileJson()),
                preview.computationsByDeviceType()
        );
    }


    /**
     * Одновременно собирает profile JSON и branch equipment JSON из DTT с metadata inheritance.
     */
    public ImportProfileBranchWithMetadataResponse importProfileAndBranchWithMetadata(ImportProfileBranchWithMetadataRequest request) {
        Objects.requireNonNull(request, "request must not be null");
        final ProfileBranchAssemblyResult result = facade.assembleProfileAndBranchWithMetadata(importPlanRequestMapper.toLibraryProfileBranchMetadataImportPlan(request));
        return new ImportProfileBranchWithMetadataResponse(
                toJsonNode(facade.toProfileJson(result.profile())),
                toJsonNode(facade.toBranchJson(result.branchEquipment()))
        );
    }


    /**
     * Экспортирует все DTT-архивы из profile JSON.
     *
     * @param profile модель profile JSON
     * @return Base64-представление экспортированных DTT-архивов
     */
    public ExportAllDttFromProfileResponse exportAllDttFromProfile(EquipmentProfile profile,
                                                                   List<String> deviceTypeIds,
                                                                   String dttVersion) {
        final var view = facade.exportDttSetFromProfileView(new ProfileExportRequest(profile, deviceTypeIds, dttVersion));
        return new ExportAllDttFromProfileResponse(view.archivesBase64ByDeviceTypeId(), view.archivesCount());
    }

    /**
     * Экспортирует все DTT-архивы из строкового profile JSON.
     *
     * @param profileJson строковое представление profile JSON
     * @param deviceTypeIds опциональный фильтр типов устройств
     * @return Base64-представление экспортированных DTT-архивов
     */
    public ExportAllDttFromProfileResponse exportAllDttFromProfileJson(JsonNode profileJson,
                                                                       List<String> deviceTypeIds,
                                                                       String dttVersion) {
        return exportAllDttFromProfile(facade.parseProfileJson(toCompactJson(profileJson)), deviceTypeIds, dttVersion);
    }

    /**
     * Экспортирует один DTT-архив из profile JSON.
     *
     * @param profile профиль оборудования
     * @param deviceTypeId идентификатор типа устройства
     * @param dttVersion опциональная версия шаблона
     * @return экспортированный архив в Base64
     */
    public ExportSingleDttResponse exportSingleDttFromProfile(EquipmentProfile profile,
                                                              String deviceTypeId,
                                                              String dttVersion) {
        final var export = facade.exportSingleDttResultFromProfile(profile, deviceTypeId, dttVersion);
        return new ExportSingleDttResponse(export.deviceTypeId(), export.archiveBase64());
    }

    /**
     * Экспортирует один DTT-архив из строкового profile JSON.
     */
    public ExportSingleDttResponse exportSingleDttFromProfileJson(JsonNode profileJson,
                                                                  String deviceTypeId,
                                                                  String dttVersion) {
        return exportSingleDttFromProfile(facade.parseProfileJson(toCompactJson(profileJson)), deviceTypeId, dttVersion);
    }

    /**
     * Экспортирует один DTT-архив из profile JSON как бинарный payload.
     */
    public byte[] exportSingleDttFromProfileToBytes(EquipmentProfile profile,
                                                    String deviceTypeId,
                                                    String dttVersion) {
        return facade.exportSingleDttResultFromProfile(profile, deviceTypeId, dttVersion).archiveBytes();
    }

    /**
     * Экспортирует один DTT-архив из строкового profile JSON как бинарный payload.
     */
    public byte[] exportSingleDttFromProfileJsonToBytes(JsonNode profileJson,
                                                        String deviceTypeId,
                                                        String dttVersion) {
        return facade.exportSingleDttResultFromProfileJson(toCompactJson(profileJson), deviceTypeId, dttVersion).archiveBytes();
    }

    /**
     * Выполняет preview single-export из profile JSON.
     */
    public SingleDttExportPreviewResponse previewSingleDttExportFromProfile(EquipmentProfile profile,
                                                                             String deviceTypeId,
                                                                             String dttVersion) {
        final var preview = facade.previewSingleDttExportFromProfile(profile, deviceTypeId, dttVersion);
        return toDemoPreview(preview);
    }

    /**
     * Выполняет preview single-export из строкового profile JSON.
     */
    public SingleDttExportPreviewResponse previewSingleDttExportFromProfileJson(JsonNode profileJson,
                                                                                 String deviceTypeId,
                                                                                 String dttVersion) {
        return toDemoPreview(facade.previewSingleDttExportFromProfileJson(toCompactJson(profileJson), deviceTypeId, dttVersion));
    }

    /**
     * Импортирует набор DTT-архивов в branch equipment JSON для заданных branch.
     *
     * @param archives список DTT-архивов
     * @param branchIds список branch, куда импортируются все типы
     * @param mergeStrategy стратегия разрешения конфликтов
     * @return branch JSON и количество отделений
     */
    public ImportDttSetToBranchResponse importDttSetToBranch(List<byte[]> archives,
                                                             List<String> branchIds,
                                                             MergeStrategy mergeStrategy) {
        return toBranchResponseView(facade.importDttSetToBranchView(archives, branchIds, mergeStrategy));
    }

    /**
     * Импортирует набор DTT-архивов в branch equipment JSON из Base64-представления.
     */
    public ImportDttSetToBranchResponse importDttSetToBranchBase64(List<String> archivesBase64,
                                                                   List<String> branchIds,
                                                                   MergeStrategy mergeStrategy) {
        return toBranchResponseView(facade.importDttBase64SetToBranchView(archivesBase64, branchIds, mergeStrategy));
    }

    /**
     * Импортирует набор DTT-архивов в уже существующий branch equipment JSON из Base64-представления.
     *
     * @param archivesBase64 список DTT-архивов в Base64
     * @param existingBranchJson исходный branch equipment JSON
     * @param branchIds branch назначения
     * @param mergeStrategy стратегия merge
     * @return branch JSON после merge-импорта и количество branch
     */
    public ImportDttSetToBranchResponse importDttSetToExistingBranchBase64(List<String> archivesBase64,
                                                                            String existingBranchJson,
                                                                            List<String> branchIds,
                                                                            MergeStrategy mergeStrategy) {
        return toBranchResponseView(facade.importDttBase64SetToExistingBranchJsonView(
                archivesBase64,
                existingBranchJson,
                branchIds,
                mergeStrategy
        ));
    }

    public ImportDttSetToBranchResponse importDttSetToBranch(ImportDttSetToBranchRequest request) {
        return toBranchResponseView(facade.assembleBranchApplyView(importPlanRequestMapper.toLibraryBranchImportPlan(request)));
    }

    public ImportDttSetToBranchResponse previewDttSetToBranch(ImportDttSetToBranchRequest request) {
        return toBranchResponseView(facade.previewBranchImportView(importPlanRequestMapper.toLibraryBranchImportPlan(request)));
    }

    public BranchPreviewDetailedResponse previewBranchDetailed(ImportDttSetToBranchRequest request) {
        final var preview = facade.previewBranchImportView(importPlanRequestMapper.toLibraryBranchImportPlan(request));
        return new BranchPreviewDetailedResponse(
                toJsonNode(preview.branchJson()),
                preview.computationsByTarget()
        );
    }

    public ImportDttSetToBranchResponse importDttSetToExistingBranch(ImportDttSetToExistingBranchRequest request) {
        return toBranchResponseView(facade.mergeIntoExistingBranchJsonApplyView(request.existingBranchJson(), importPlanRequestMapper.toLibraryBranchImportPlan(request)));
    }

    /**
     * Выполняет preview-сборку profile JSON из набора DTT-архивов без сохранения результата.
     *
     * @param archives список DTT-архивов
     * @param mergeStrategy стратегия разрешения конфликтов
     * @return рассчитанный preview profile JSON и количество типов устройств
     */
    public ImportDttSetToProfileResponse previewDttSetToProfile(List<byte[]> archives, MergeStrategy mergeStrategy) {
        final var view = facade.previewDttSetToProfileView(archives, mergeStrategy);
        return new ImportDttSetToProfileResponse(toJsonNode(view.profileJson()), view.deviceTypesCount());
    }

    /**
     * Выполняет preview-сборку profile JSON из Base64-представления набора DTT-архивов.
     */
    public ImportDttSetToProfileResponse previewDttSetToProfileBase64(List<String> archivesBase64, MergeStrategy mergeStrategy) {
        final var view = facade.previewDttBase64SetToProfileView(archivesBase64, mergeStrategy);
        return new ImportDttSetToProfileResponse(toJsonNode(view.profileJson()), view.deviceTypesCount());
    }

    /**
     * Выполняет preview-сборку branch equipment JSON из набора DTT-архивов без сохранения результата.
     *
     * @param archives список DTT-архивов
     * @param branchIds branch-ы назначения
     * @param mergeStrategy стратегия разрешения конфликтов
     * @return рассчитанный preview branch equipment JSON и количество отделений
     */
    public ImportDttSetToBranchResponse previewDttSetToBranch(List<byte[]> archives,
                                                              List<String> branchIds,
                                                              MergeStrategy mergeStrategy) {
        return toBranchResponseView(facade.previewDttSetToBranchView(archives, branchIds, mergeStrategy));
    }

    /**
     * Выполняет preview-сборку branch equipment JSON из Base64-представления набора DTT-архивов.
     */
    public ImportDttSetToBranchResponse previewDttSetToBranchBase64(List<String> archivesBase64,
                                                                    List<String> branchIds,
                                                                    MergeStrategy mergeStrategy) {
        return toBranchResponseView(facade.previewDttBase64SetToBranchView(archivesBase64, branchIds, mergeStrategy));
    }

    /**
     * Выполняет preview-сборку profile JSON из zip-набора DTT архивов.
     *
     * @param zipBytes zip-архив с файлами .dtt
     * @param mergeStrategy стратегия merge
     * @return рассчитанный preview profile JSON и количество типов устройств
     */
    public ImportDttSetToProfileResponse previewDttZipToProfile(byte[] zipBytes, MergeStrategy mergeStrategy) {
        final var view = facade.previewDttZipToProfileView(zipBytes, mergeStrategy);
        return new ImportDttSetToProfileResponse(toJsonNode(view.profileJson()), view.deviceTypesCount());
    }

    /**
     * Выполняет preview-сборку branch equipment JSON из zip-набора DTT архивов.
     *
     * @param zipBytes zip-архив с файлами .dtt
     * @param branchIds branch назначения
     * @param mergeStrategy стратегия merge
     * @return рассчитанный preview branch equipment JSON и количество отделений
     */
    public ImportDttSetToBranchResponse previewDttZipToBranch(byte[] zipBytes,
                                                              List<String> branchIds,
                                                              MergeStrategy mergeStrategy) {
        return toBranchResponseView(facade.previewDttZipToBranchView(zipBytes, branchIds, mergeStrategy));
    }

    /**
     * Импортирует zip-набор DTT архивов в profile JSON.
     *
     * @param zipBytes zip-архив с файлами .dtt
     * @param mergeStrategy стратегия merge
     * @return результат импорта профиля
     */
    public ImportDttSetToProfileResponse importDttZipToProfile(byte[] zipBytes, MergeStrategy mergeStrategy) {
        final var view = facade.importDttZipToProfileView(zipBytes, mergeStrategy);
        return new ImportDttSetToProfileResponse(toJsonNode(view.profileJson()), view.deviceTypesCount());
    }

    /**
     * Импортирует zip-набор DTT архивов в branch equipment JSON.
     *
     * @param zipBytes zip-архив с файлами .dtt
     * @param branchIds branch назначения
     * @param mergeStrategy стратегия merge
     * @return результат импорта branch equipment
     */
    public ImportDttSetToBranchResponse importDttZipToBranch(byte[] zipBytes,
                                                             List<String> branchIds,
                                                             MergeStrategy mergeStrategy) {
        return toBranchResponseView(facade.importDttZipToBranchView(zipBytes, branchIds, mergeStrategy));
    }


    public ImportDttSetToProfileResponse importDttZipToProfile(byte[] zipBytes, ImportDttZipToProfileUploadRequest request) {
        final var view = facade.assembleProfileApplyView(zipBytes, importPlanRequestMapper.toLibraryProfileImportPlan(request));
        return new ImportDttSetToProfileResponse(toJsonNode(view.profileJson()), view.deviceTypesCount());
    }

    public ImportDttSetToProfileResponse previewDttZipToProfile(byte[] zipBytes, ImportDttZipToProfileUploadRequest request) {
        final var view = facade.previewProfileImportView(zipBytes, importPlanRequestMapper.toLibraryProfileImportPlan(request));
        return new ImportDttSetToProfileResponse(toJsonNode(view.profileJson()), view.deviceTypesCount());
    }

    public ImportDttSetToBranchResponse importDttZipToBranch(byte[] zipBytes, ImportDttZipToBranchUploadRequest request) {
        return toBranchResponseView(facade.assembleBranchApplyView(zipBytes, importPlanRequestMapper.toLibraryBranchImportPlan(request)));
    }

    public ImportDttSetToBranchResponse previewDttZipToBranch(byte[] zipBytes, ImportDttZipToBranchUploadRequest request) {
        return toBranchResponseView(facade.previewBranchImportView(zipBytes, importPlanRequestMapper.toLibraryBranchImportPlan(request)));
    }

    public ImportDttSetToBranchResponse importDttZipToExistingBranch(byte[] zipBytes,
                                                                     ImportDttZipToExistingBranchUploadRequest request) {
        return toBranchResponseView(facade.mergeIntoExistingBranchJsonApplyView(zipBytes, request.existingBranchJson(), importPlanRequestMapper.toLibraryBranchImportPlan(request)));
    }

    private ImportDttSetToBranchResponse toBranchResponseView(ru.aritmos.dtt.api.dto.BranchAssemblyView view) {
        return toBranchResponseView(view.branchJson(), view.branchesCount(), view.deviceTypeMetadata());
    }

    private ImportDttSetToBranchResponse toBranchResponseView(ru.aritmos.dtt.api.dto.importplan.BranchImportPreviewView view) {
        return toBranchResponseView(view.branchJson(), view.branchesCount(), view.deviceTypeMetadata());
    }

    private ImportDttSetToBranchResponse toBranchResponseView(ru.aritmos.dtt.api.dto.importplan.BranchImportApplyView view) {
        return toBranchResponseView(view.branchJson(), view.branchesCount(), view.deviceTypeMetadata());
    }

    private ImportDttSetToBranchResponse toBranchResponseView(String branchJson,
                                                              int branchesCount,
                                                              List<DeviceTypeMetadata> deviceTypeMetadata) {
        final List<DeviceTypeBasicMetadataResponse> metadata =
                (deviceTypeMetadata == null ? List.<DeviceTypeMetadata>of() : deviceTypeMetadata).stream()
                .map(item -> new DeviceTypeBasicMetadataResponse(
                        item.id(),
                        item.name(),
                        item.displayName(),
                        item.version(),
                        item.description(),
                        item.iconBase64()
                ))
                .toList();
        return new ImportDttSetToBranchResponse(toJsonNode(branchJson), branchesCount, metadata);
    }

    /**
     * Экспортирует все DTT-архивы из branch equipment JSON.
     *
     * @param branchEquipment branch equipment JSON модель
     * @param mergeStrategy стратегия конфликтов между branch при повторении deviceTypeId
     * @return Base64-представление экспортированных DTT-архивов
     */
    public ExportAllDttFromBranchResponse exportAllDttFromBranch(BranchEquipment branchEquipment,
                                                                 List<String> branchIds,
                                                                 List<String> deviceTypeIds,
                                                                 MergeStrategy mergeStrategy,
                                                                 String dttVersion) {
        final var view = facade.exportDttSetFromBranchView(new BranchEquipmentExportRequest(
                branchEquipment,
                branchIds,
                deviceTypeIds,
                mergeStrategy,
                dttVersion
        ));
        return new ExportAllDttFromBranchResponse(view.archivesBase64ByDeviceTypeId(), view.archivesCount());
    }

    /**
     * Экспортирует все DTT-архивы из строкового branch equipment JSON.
     *
     * @param branchJson строковое представление branch equipment JSON
     * @param branchIds опциональный фильтр branch
     * @param deviceTypeIds опциональный фильтр типов устройств
     * @param mergeStrategy стратегия конфликтов между branch
     * @return Base64-представление экспортированных DTT-архивов
     */
    public ExportAllDttFromBranchResponse exportAllDttFromBranchJson(JsonNode branchJson,
                                                                     List<String> branchIds,
                                                                     List<String> deviceTypeIds,
                                                                     MergeStrategy mergeStrategy,
                                                                     String dttVersion) {
        return exportAllDttFromBranch(facade.parseBranchJson(toCompactJson(branchJson)), branchIds, deviceTypeIds, mergeStrategy, dttVersion);
    }

    /**
     * Экспортирует один DTT-архив из branch equipment JSON.
     *
     * @param branchEquipment branch equipment модель
     * @param branchIds опциональный фильтр branch
     * @param deviceTypeId идентификатор типа устройства
     * @param mergeStrategy стратегия merge
     * @param dttVersion опциональная версия шаблона
     * @return экспортированный архив в Base64
     */
    public ExportSingleDttResponse exportSingleDttFromBranch(BranchEquipment branchEquipment,
                                                             List<String> branchIds,
                                                             String deviceTypeId,
                                                             MergeStrategy mergeStrategy,
                                                             String dttVersion) {
        final var export = facade.exportSingleDttResultFromBranch(
                branchEquipment,
                branchIds,
                deviceTypeId,
                mergeStrategy,
                dttVersion
        );
        return new ExportSingleDttResponse(export.deviceTypeId(), export.archiveBase64());
    }

    /**
     * Экспортирует один DTT-архив из строкового branch equipment JSON.
     */
    public ExportSingleDttResponse exportSingleDttFromBranchJson(JsonNode branchJson,
                                                                 List<String> branchIds,
                                                                 String deviceTypeId,
                                                                 MergeStrategy mergeStrategy,
                                                                 String dttVersion) {
        return exportSingleDttFromBranch(
                facade.parseBranchJson(toCompactJson(branchJson)),
                branchIds,
                deviceTypeId,
                mergeStrategy,
                dttVersion
        );
    }

    /**
     * Экспортирует один DTT-архив из branch equipment JSON как бинарный payload.
     */
    public byte[] exportSingleDttFromBranchToBytes(BranchEquipment branchEquipment,
                                                   List<String> branchIds,
                                                   String deviceTypeId,
                                                   MergeStrategy mergeStrategy,
                                                   String dttVersion) {
        return facade.exportSingleDttResultFromBranch(
                branchEquipment,
                branchIds,
                deviceTypeId,
                mergeStrategy,
                dttVersion
        ).archiveBytes();
    }

    /**
     * Экспортирует один DTT-архив из строкового branch equipment JSON как бинарный payload.
     */
    public byte[] exportSingleDttFromBranchJsonToBytes(JsonNode branchJson,
                                                       List<String> branchIds,
                                                       String deviceTypeId,
                                                       MergeStrategy mergeStrategy,
                                                       String dttVersion) {
        return facade.exportSingleDttResultFromBranchJson(
                toCompactJson(branchJson),
                branchIds,
                deviceTypeId,
                mergeStrategy,
                dttVersion
        ).archiveBytes();
    }

    /**
     * Выполняет preview single-export из branch equipment JSON.
     */
    public SingleDttExportPreviewResponse previewSingleDttExportFromBranch(BranchEquipment branchEquipment,
                                                                            List<String> branchIds,
                                                                            String deviceTypeId,
                                                                            MergeStrategy mergeStrategy,
                                                                            String dttVersion) {
        return toDemoPreview(facade.previewSingleDttExportFromBranch(branchEquipment, branchIds, deviceTypeId, mergeStrategy, dttVersion));
    }

    /**
     * Выполняет preview single-export из строкового branch equipment JSON.
     */
    public SingleDttExportPreviewResponse previewSingleDttExportFromBranchJson(JsonNode branchJson,
                                                                                List<String> branchIds,
                                                                                String deviceTypeId,
                                                                                MergeStrategy mergeStrategy,
                                                                                String dttVersion) {
        return toDemoPreview(facade.previewSingleDttExportFromBranchJson(
                toCompactJson(branchJson),
                branchIds,
                deviceTypeId,
                mergeStrategy,
                dttVersion
        ));
    }

    private SingleDttExportPreviewResponse toDemoPreview(ru.aritmos.dtt.api.dto.SingleDttExportPreviewResult preview) {
        if (preview.success()) {
            return new SingleDttExportPreviewResponse(true, preview.deviceTypeId(), preview.archiveSizeBytes(), List.of());
        }
        return new SingleDttExportPreviewResponse(
                false,
                preview.deviceTypeId(),
                null,
                List.of(new SingleDttExportPreviewIssueResponse(preview.issueCode(), preview.issueMessage()))
        );
    }

    /**
     * Экспортирует все DTT-архивы из profile JSON в zip-файл (upload-download режим).
     */
    public byte[] exportProfileToZip(EquipmentProfile profile,
                                     JsonNode profileJson,
                                     List<String> deviceTypeIds,
                                     String dttVersion) {
        if (profile != null) {
            return facade.exportProfileToDttZip(new ProfileExportRequest(profile, deviceTypeIds, dttVersion));
        }
        return facade.exportProfileToDttZip(toCompactJson(profileJson), deviceTypeIds, dttVersion);
    }

    /**
     * Экспортирует все DTT-архивы из branch equipment JSON в zip-файл (upload-download режим).
     */
    public byte[] exportBranchToZip(BranchEquipment branchEquipment,
                                    JsonNode branchJson,
                                    List<String> branchIds,
                                    List<String> deviceTypeIds,
                                    MergeStrategy mergeStrategy,
                                    String dttVersion) {
        if (branchEquipment != null) {
            return facade.exportBranchToDttZip(new BranchEquipmentExportRequest(
                    branchEquipment,
                    branchIds,
                    deviceTypeIds,
                    mergeStrategy,
                    dttVersion
            ));
        }
        return facade.exportBranchToDttZip(
                toCompactJson(branchJson),
                branchIds,
                deviceTypeIds,
                mergeStrategy,
                dttVersion
        );
    }

    private String toCompactJson(JsonNode node) {
        if (node == null || node.isNull()) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(node);
        } catch (IOException exception) {
            throw new IllegalStateException("Failed to serialize request JSON", exception);
        }
    }

    private JsonNode toJsonNode(String json) {
        try {
            return objectMapper.readTree(json);
        } catch (IOException exception) {
            throw new IllegalStateException("Failed to parse generated JSON response", exception);
        }
    }

}
