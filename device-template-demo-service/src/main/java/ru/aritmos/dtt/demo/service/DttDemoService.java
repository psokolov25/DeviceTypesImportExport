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
import ru.aritmos.dtt.api.dto.importplan.BranchDeviceTypeMetadataOverrideImportRequest;
import ru.aritmos.dtt.api.dto.importplan.BranchMetadataImportRequest;
import ru.aritmos.dtt.api.dto.importplan.ProfileBranchMetadataImportPlanRequest;
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
import ru.aritmos.dtt.demo.dto.ImportBranchDeviceRequest;
import ru.aritmos.dtt.demo.dto.ImportBranchDeviceTypeRequest;
import ru.aritmos.dtt.demo.dto.ImportBranchMetadataRequest;
import ru.aritmos.dtt.demo.dto.ImportBranchRequest;
import ru.aritmos.dtt.demo.dto.ImportDeviceTypeMetadataOverrideRequest;
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
import ru.aritmos.dtt.demo.dto.ImportProfileDeviceTypeRequest;
import ru.aritmos.dtt.demo.dto.ImportUploadedBranchDeviceTypeRequest;
import ru.aritmos.dtt.demo.dto.ImportUploadedBranchRequest;
import ru.aritmos.dtt.demo.dto.ImportUploadedProfileDeviceTypeRequest;
import ru.aritmos.dtt.demo.dto.PreviewComputationEntry;
import ru.aritmos.dtt.demo.dto.ProfilePreviewDetailedResponse;
import ru.aritmos.dtt.demo.dto.SingleDttExportPreviewIssueResponse;
import ru.aritmos.dtt.demo.dto.SingleDttExportPreviewResponse;
import ru.aritmos.dtt.json.branch.BranchEquipment;
import ru.aritmos.dtt.json.profile.EquipmentProfile;

import java.io.IOException;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Сервис demo-модуля для валидации и инспекции DTT-архивов.
 */
@Singleton
public class DttDemoService {

    private final DeviceTemplateLibraryFacade facade;
    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * Создаёт demo-сервис с явно переданным фасадом библиотеки.
     *
     * @param facade фасад библиотеки DTT
     */
    public DttDemoService(DeviceTemplateLibraryFacade facade) {
        this.facade = facade;
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
        final var profile = facade.importDttSetToProfile(archives, mergeStrategy);
        final String profileJson = facade.toProfileJson(profile);
        final int deviceTypesCount = profile.deviceTypes() == null ? 0 : profile.deviceTypes().size();
        return new ImportDttSetToProfileResponse(toJsonNode(profileJson), deviceTypesCount);
    }

    /**
     * Импортирует набор DTT-архивов в profile JSON из Base64-представления.
     *
     * @param archivesBase64 список DTT-архивов в Base64
     * @param mergeStrategy стратегия разрешения конфликтов
     * @return результат импорта с JSON и фактическим количеством device types
     */
    public ImportDttSetToProfileResponse importDttSetToProfileBase64(List<String> archivesBase64, MergeStrategy mergeStrategy) {
        final var profile = facade.importDttBase64SetToProfile(archivesBase64, mergeStrategy);
        final String profileJson = facade.toProfileJson(profile);
        final int deviceTypesCount = profile.deviceTypes() == null ? 0 : profile.deviceTypes().size();
        return new ImportDttSetToProfileResponse(toJsonNode(profileJson), deviceTypesCount);
    }

    public ImportDttSetToProfileResponse importDttSetToProfile(ImportDttSetToProfileRequest request) {
        return toProfileResponse(facade.assembleProfile(toLibraryProfileImportPlan(request)));
    }

    public ImportDttSetToProfileResponse previewDttSetToProfile(ImportDttSetToProfileRequest request) {
        return toProfileResponse(facade.previewProfileImport(toLibraryProfileImportPlan(request)));
    }

    public ProfilePreviewDetailedResponse previewProfileDetailed(ImportDttSetToProfileRequest request) {
        final var preview = facade.previewProfileImportDetailed(toLibraryProfileImportPlan(request));
        return new ProfilePreviewDetailedResponse(
                toJsonNode(facade.toProfileJson(preview.profile())),
                toDemoPreviewComputations(preview.computationsByDeviceType())
        );
    }


    /**
     * Одновременно собирает profile JSON и branch equipment JSON из DTT с metadata inheritance.
     */
    public ImportProfileBranchWithMetadataResponse importProfileAndBranchWithMetadata(ImportProfileBranchWithMetadataRequest request) {
        Objects.requireNonNull(request, "request must not be null");
        final ProfileBranchAssemblyResult result = facade.assembleProfileAndBranchWithMetadata(toLibraryProfileBranchMetadataImportPlan(request));
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
        final Map<String, String> encoded = facade.exportDttSetFromProfileBase64(
                new ProfileExportRequest(profile, deviceTypeIds, dttVersion)
        );
        return new ExportAllDttFromProfileResponse(encoded, encoded.size());
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
        final var branch = facade.importDttSetToBranch(archives, branchIds, mergeStrategy);
        return toBranchResponse(branch);
    }

    /**
     * Импортирует набор DTT-архивов в branch equipment JSON из Base64-представления.
     */
    public ImportDttSetToBranchResponse importDttSetToBranchBase64(List<String> archivesBase64,
                                                                   List<String> branchIds,
                                                                   MergeStrategy mergeStrategy) {
        final var branch = facade.importDttBase64SetToBranch(archivesBase64, branchIds, mergeStrategy);
        return toBranchResponse(branch);
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
        final var branch = facade.importDttBase64SetToExistingBranchJson(archivesBase64, existingBranchJson, branchIds, mergeStrategy);
        return toBranchResponse(branch);
    }

    public ImportDttSetToBranchResponse importDttSetToBranch(ImportDttSetToBranchRequest request) {
        return toBranchResponse(facade.assembleBranch(toLibraryBranchImportPlan(request)));
    }

    public ImportDttSetToBranchResponse previewDttSetToBranch(ImportDttSetToBranchRequest request) {
        return toBranchResponse(facade.previewBranchImport(toLibraryBranchImportPlan(request)));
    }

    public BranchPreviewDetailedResponse previewBranchDetailed(ImportDttSetToBranchRequest request) {
        final var preview = facade.previewBranchImportDetailed(toLibraryBranchImportPlan(request));
        return new BranchPreviewDetailedResponse(
                toJsonNode(facade.toBranchJson(preview.branchEquipment())),
                toDemoPreviewComputations(preview.computationsByTarget())
        );
    }

    public ImportDttSetToBranchResponse importDttSetToExistingBranch(ImportDttSetToExistingBranchRequest request) {
        return toBranchResponse(facade.mergeIntoExistingBranchJson(request.existingBranchJson(), toLibraryBranchImportPlan(request)));
    }

    /**
     * Выполняет preview-сборку profile JSON из набора DTT-архивов без сохранения результата.
     *
     * @param archives список DTT-архивов
     * @param mergeStrategy стратегия разрешения конфликтов
     * @return рассчитанный preview profile JSON и количество типов устройств
     */
    public ImportDttSetToProfileResponse previewDttSetToProfile(List<byte[]> archives, MergeStrategy mergeStrategy) {
        final var profile = facade.previewDttSetToProfile(archives, mergeStrategy);
        final String profileJson = facade.toProfileJson(profile);
        final int deviceTypesCount = profile.deviceTypes() == null ? 0 : profile.deviceTypes().size();
        return new ImportDttSetToProfileResponse(toJsonNode(profileJson), deviceTypesCount);
    }

    /**
     * Выполняет preview-сборку profile JSON из Base64-представления набора DTT-архивов.
     */
    public ImportDttSetToProfileResponse previewDttSetToProfileBase64(List<String> archivesBase64, MergeStrategy mergeStrategy) {
        final var profile = facade.previewDttBase64SetToProfile(archivesBase64, mergeStrategy);
        final String profileJson = facade.toProfileJson(profile);
        final int deviceTypesCount = profile.deviceTypes() == null ? 0 : profile.deviceTypes().size();
        return new ImportDttSetToProfileResponse(toJsonNode(profileJson), deviceTypesCount);
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
        final var branch = facade.previewDttSetToBranch(archives, branchIds, mergeStrategy);
        return toBranchResponse(branch);
    }

    /**
     * Выполняет preview-сборку branch equipment JSON из Base64-представления набора DTT-архивов.
     */
    public ImportDttSetToBranchResponse previewDttSetToBranchBase64(List<String> archivesBase64,
                                                                    List<String> branchIds,
                                                                    MergeStrategy mergeStrategy) {
        final var branch = facade.previewDttBase64SetToBranch(archivesBase64, branchIds, mergeStrategy);
        return toBranchResponse(branch);
    }

    /**
     * Выполняет preview-сборку profile JSON из zip-набора DTT архивов.
     *
     * @param zipBytes zip-архив с файлами .dtt
     * @param mergeStrategy стратегия merge
     * @return рассчитанный preview profile JSON и количество типов устройств
     */
    public ImportDttSetToProfileResponse previewDttZipToProfile(byte[] zipBytes, MergeStrategy mergeStrategy) {
        final var profile = facade.previewDttZipToProfile(zipBytes, mergeStrategy);
        final String profileJson = facade.toProfileJson(profile);
        final int deviceTypesCount = profile.deviceTypes() == null ? 0 : profile.deviceTypes().size();
        return new ImportDttSetToProfileResponse(toJsonNode(profileJson), deviceTypesCount);
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
        final var branch = facade.previewDttZipToBranch(zipBytes, branchIds, mergeStrategy);
        return toBranchResponse(branch);
    }

    /**
     * Импортирует zip-набор DTT архивов в profile JSON.
     *
     * @param zipBytes zip-архив с файлами .dtt
     * @param mergeStrategy стратегия merge
     * @return результат импорта профиля
     */
    public ImportDttSetToProfileResponse importDttZipToProfile(byte[] zipBytes, MergeStrategy mergeStrategy) {
        final var profile = facade.importDttZipToProfile(zipBytes, mergeStrategy);
        final String profileJson = facade.toProfileJson(profile);
        final int deviceTypesCount = profile.deviceTypes() == null ? 0 : profile.deviceTypes().size();
        return new ImportDttSetToProfileResponse(toJsonNode(profileJson), deviceTypesCount);
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
        final var branch = facade.importDttZipToBranch(zipBytes, branchIds, mergeStrategy);
        return toBranchResponse(branch);
    }


    public ImportDttSetToProfileResponse importDttZipToProfile(byte[] zipBytes, ImportDttZipToProfileUploadRequest request) {
        return toProfileResponse(facade.assembleProfile(zipBytes, toLibraryProfileImportPlan(request)));
    }

    public ImportDttSetToProfileResponse previewDttZipToProfile(byte[] zipBytes, ImportDttZipToProfileUploadRequest request) {
        return toProfileResponse(facade.previewProfileImport(zipBytes, toLibraryProfileImportPlan(request)));
    }

    public ImportDttSetToBranchResponse importDttZipToBranch(byte[] zipBytes, ImportDttZipToBranchUploadRequest request) {
        return toBranchResponse(facade.assembleBranch(zipBytes, toLibraryBranchImportPlan(request)));
    }

    public ImportDttSetToBranchResponse previewDttZipToBranch(byte[] zipBytes, ImportDttZipToBranchUploadRequest request) {
        return toBranchResponse(facade.previewBranchImport(zipBytes, toLibraryBranchImportPlan(request)));
    }

    public ImportDttSetToBranchResponse importDttZipToExistingBranch(byte[] zipBytes,
                                                                     ImportDttZipToExistingBranchUploadRequest request) {
        return toBranchResponse(facade.mergeIntoExistingBranchJson(zipBytes, request.existingBranchJson(), toLibraryBranchImportPlan(request)));
    }

    private ImportDttSetToProfileResponse toProfileResponse(EquipmentProfile profile) {
        final var view = facade.toProfileAssemblyView(profile);
        return new ImportDttSetToProfileResponse(toJsonNode(view.profileJson()), view.deviceTypesCount());
    }

    private ImportDttSetToBranchResponse toBranchResponse(BranchEquipment branchEquipment) {
        final var view = facade.toBranchAssemblyView(branchEquipment);
        final List<DeviceTypeBasicMetadataResponse> metadata = facade.normalizeDeviceTypeMetadata(
                        view.deviceTypeMetadata() == null ? List.<DeviceTypeMetadata>of() : view.deviceTypeMetadata()
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
        return new ImportDttSetToBranchResponse(toJsonNode(view.branchJson()), view.branchesCount(), metadata);
    }

    private byte[] decodeBase64Archive(String archiveBase64) {
        if (archiveBase64 == null || archiveBase64.isBlank()) {
            throw new IllegalArgumentException("DTT archive payload must not be blank");
        }
        return Base64.getDecoder().decode(archiveBase64);
    }

    private String firstNonBlank(String... values) {
        if (values == null) {
            return null;
        }
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value;
            }
        }
        return null;
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
        final Map<String, String> encoded = facade.exportDttSetFromBranchBase64(new BranchEquipmentExportRequest(
                branchEquipment,
                branchIds,
                deviceTypeIds,
                mergeStrategy,
                dttVersion
        ));
        return new ExportAllDttFromBranchResponse(encoded, encoded.size());
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


    private ProfileBranchMetadataImportPlanRequest toLibraryProfileBranchMetadataImportPlan(ImportProfileBranchWithMetadataRequest request) {
        return new ProfileBranchMetadataImportPlanRequest(
                request == null || request.deviceTypes() == null
                        ? List.of()
                        : request.deviceTypes().stream()
                        .map(this::toLibraryProfileSource)
                        .toList(),
                request == null || request.branches() == null
                        ? List.of()
                        : request.branches().stream()
                        .map(this::toLibraryBranchMetadataImport)
                        .toList(),
                request == null ? null : request.mergeStrategy()
        );
    }

    private BranchMetadataImportRequest toLibraryBranchMetadataImport(ImportBranchMetadataRequest request) {
        return new BranchMetadataImportRequest(
                request.branchId(),
                request.branchId(),
                request.metadataOverrides() == null ? List.of() : request.metadataOverrides().stream()
                        .map(override -> new BranchDeviceTypeMetadataOverrideImportRequest(override.deviceTypeId(), toLibraryMetadataOverride(override.metadata())))
                        .toList()
        );
    }

    private ru.aritmos.dtt.api.dto.importplan.ProfileImportPlanRequest toLibraryProfileImportPlan(ImportDttSetToProfileRequest request) {
        return new ru.aritmos.dtt.api.dto.importplan.ProfileImportPlanRequest(
                request == null ? null : request.archivesBase64(),
                request == null ? null : request.mergeStrategy(),
                request == null || request.deviceTypes() == null
                        ? List.of()
                        : request.deviceTypes().stream().map(this::toLibraryProfileSource).toList()
        );
    }

    private ru.aritmos.dtt.api.dto.importplan.ProfileImportPlanRequest toLibraryProfileImportPlan(ImportDttZipToProfileUploadRequest request) {
        return new ru.aritmos.dtt.api.dto.importplan.ProfileImportPlanRequest(
                null,
                request == null ? null : request.mergeStrategy(),
                request == null || request.deviceTypes() == null
                        ? List.of()
                        : request.deviceTypes().stream().map(this::toLibraryProfileSource).toList()
        );
    }

    private ru.aritmos.dtt.api.dto.importplan.BranchImportPlanRequest toLibraryBranchImportPlan(ImportDttSetToBranchRequest request) {
        return new ru.aritmos.dtt.api.dto.importplan.BranchImportPlanRequest(
                request == null ? null : request.archivesBase64(),
                request == null ? null : request.branchIds(),
                request == null ? null : request.mergeStrategy(),
                request == null || request.branches() == null
                        ? List.of()
                        : request.branches().stream().map(this::toLibraryBranchSource).toList()
        );
    }

    private ru.aritmos.dtt.api.dto.importplan.BranchImportPlanRequest toLibraryBranchImportPlan(ImportDttSetToExistingBranchRequest request) {
        return new ru.aritmos.dtt.api.dto.importplan.BranchImportPlanRequest(
                request == null ? null : request.archivesBase64(),
                request == null ? null : request.branchIds(),
                request == null ? null : request.mergeStrategy(),
                request == null || request.branches() == null
                        ? List.of()
                        : request.branches().stream().map(this::toLibraryBranchSource).toList()
        );
    }

    private ru.aritmos.dtt.api.dto.importplan.BranchImportPlanRequest toLibraryBranchImportPlan(ImportDttZipToBranchUploadRequest request) {
        return new ru.aritmos.dtt.api.dto.importplan.BranchImportPlanRequest(
                null,
                request == null ? null : request.branchIds(),
                request == null ? null : request.mergeStrategy(),
                request == null || request.branches() == null
                        ? List.of()
                        : request.branches().stream().map(this::toLibraryBranchSource).toList()
        );
    }

    private ru.aritmos.dtt.api.dto.importplan.BranchImportPlanRequest toLibraryBranchImportPlan(ImportDttZipToExistingBranchUploadRequest request) {
        return new ru.aritmos.dtt.api.dto.importplan.BranchImportPlanRequest(
                null,
                request == null ? null : request.branchIds(),
                request == null ? null : request.mergeStrategy(),
                request == null || request.branches() == null
                        ? List.of()
                        : request.branches().stream().map(this::toLibraryBranchSource).toList()
        );
    }

    private ru.aritmos.dtt.api.dto.importplan.ProfileDeviceTypeImportSourceRequest toLibraryProfileSource(ImportProfileDeviceTypeRequest request) {
        return new ru.aritmos.dtt.api.dto.importplan.ProfileDeviceTypeImportSourceRequest(
                request.archiveBase64(),
                null,
                request.deviceTypeParamValues(),
                toLibraryMetadataOverride(request.metadataOverride())
        );
    }

    private ru.aritmos.dtt.api.dto.importplan.ProfileDeviceTypeImportSourceRequest toLibraryProfileSource(ImportUploadedProfileDeviceTypeRequest request) {
        return new ru.aritmos.dtt.api.dto.importplan.ProfileDeviceTypeImportSourceRequest(
                null,
                request.archiveEntryName(),
                request.deviceTypeParamValues(),
                toLibraryMetadataOverride(request.metadataOverride())
        );
    }

    private ru.aritmos.dtt.api.dto.importplan.BranchImportSourceRequest toLibraryBranchSource(ImportBranchRequest request) {
        return new ru.aritmos.dtt.api.dto.importplan.BranchImportSourceRequest(
                request.branchId(),
                request.displayName(),
                request.deviceTypes() == null ? List.of() : request.deviceTypes().stream().map(this::toLibraryBranchDeviceTypeSource).toList()
        );
    }

    private ru.aritmos.dtt.api.dto.importplan.BranchImportSourceRequest toLibraryBranchSource(ImportUploadedBranchRequest request) {
        return new ru.aritmos.dtt.api.dto.importplan.BranchImportSourceRequest(
                request.branchId(),
                request.displayName(),
                request.deviceTypes() == null ? List.of() : request.deviceTypes().stream().map(this::toLibraryBranchDeviceTypeSource).toList()
        );
    }

    private ru.aritmos.dtt.api.dto.importplan.BranchDeviceTypeImportSourceRequest toLibraryBranchDeviceTypeSource(ImportBranchDeviceTypeRequest request) {
        return new ru.aritmos.dtt.api.dto.importplan.BranchDeviceTypeImportSourceRequest(
                request.archiveBase64(),
                null,
                request.deviceTypeParamValues(),
                toLibraryMetadataOverride(request.metadataOverride()),
                request.devices() == null ? List.of() : request.devices().stream().map(this::toLibraryDeviceInstance).toList(),
                request.kind()
        );
    }

    private ru.aritmos.dtt.api.dto.importplan.BranchDeviceTypeImportSourceRequest toLibraryBranchDeviceTypeSource(ImportUploadedBranchDeviceTypeRequest request) {
        return new ru.aritmos.dtt.api.dto.importplan.BranchDeviceTypeImportSourceRequest(
                null,
                request.archiveEntryName(),
                request.deviceTypeParamValues(),
                toLibraryMetadataOverride(request.metadataOverride()),
                request.devices() == null ? List.of() : request.devices().stream().map(this::toLibraryDeviceInstance).toList(),
                request.kind()
        );
    }

    private ru.aritmos.dtt.api.dto.branch.DeviceInstanceImportRequest toLibraryDeviceInstance(ImportBranchDeviceRequest request) {
        return new ru.aritmos.dtt.api.dto.branch.DeviceInstanceImportRequest(
                request.id(),
                request.name(),
                request.displayName(),
                request.description(),
                request.deviceParamValues()
        );
    }

    private DeviceTypeMetadata toLibraryMetadataOverride(ImportDeviceTypeMetadataOverrideRequest override) {
        if (override == null) {
            return null;
        }
        return new DeviceTypeMetadata(
                override.id(),
                override.name(),
                override.displayName(),
                override.description(),
                override.version(),
                override.iconBase64()
        );
    }

    private Map<String, PreviewComputationEntry> toDemoPreviewComputations(
            Map<String, ru.aritmos.dtt.api.dto.importplan.ImportPreviewComputationEntry> computations
    ) {
        final Map<String, PreviewComputationEntry> result = new LinkedHashMap<>();
        computations.forEach((key, value) -> result.put(
                key,
                new PreviewComputationEntry(value.defaultValuesCount(), value.overrideValuesCount())
        ));
        return result;
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
