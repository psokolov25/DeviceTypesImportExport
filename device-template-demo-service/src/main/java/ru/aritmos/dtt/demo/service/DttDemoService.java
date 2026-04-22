package ru.aritmos.dtt.demo.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.inject.Singleton;
import ru.aritmos.dtt.api.DeviceTemplateLibraryFacade;
import ru.aritmos.dtt.api.dto.DeviceTypeMetadata;
import ru.aritmos.dtt.api.dto.DeviceTypeTemplate;
import ru.aritmos.dtt.api.dto.EquipmentProfileAssemblyRequest;
import ru.aritmos.dtt.api.dto.EquipmentProfileDeviceTypeRequest;
import ru.aritmos.dtt.api.dto.MergeStrategy;
import ru.aritmos.dtt.api.dto.ProfileExportRequest;
import ru.aritmos.dtt.demo.dto.ImportBranchDeviceTypeMetadataOverrideRequest;
import ru.aritmos.dtt.demo.dto.ImportBranchMetadataRequest;
import ru.aritmos.dtt.demo.dto.ImportProfileBranchWithMetadataResponse;
import ru.aritmos.dtt.demo.dto.ImportProfileBranchWithMetadataRequest;
import ru.aritmos.dtt.api.dto.ProfileBranchAssemblyResult;
import ru.aritmos.dtt.api.dto.branch.BranchDeviceTypeImportRequest;
import ru.aritmos.dtt.api.dto.branch.BranchEquipmentAssemblyRequest;
import ru.aritmos.dtt.api.dto.branch.BranchEquipmentExportRequest;
import ru.aritmos.dtt.api.dto.branch.BranchImportRequest;
import ru.aritmos.dtt.api.dto.branch.DeviceInstanceImportRequest;
import ru.aritmos.dtt.api.DttVersionSupport;
import ru.aritmos.dtt.archive.DttIconSupport;
import ru.aritmos.dtt.archive.model.DttArchiveTemplate;
import ru.aritmos.dtt.demo.dto.DeviceTypeBasicMetadataResponse;
import ru.aritmos.dtt.demo.dto.DttMetadataBatchResponse;
import ru.aritmos.dtt.demo.dto.DttVersionComparisonResponse;
import ru.aritmos.dtt.demo.dto.ProfilePreviewDetailedResponse;
import ru.aritmos.dtt.demo.dto.BranchPreviewDetailedResponse;
import ru.aritmos.dtt.demo.dto.PreviewComputationEntry;
import ru.aritmos.dtt.demo.dto.DttInspectionResponse;
import ru.aritmos.dtt.demo.dto.DttValidationIssueResponse;
import ru.aritmos.dtt.demo.dto.DttValidationResponse;
import ru.aritmos.dtt.demo.dto.ExportAllDttFromBranchResponse;
import ru.aritmos.dtt.demo.dto.ExportAllDttFromProfileResponse;
import ru.aritmos.dtt.demo.dto.ExportSingleDttResponse;
import ru.aritmos.dtt.demo.dto.ImportBranchDeviceRequest;
import ru.aritmos.dtt.demo.dto.ImportBranchDeviceTypeRequest;
import ru.aritmos.dtt.demo.dto.ImportDeviceTypeMetadataOverrideRequest;
import ru.aritmos.dtt.demo.dto.ImportBranchRequest;
import ru.aritmos.dtt.demo.dto.ImportDttSetToBranchRequest;
import ru.aritmos.dtt.demo.dto.ImportDttSetToBranchResponse;
import ru.aritmos.dtt.demo.dto.ImportDttZipToBranchUploadRequest;
import ru.aritmos.dtt.demo.dto.ImportDttZipToExistingBranchUploadRequest;
import ru.aritmos.dtt.demo.dto.ImportDttSetToExistingBranchRequest;
import ru.aritmos.dtt.demo.dto.ImportDttSetToProfileRequest;
import ru.aritmos.dtt.demo.dto.ImportDttSetToProfileResponse;
import ru.aritmos.dtt.demo.dto.ImportDttZipToProfileUploadRequest;
import ru.aritmos.dtt.demo.dto.ImportProfileDeviceTypeRequest;
import ru.aritmos.dtt.demo.dto.ImportUploadedBranchDeviceTypeRequest;
import ru.aritmos.dtt.demo.dto.ImportUploadedBranchRequest;
import ru.aritmos.dtt.demo.dto.ImportUploadedProfileDeviceTypeRequest;
import ru.aritmos.dtt.demo.dto.SingleDttExportPreviewIssueResponse;
import ru.aritmos.dtt.demo.dto.SingleDttExportPreviewResponse;
import ru.aritmos.dtt.json.branch.BranchEquipment;
import ru.aritmos.dtt.json.branch.BranchDeviceTypeMetadata;
import ru.aritmos.dtt.json.branch.BranchScript;
import ru.aritmos.dtt.json.profile.EquipmentProfile;
import ru.aritmos.dtt.model.canonical.CanonicalBranchProjection;
import ru.aritmos.dtt.model.canonical.CanonicalProfileProjection;
import ru.aritmos.dtt.model.mapping.CanonicalProjectionMapper;
import ru.aritmos.dtt.model.mapping.CanonicalTemplateMapper;
import ru.aritmos.dtt.model.mapping.DefaultCanonicalProjectionMapper;
import ru.aritmos.dtt.model.mapping.DefaultCanonicalTemplateMapper;

import java.util.ArrayList;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.io.ByteArrayInputStream;
import java.io.IOException;

/**
 * Сервис demo-модуля для валидации и инспекции DTT-архивов.
 */
@Singleton
public class DttDemoService {

    private final DeviceTemplateLibraryFacade facade;
    private final CanonicalTemplateMapper canonicalTemplateMapper = new DefaultCanonicalTemplateMapper();
    private final CanonicalProjectionMapper canonicalProjectionMapper = new DefaultCanonicalProjectionMapper();
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
        final DttArchiveTemplate template = facade.readDtt(archiveBytes);
        return new DttInspectionResponse(
                template.descriptor().formatName(),
                template.descriptor().formatVersion(),
                template.metadata().id(),
                template.metadata().name(),
                template.descriptor().deviceTypeVersion(),
                template.metadata().description(),
                DttIconSupport.resolveOrDefault(template.metadata().iconBase64()),
                template.eventHandlers() == null ? 0 : template.eventHandlers().size(),
                template.commands() == null ? 0 : template.commands().size()
        );
    }

    /**
     * Извлекает базовые метаданные типов устройств из DTT-файла или zip-набора DTT.
     *
     * @param payload бинарный DTT или zip с DTT-файлами
     * @return список базовых метаданных типов устройств
     */
    public DttMetadataBatchResponse extractMetadata(byte[] payload) {
        final List<DttArchiveTemplate> templates = readTemplatesFromDttOrZip(payload);
        final List<DeviceTypeBasicMetadataResponse> metadata = templates.stream()
                .map(this::toBasicMetadata)
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
        final DttArchiveTemplate template = facade.readDtt(archiveBytes);
        final String normalizedInput = DttVersionSupport.normalize(inputVersion);
        final String normalizedDtt = DttVersionSupport.normalize(template.descriptor().deviceTypeVersion());
        final int comparison = DttVersionSupport.compare(normalizedInput, normalizedDtt);
        final String source = comparison == 0 ? "EQUAL" : (comparison > 0 ? "INPUT" : "DTT");
        final String greater = comparison >= 0 ? normalizedInput : normalizedDtt;
        return new DttVersionComparisonResponse(normalizedInput, normalizedDtt, greater, source);
    }


    /**
     * Возвращает человеко-читаемое имя типа устройства из DTT-архива.
     *
     * @param archiveBytes бинарное содержимое DTT-архива
     * @param fallbackName резервное имя
     * @return базовое имя файла без расширения
     */
    public String resolveDeviceTypeArchiveBaseName(byte[] archiveBytes, String fallbackName) {
        final DttArchiveTemplate template = facade.readDtt(archiveBytes);
        return ru.aritmos.dtt.archive.DttFileNames.resolveBaseName(template.metadata(), fallbackName);
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
        return toProfileResponse(facade.assembleProfile(toStructuredProfileAssemblyRequest(request)));
    }

    public ImportDttSetToProfileResponse previewDttSetToProfile(ImportDttSetToProfileRequest request) {
        return toProfileResponse(facade.assembleProfile(toStructuredProfileAssemblyRequest(request)));
    }

    public ProfilePreviewDetailedResponse previewProfileDetailed(ImportDttSetToProfileRequest request) {
        final var preview = facade.assembleProfile(toStructuredProfileAssemblyRequest(request));
        return new ProfilePreviewDetailedResponse(
                toJsonNode(facade.toProfileJson(preview)),
                computeProfilePreview(request)
        );
    }


    /**
     * Одновременно собирает profile JSON и branch equipment JSON из DTT с metadata inheritance.
     */
    public ImportProfileBranchWithMetadataResponse importProfileAndBranchWithMetadata(ImportProfileBranchWithMetadataRequest request) {
        Objects.requireNonNull(request, "request must not be null");
        final List<byte[]> archives = request.deviceTypes().stream()
                .map(ImportProfileDeviceTypeRequest::archiveBase64)
                .map(this::decodeBase64Archive)
                .toList();
        final Map<String, DeviceTypeMetadata> profileMetadataOverrides = new LinkedHashMap<>();
        request.deviceTypes().forEach(deviceType -> {
            final DttArchiveTemplate archive = facade.readDtt(decodeBase64Archive(deviceType.archiveBase64()));
            final String deviceTypeId = archive.metadata().id();
            profileMetadataOverrides.put(deviceTypeId, applyMetadataOverride(archive.metadata(), deviceType.metadataOverride()));
        });
        final Map<String, Map<String, DeviceTypeMetadata>> branchOverrides = new LinkedHashMap<>();
        final List<String> branchIds = request.branches().stream().map(ImportBranchMetadataRequest::branchId).toList();
        request.branches().forEach(branch -> {
            final Map<String, DeviceTypeMetadata> byType = new LinkedHashMap<>();
            if (branch.metadataOverrides() != null) {
                branch.metadataOverrides().forEach(override -> byType.put(override.deviceTypeId(), applyMetadataOverride(null, override.metadata())));
            }
            branchOverrides.put(branch.branchId(), byType);
        });
        final ProfileBranchAssemblyResult result = facade.importDttSetToProfileAndBranchWithMetadata(
                archives,
                branchIds,
                profileMetadataOverrides,
                branchOverrides,
                request.mergeStrategy()
        );
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
        final ExportAllDttFromProfileResponse exported =
                exportAllDttFromProfile(profile, List.of(deviceTypeId), dttVersion);
        final String archiveBase64 = exported.archivesBase64ByDeviceTypeId().get(deviceTypeId);
        if (archiveBase64 == null || archiveBase64.isBlank()) {
            throw new IllegalArgumentException("deviceTypeId not found in profile: " + deviceTypeId);
        }
        return new ExportSingleDttResponse(deviceTypeId, archiveBase64);
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
        return Base64.getDecoder().decode(exportSingleDttFromProfile(profile, deviceTypeId, dttVersion).archiveBase64());
    }

    /**
     * Экспортирует один DTT-архив из строкового profile JSON как бинарный payload.
     */
    public byte[] exportSingleDttFromProfileJsonToBytes(JsonNode profileJson,
                                                        String deviceTypeId,
                                                        String dttVersion) {
        return Base64.getDecoder().decode(exportSingleDttFromProfileJson(profileJson, deviceTypeId, dttVersion).archiveBase64());
    }

    /**
     * Выполняет preview single-export из profile JSON.
     */
    public SingleDttExportPreviewResponse previewSingleDttExportFromProfile(EquipmentProfile profile,
                                                                             String deviceTypeId,
                                                                             String dttVersion) {
        try {
            final byte[] payload = exportSingleDttFromProfileToBytes(profile, deviceTypeId, dttVersion);
            return new SingleDttExportPreviewResponse(true, deviceTypeId, payload.length, List.of());
        } catch (RuntimeException exception) {
            return failedPreview(deviceTypeId, exception, false);
        }
    }

    /**
     * Выполняет preview single-export из строкового profile JSON.
     */
    public SingleDttExportPreviewResponse previewSingleDttExportFromProfileJson(JsonNode profileJson,
                                                                                 String deviceTypeId,
                                                                                 String dttVersion) {
        try {
            final byte[] payload = exportSingleDttFromProfileJsonToBytes(profileJson, deviceTypeId, dttVersion);
            return new SingleDttExportPreviewResponse(true, deviceTypeId, payload.length, List.of());
        } catch (RuntimeException exception) {
            return failedPreview(deviceTypeId, exception, false);
        }
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
        final BranchEquipment existing = facade.parseBranchJson(existingBranchJson);
        final var branch = facade.importDttBase64SetToExistingBranch(archivesBase64, existing, branchIds, mergeStrategy);
        return toBranchResponse(branch);
    }

    public ImportDttSetToBranchResponse importDttSetToBranch(ImportDttSetToBranchRequest request) {
        return toBranchResponse(facade.assembleBranch(toStructuredBranchAssemblyRequest(request)));
    }

    public ImportDttSetToBranchResponse previewDttSetToBranch(ImportDttSetToBranchRequest request) {
        return toBranchResponse(facade.assembleBranch(toStructuredBranchAssemblyRequest(request)));
    }

    public BranchPreviewDetailedResponse previewBranchDetailed(ImportDttSetToBranchRequest request) {
        final BranchEquipment preview = facade.assembleBranch(toStructuredBranchAssemblyRequest(request));
        return new BranchPreviewDetailedResponse(
                toJsonNode(facade.toBranchJson(preview)),
                computeBranchPreview(request)
        );
    }

    public ImportDttSetToBranchResponse importDttSetToExistingBranch(ImportDttSetToExistingBranchRequest request) {
        final BranchEquipment existing = facade.parseBranchJson(request.existingBranchJson());
        final BranchEquipment incoming = facade.assembleBranch(toStructuredBranchAssemblyRequest(request));
        final BranchEquipment merged = mergeBranchEquipment(existing, incoming, request.mergeStrategy());
        return toBranchResponse(merged);
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
        return toProfileResponse(facade.assembleProfile(toStructuredProfileAssemblyRequest(zipBytes, request)));
    }

    public ImportDttSetToProfileResponse previewDttZipToProfile(byte[] zipBytes, ImportDttZipToProfileUploadRequest request) {
        return toProfileResponse(facade.assembleProfile(toStructuredProfileAssemblyRequest(zipBytes, request)));
    }

    public ImportDttSetToBranchResponse importDttZipToBranch(byte[] zipBytes, ImportDttZipToBranchUploadRequest request) {
        return toBranchResponse(facade.assembleBranch(toStructuredBranchAssemblyRequest(zipBytes, request)));
    }

    public ImportDttSetToBranchResponse previewDttZipToBranch(byte[] zipBytes, ImportDttZipToBranchUploadRequest request) {
        return toBranchResponse(facade.assembleBranch(toStructuredBranchAssemblyRequest(zipBytes, request)));
    }

    public ImportDttSetToBranchResponse importDttZipToExistingBranch(byte[] zipBytes,
                                                                     ImportDttZipToExistingBranchUploadRequest request) {
        final BranchEquipment existing = facade.parseBranchJson(request.existingBranchJson());
        final BranchEquipment imported = facade.assembleBranch(toStructuredBranchAssemblyRequest(zipBytes, request));
        return toBranchResponse(mergeBranchEquipment(existing, imported, request.mergeStrategy()));
    }

    private ImportDttSetToProfileResponse toProfileResponse(EquipmentProfile profile) {
        final String profileJson = facade.toProfileJson(profile);
        final int deviceTypesCount = profile.deviceTypes() == null ? 0 : profile.deviceTypes().size();
        return new ImportDttSetToProfileResponse(toJsonNode(profileJson), deviceTypesCount);
    }

    private ImportDttSetToBranchResponse toBranchResponse(BranchEquipment branchEquipment) {
        final String branchJson = facade.toBranchJson(branchEquipment);
        final int branchesCount = branchEquipment.branches() == null ? 0 : branchEquipment.branches().size();
        final List<DeviceTypeBasicMetadataResponse> metadata = (branchEquipment.metadata() == null ? List.<BranchDeviceTypeMetadata>of() : branchEquipment.metadata())
                .stream()
                .map(item -> new DeviceTypeBasicMetadataResponse(
                        item.id(),
                        firstNonBlank(item.name(), item.id()),
                        firstNonBlank(item.displayName(), item.name(), item.id()),
                        item.version(),
                        firstNonBlank(item.description(), item.name(), item.id(), ""),
                        DttIconSupport.resolveOrDefault(item.imageBase64())
                ))
                .toList();
        return new ImportDttSetToBranchResponse(toJsonNode(branchJson), branchesCount, metadata);
    }

    private List<DttArchiveTemplate> readTemplatesFromDttOrZip(byte[] payload) {
        try {
            return List.of(facade.readDtt(payload));
        } catch (RuntimeException ignored) {
            final Map<String, byte[]> archivesByEntry = readDttFilesFromZipByEntryName(payload);
            return archivesByEntry.values().stream().map(facade::readDtt).toList();
        }
    }

    private DeviceTypeBasicMetadataResponse toBasicMetadata(DttArchiveTemplate template) {
        final String name = firstNonBlank(template.metadata().name(), template.metadata().id());
        return new DeviceTypeBasicMetadataResponse(
                template.metadata().id(),
                name,
                firstNonBlank(template.metadata().displayName(), name, template.metadata().id()),
                template.descriptor().deviceTypeVersion(),
                firstNonBlank(template.metadata().description(), name, template.metadata().id(), ""),
                DttIconSupport.resolveOrDefault(template.metadata().iconBase64())
        );
    }

    private Map<String, PreviewComputationEntry> computeProfilePreview(ImportDttSetToProfileRequest request) {
        final Map<String, PreviewComputationEntry> result = new LinkedHashMap<>();
        if (request.deviceTypes() != null && !request.deviceTypes().isEmpty()) {
            for (ImportProfileDeviceTypeRequest deviceType : request.deviceTypes()) {
                final DttArchiveTemplate archive = facade.readDtt(decodeBase64Archive(deviceType.archiveBase64()));
                final int defaults = archive.defaultValues() == null ? 0 : archive.defaultValues().size();
                final int overrides = deviceType.deviceTypeParamValues() == null ? 0 : deviceType.deviceTypeParamValues().size();
                result.put(archive.metadata().id(), new PreviewComputationEntry(defaults, overrides));
            }
            return result;
        }
        if (request.archivesBase64() != null) {
            for (String archiveBase64 : request.archivesBase64()) {
                final DttArchiveTemplate archive = facade.readDtt(decodeBase64Archive(archiveBase64));
                final int defaults = archive.defaultValues() == null ? 0 : archive.defaultValues().size();
                result.put(archive.metadata().id(), new PreviewComputationEntry(defaults, 0));
            }
        }
        return result;
    }

    private Map<String, PreviewComputationEntry> computeBranchPreview(ImportDttSetToBranchRequest request) {
        final Map<String, PreviewComputationEntry> result = new LinkedHashMap<>();
        if (request.branches() == null || request.branches().isEmpty()) {
            return result;
        }
        for (ImportBranchRequest branch : request.branches()) {
            if (branch.deviceTypes() == null) {
                continue;
            }
            for (ImportBranchDeviceTypeRequest deviceType : branch.deviceTypes()) {
                final DttArchiveTemplate archive = facade.readDtt(decodeBase64Archive(deviceType.archiveBase64()));
                final int defaults = archive.defaultValues() == null ? 0 : archive.defaultValues().size();
                final int overrides = deviceType.deviceTypeParamValues() == null ? 0 : deviceType.deviceTypeParamValues().size();
                result.put(branch.branchId() + ":" + archive.metadata().id(), new PreviewComputationEntry(defaults, overrides));
            }
        }
        return result;
    }


    private EquipmentProfileAssemblyRequest toStructuredProfileAssemblyRequest(byte[] zipBytes,
                                                                              ImportDttZipToProfileUploadRequest request) {
        final Map<String, byte[]> archivesByEntryName = readDttFilesFromZipByEntryName(zipBytes);
        final List<EquipmentProfileDeviceTypeRequest> deviceTypes = new ArrayList<>();
        if (request != null && request.deviceTypes() != null && !request.deviceTypes().isEmpty()) {
            request.deviceTypes().forEach(item -> deviceTypes.add(toProfileDeviceTypeRequest(archivesByEntryName, item)));
        } else {
            archivesByEntryName.values().forEach(bytes -> {
                final DttArchiveTemplate archive = facade.readDtt(bytes);
                deviceTypes.add(new EquipmentProfileDeviceTypeRequest(toDeviceTypeTemplate(archive), true));
            });
        }
        if (deviceTypes.isEmpty()) {
            throw new IllegalArgumentException("zip payload must contain at least one .dtt file");
        }
        return new EquipmentProfileAssemblyRequest(
                deviceTypes,
                List.of(),
                request == null ? MergeStrategy.FAIL_IF_EXISTS : request.mergeStrategy()
        );
    }

    private EquipmentProfileDeviceTypeRequest toProfileDeviceTypeRequest(Map<String, byte[]> archivesByEntryName,
                                                                        ImportUploadedProfileDeviceTypeRequest request) {
        final DttArchiveTemplate archive = facade.readDtt(resolveArchiveEntry(archivesByEntryName, request.archiveEntryName()));
        final DeviceTypeTemplate template = toDeviceTypeTemplate(archive);
        return new EquipmentProfileDeviceTypeRequest(
                new DeviceTypeTemplate(applyMetadataOverride(template.metadata(), request.metadataOverride()), mergeValues(template.deviceTypeParamValues(), request.deviceTypeParamValues())),
                true
        );
    }

    private BranchEquipmentAssemblyRequest toStructuredBranchAssemblyRequest(byte[] zipBytes,
                                                                            ImportDttZipToBranchUploadRequest request) {
        Objects.requireNonNull(request, "request must not be null");
        final Map<String, byte[]> archivesByEntryName = readDttFilesFromZipByEntryName(zipBytes);
        final Map<String, BranchImportRequest> branches = new LinkedHashMap<>();
        if (request.branchIds() != null) {
            final List<byte[]> archives = new ArrayList<>(archivesByEntryName.values());
            for (String branchId : request.branchIds()) {
                branches.put(branchId, buildLegacyBranchImportRequestFromBytes(branchId, archives));
            }
        }
        if (request.branches() != null) {
            request.branches().forEach(branchRequest -> mergeBranchImport(branches, toStructuredBranchImportRequest(archivesByEntryName, branchRequest)));
        }
        if (branches.isEmpty()) {
            throw new IllegalArgumentException("Either branchIds or structured branches must contain at least one import target");
        }
        return new BranchEquipmentAssemblyRequest(new ArrayList<>(branches.values()), request.mergeStrategy());
    }

    private BranchEquipmentAssemblyRequest toStructuredBranchAssemblyRequest(byte[] zipBytes,
                                                                            ImportDttZipToExistingBranchUploadRequest request) {
        Objects.requireNonNull(request, "request must not be null");
        final Map<String, byte[]> archivesByEntryName = readDttFilesFromZipByEntryName(zipBytes);
        final Map<String, BranchImportRequest> branches = new LinkedHashMap<>();
        if (request.branchIds() != null) {
            final List<byte[]> archives = new ArrayList<>(archivesByEntryName.values());
            for (String branchId : request.branchIds()) {
                branches.put(branchId, buildLegacyBranchImportRequestFromBytes(branchId, archives));
            }
        }
        if (request.branches() != null) {
            request.branches().forEach(branchRequest -> mergeBranchImport(branches, toStructuredBranchImportRequest(archivesByEntryName, branchRequest)));
        }
        if (branches.isEmpty()) {
            throw new IllegalArgumentException("Either branchIds or structured branches must contain at least one import target");
        }
        return new BranchEquipmentAssemblyRequest(new ArrayList<>(branches.values()), request.mergeStrategy());
    }

    private BranchImportRequest toStructuredBranchImportRequest(Map<String, byte[]> archivesByEntryName,
                                                               ImportUploadedBranchRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("branch request must not be null");
        }
        if (request.branchId() == null || request.branchId().isBlank()) {
            throw new IllegalArgumentException("branchId must not be blank");
        }
        if (request.deviceTypes() == null || request.deviceTypes().isEmpty()) {
            throw new IllegalArgumentException("deviceTypes must contain at least one DTT archive for branch " + request.branchId());
        }
        final List<BranchDeviceTypeImportRequest> deviceTypes = request.deviceTypes().stream()
                .map(deviceTypeRequest -> toStructuredBranchDeviceTypeImportRequest(request.branchId(), archivesByEntryName, deviceTypeRequest))
                .toList();
        final String displayName = request.displayName() == null || request.displayName().isBlank()
                ? request.branchId()
                : request.displayName();
        return new BranchImportRequest(request.branchId(), displayName, deviceTypes);
    }

    private BranchDeviceTypeImportRequest toStructuredBranchDeviceTypeImportRequest(String branchId,
                                                                                    Map<String, byte[]> archivesByEntryName,
                                                                                    ImportUploadedBranchDeviceTypeRequest request) {
        final DttArchiveTemplate archive = facade.readDtt(resolveArchiveEntry(archivesByEntryName, request.archiveEntryName()));
        final BranchDeviceTypeImportRequest base = toBranchDeviceTypeImportRequest(archive, branchId);
        final DeviceTypeTemplate baseTemplate = base.deviceTypeRequest().template();
        final DeviceTypeTemplate overriddenTemplate = new DeviceTypeTemplate(
                applyMetadataOverride(baseTemplate.metadata(), request.metadataOverride()),
                mergeValues(baseTemplate.deviceTypeParamValues(), request.deviceTypeParamValues())
        );
        final String kind = firstNonBlank(request.kind(), base.kind());
        return new BranchDeviceTypeImportRequest(
                new EquipmentProfileDeviceTypeRequest(overriddenTemplate, true),
                mergeDeviceInstances(base.deviceInstances(), request.devices()),
                kind,
                base.onStartEvent(),
                base.onStopEvent(),
                base.onPublicStartEvent(),
                base.onPublicFinishEvent(),
                base.deviceTypeFunctions(),
                base.eventHandlers(),
                base.commands()
        );
    }

    private BranchImportRequest buildLegacyBranchImportRequestFromBytes(String branchId, List<byte[]> archives) {
        if (archives == null || archives.isEmpty()) {
            throw new IllegalArgumentException("zip payload must contain at least one .dtt file");
        }
        final List<BranchDeviceTypeImportRequest> deviceTypes = archives.stream()
                .map(facade::readDtt)
                .map(archive -> toBranchDeviceTypeImportRequest(archive, branchId))
                .toList();
        return new BranchImportRequest(branchId, branchId, deviceTypes);
    }

    private Map<String, byte[]> readDttFilesFromZipByEntryName(byte[] zipBytes) {
        if (zipBytes == null || zipBytes.length == 0) {
            throw new IllegalArgumentException("zip payload must not be empty");
        }
        final Map<String, byte[]> result = new LinkedHashMap<>();
        try (ZipInputStream input = new ZipInputStream(new ByteArrayInputStream(zipBytes))) {
            ZipEntry entry;
            while ((entry = input.getNextEntry()) != null) {
                if (!entry.isDirectory() && entry.getName() != null && entry.getName().toLowerCase().endsWith(".dtt")) {
                    result.put(entry.getName(), input.readAllBytes());
                }
            }
        } catch (IOException exception) {
            throw new IllegalArgumentException("Failed to read DTT zip payload", exception);
        }
        if (result.isEmpty()) {
            throw new IllegalArgumentException("zip payload does not contain any .dtt entries");
        }
        return result;
    }

    private byte[] resolveArchiveEntry(Map<String, byte[]> archivesByEntryName, String archiveEntryName) {
        if (archiveEntryName == null || archiveEntryName.isBlank()) {
            throw new IllegalArgumentException("archiveEntryName must not be blank");
        }
        final byte[] exact = archivesByEntryName.get(archiveEntryName);
        if (exact != null) {
            return exact;
        }
        final String normalized = normalizeArchiveEntryName(archiveEntryName);
        for (Map.Entry<String, byte[]> entry : archivesByEntryName.entrySet()) {
            if (normalizeArchiveEntryName(entry.getKey()).equalsIgnoreCase(normalized)) {
                return entry.getValue();
            }
        }
        throw new IllegalArgumentException("DTT archive entry not found in uploaded zip: " + archiveEntryName
                + ". Available entries: " + archivesByEntryName.keySet());
    }

    private String normalizeArchiveEntryName(String name) {
        final String normalized = name.replace('\\', '/');
        final int slashIndex = normalized.lastIndexOf('/');
        final String fileName = slashIndex >= 0 ? normalized.substring(slashIndex + 1) : normalized;
        return fileName.toLowerCase().endsWith(".dtt") ? fileName.substring(0, fileName.length() - 4) : fileName;
    }

    private EquipmentProfileAssemblyRequest toStructuredProfileAssemblyRequest(ImportDttSetToProfileRequest request) {
        Objects.requireNonNull(request, "request must not be null");
        final List<EquipmentProfileDeviceTypeRequest> deviceTypes = new ArrayList<>();
        if (request.archivesBase64() != null) {
            request.archivesBase64().forEach(archiveBase64 -> {
                final DttArchiveTemplate archive = facade.readDtt(decodeBase64Archive(archiveBase64));
                deviceTypes.add(new EquipmentProfileDeviceTypeRequest(toDeviceTypeTemplate(archive), true));
            });
        }
        if (request.deviceTypes() != null) {
            request.deviceTypes().forEach(item -> deviceTypes.add(toProfileDeviceTypeRequest(item)));
        }
        if (deviceTypes.isEmpty()) {
            throw new IllegalArgumentException("Either archivesBase64 or deviceTypes must contain at least one DTT archive");
        }
        return new EquipmentProfileAssemblyRequest(deviceTypes, List.of(), request.mergeStrategy());
    }

    private EquipmentProfileDeviceTypeRequest toProfileDeviceTypeRequest(ImportProfileDeviceTypeRequest request) {
        final DttArchiveTemplate archive = facade.readDtt(decodeBase64Archive(request.archiveBase64()));
        final DeviceTypeTemplate template = toDeviceTypeTemplate(archive);
        return new EquipmentProfileDeviceTypeRequest(
                new DeviceTypeTemplate(applyMetadataOverride(template.metadata(), request.metadataOverride()), mergeValues(template.deviceTypeParamValues(), request.deviceTypeParamValues())),
                true
        );
    }

    private BranchEquipmentAssemblyRequest toStructuredBranchAssemblyRequest(ImportDttSetToBranchRequest request) {
        Objects.requireNonNull(request, "request must not be null");
        final Map<String, BranchImportRequest> branches = new LinkedHashMap<>();
        if (request.archivesBase64() != null && request.branchIds() != null) {
            for (String branchId : request.branchIds()) {
                branches.put(branchId, buildLegacyBranchImportRequest(branchId, request.archivesBase64()));
            }
        }
        if (request.branches() != null) {
            request.branches().forEach(branchRequest -> mergeBranchImport(branches, toStructuredBranchImportRequest(branchRequest)));
        }
        if (branches.isEmpty()) {
            throw new IllegalArgumentException("Either legacy archivesBase64/branchIds or structured branches must contain at least one import target");
        }
        return new BranchEquipmentAssemblyRequest(new ArrayList<>(branches.values()), request.mergeStrategy());
    }

    private BranchEquipmentAssemblyRequest toStructuredBranchAssemblyRequest(ImportDttSetToExistingBranchRequest request) {
        Objects.requireNonNull(request, "request must not be null");
        final Map<String, BranchImportRequest> branches = new LinkedHashMap<>();
        if (request.archivesBase64() != null && request.branchIds() != null) {
            for (String branchId : request.branchIds()) {
                branches.put(branchId, buildLegacyBranchImportRequest(branchId, request.archivesBase64()));
            }
        }
        if (request.branches() != null) {
            request.branches().forEach(branchRequest -> mergeBranchImport(branches, toStructuredBranchImportRequest(branchRequest)));
        }
        if (branches.isEmpty()) {
            throw new IllegalArgumentException("Either legacy archivesBase64/branchIds or structured branches must contain at least one import target");
        }
        return new BranchEquipmentAssemblyRequest(new ArrayList<>(branches.values()), request.mergeStrategy());
    }

    private BranchImportRequest buildLegacyBranchImportRequest(String branchId, List<String> archivesBase64) {
        if (archivesBase64 == null || archivesBase64.isEmpty()) {
            throw new IllegalArgumentException("archivesBase64 must contain at least one DTT archive");
        }
        final List<BranchDeviceTypeImportRequest> deviceTypes = archivesBase64.stream()
                .map(this::decodeBase64Archive)
                .map(facade::readDtt)
                .map(archive -> toBranchDeviceTypeImportRequest(archive, branchId))
                .toList();
        return new BranchImportRequest(branchId, branchId, deviceTypes);
    }

    private BranchImportRequest toStructuredBranchImportRequest(ImportBranchRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("branch request must not be null");
        }
        if (request.branchId() == null || request.branchId().isBlank()) {
            throw new IllegalArgumentException("branchId must not be blank");
        }
        if (request.deviceTypes() == null || request.deviceTypes().isEmpty()) {
            throw new IllegalArgumentException("deviceTypes must contain at least one DTT archive for branch " + request.branchId());
        }
        final List<BranchDeviceTypeImportRequest> deviceTypes = request.deviceTypes().stream()
                .map(deviceTypeRequest -> toStructuredBranchDeviceTypeImportRequest(request.branchId(), deviceTypeRequest))
                .toList();
        final String displayName = request.displayName() == null || request.displayName().isBlank()
                ? request.branchId()
                : request.displayName();
        return new BranchImportRequest(request.branchId(), displayName, deviceTypes);
    }

    private BranchDeviceTypeImportRequest toStructuredBranchDeviceTypeImportRequest(String branchId, ImportBranchDeviceTypeRequest request) {
        final DttArchiveTemplate archive = facade.readDtt(decodeBase64Archive(request.archiveBase64()));
        final BranchDeviceTypeImportRequest base = toBranchDeviceTypeImportRequest(archive, branchId);
        final DeviceTypeTemplate baseTemplate = base.deviceTypeRequest().template();
        final DeviceTypeTemplate overriddenTemplate = new DeviceTypeTemplate(
                applyMetadataOverride(baseTemplate.metadata(), request.metadataOverride()),
                mergeValues(baseTemplate.deviceTypeParamValues(), request.deviceTypeParamValues())
        );
        final String kind = firstNonBlank(request.kind(), base.kind());
        return new BranchDeviceTypeImportRequest(
                new EquipmentProfileDeviceTypeRequest(overriddenTemplate, true),
                mergeDeviceInstances(base.deviceInstances(), request.devices()),
                kind,
                base.onStartEvent(),
                base.onStopEvent(),
                base.onPublicStartEvent(),
                base.onPublicFinishEvent(),
                base.deviceTypeFunctions(),
                base.eventHandlers(),
                base.commands()
        );
    }

    private List<DeviceInstanceImportRequest> mergeDeviceInstances(List<DeviceInstanceImportRequest> defaults,
                                                                   List<ImportBranchDeviceRequest> overrides) {
        final Map<String, DeviceInstanceImportRequest> merged = new LinkedHashMap<>();
        if (defaults != null) {
            defaults.forEach(item -> merged.put(resolveDeviceId(item.id(), item.name()), item));
        }
        if (overrides == null || overrides.isEmpty()) {
            return new ArrayList<>(merged.values());
        }
        for (ImportBranchDeviceRequest override : overrides) {
            final String key = resolveDeviceId(override.id(), override.name());
            final DeviceInstanceImportRequest existing = merged.get(key);
            final Map<String, Object> values = mergeValues(existing == null ? Map.of() : existing.deviceParamValues(), override.deviceParamValues());
            merged.put(key, new DeviceInstanceImportRequest(
                    key,
                    firstNonBlank(override.name(), existing == null ? null : existing.name(), key),
                    firstNonBlank(override.displayName(), existing == null ? null : existing.displayName(), firstNonBlank(override.name(), key)),
                    firstNonBlank(override.description(), existing == null ? null : existing.description()),
                    values
            ));
        }
        return new ArrayList<>(merged.values());
    }

    private String resolveDeviceId(String id, String name) {
        final String resolved = firstNonBlank(id, name);
        return resolved == null ? UUID.randomUUID().toString() : resolved;
    }

    private void mergeBranchImport(Map<String, BranchImportRequest> branches, BranchImportRequest incoming) {
        final BranchImportRequest existing = branches.get(incoming.branchId());
        if (existing == null) {
            branches.put(incoming.branchId(), incoming);
            return;
        }
        final List<BranchDeviceTypeImportRequest> mergedDeviceTypes = new ArrayList<>();
        if (existing.deviceTypes() != null) {
            mergedDeviceTypes.addAll(existing.deviceTypes());
        }
        if (incoming.deviceTypes() != null) {
            mergedDeviceTypes.addAll(incoming.deviceTypes());
        }
        branches.put(incoming.branchId(), new BranchImportRequest(
                incoming.branchId(),
                firstNonBlank(incoming.displayName(), existing.displayName(), incoming.branchId()),
                mergedDeviceTypes
        ));
    }

    private DeviceTypeTemplate toDeviceTypeTemplate(DttArchiveTemplate template) {
        final CanonicalProfileProjection projection = canonicalProjectionMapper.toProfileProjection(canonicalTemplateMapper.toCanonical(template));
        return new DeviceTypeTemplate(template.metadata(), projection.deviceTypeParamValues());
    }

    private BranchDeviceTypeImportRequest toBranchDeviceTypeImportRequest(DttArchiveTemplate template, String branchId) {
        final BranchDeviceTypeImportRequest branchSpecificImport = toBranchSpecificImportRequest(template, branchId);
        if (branchSpecificImport != null) {
            return branchSpecificImport;
        }
        final Map<String, Object> hints = template.bindingHints() == null ? Map.of() : template.bindingHints();
        final String kind = hints.get("deviceTypeKind") instanceof String kindHint && !kindHint.isBlank()
                ? kindHint
                : template.metadata().name();
        final CanonicalBranchProjection branchProjection = canonicalProjectionMapper.toBranchProjection(canonicalTemplateMapper.toCanonical(template), kind);
        return new BranchDeviceTypeImportRequest(
                new EquipmentProfileDeviceTypeRequest(
                        new DeviceTypeTemplate(template.metadata(), branchProjection.deviceTypeParamValues()),
                        true
                ),
                List.of(),
                branchProjection.kind(),
                toBranchScript(template.onStartEvent(), hints.get("onStartEvent")),
                toBranchScript(template.onStopEvent(), hints.get("onStopEvent")),
                toBranchScript(template.onPublicStartEvent(), hints.get("onPublicStartEvent")),
                toBranchScript(template.onPublicFinishEvent(), hints.get("onPublicFinishEvent")),
                template.deviceTypeFunctions(),
                toBranchScriptMap(template.eventHandlers(), hints.get("eventHandlers")),
                toBranchScriptMap(template.commands(), hints.get("commands"))
        );
    }

    private BranchDeviceTypeImportRequest toBranchSpecificImportRequest(DttArchiveTemplate template, String branchId) {
        final Map<String, Object> origin = template.templateOrigin() == null ? Map.of() : template.templateOrigin();
        if (!(origin.get("branchTopologyByBranchId") instanceof Map<?, ?> topologyByBranchRaw)) {
            return null;
        }
        final Map<String, Object> topologyByBranch = castToStringObjectMap(topologyByBranchRaw);
        if (!(topologyByBranch.get(branchId) instanceof Map<?, ?> branchTopologyRaw)) {
            return null;
        }
        final Map<String, Object> branchTopology = castToStringObjectMap(branchTopologyRaw);
        final DeviceTypeMetadata metadata = toMetadataFromSnapshot(template.metadata(), branchTopology.get("metadata"));
        final Map<String, Object> paramValues = toSnapshotValues(branchTopology.get("deviceTypeParamValues"));
        return new BranchDeviceTypeImportRequest(
                new EquipmentProfileDeviceTypeRequest(new DeviceTypeTemplate(metadata, paramValues), true),
                toDeviceImportRequests(branchTopology.get("devices")),
                toNullableString(branchTopology.get("kind")),
                toBranchScriptFromSnapshot(branchTopology.get("onStartEvent")),
                toBranchScriptFromSnapshot(branchTopology.get("onStopEvent")),
                toBranchScriptFromSnapshot(branchTopology.get("onPublicStartEvent")),
                toBranchScriptFromSnapshot(branchTopology.get("onPublicFinishEvent")),
                toNullableString(branchTopology.get("deviceTypeFunctions")),
                toBranchScriptMapFromSnapshot(branchTopology.get("eventHandlers")),
                toBranchScriptMapFromSnapshot(branchTopology.get("commands"))
        );
    }

    private BranchEquipment mergeBranchEquipment(BranchEquipment existing, BranchEquipment incoming, MergeStrategy mergeStrategy) {
        final Map<String, ru.aritmos.dtt.json.branch.BranchNode> mergedBranches = new LinkedHashMap<>(existing.branches());
        final MergeStrategy effectiveStrategy = mergeStrategy == null ? MergeStrategy.FAIL_IF_EXISTS : mergeStrategy;
        incoming.branches().forEach((branchId, incomingBranch) -> {
            final ru.aritmos.dtt.json.branch.BranchNode existingBranch = mergedBranches.get(branchId);
            if (existingBranch == null) {
                mergedBranches.put(branchId, incomingBranch);
                return;
            }
            final Map<String, ru.aritmos.dtt.json.branch.BranchDeviceType> mergedTypes = new LinkedHashMap<>(existingBranch.deviceTypes());
            incomingBranch.deviceTypes().forEach((typeId, incomingType) -> mergeBranchDeviceType(mergedTypes, typeId, incomingType, effectiveStrategy));
            mergedBranches.put(branchId, new ru.aritmos.dtt.json.branch.BranchNode(existingBranch.id(), existingBranch.displayName(), mergedTypes));
        });
        return new BranchEquipment(mergedBranches);
    }

    private void mergeBranchDeviceType(Map<String, ru.aritmos.dtt.json.branch.BranchDeviceType> deviceTypes,
                                       String typeId,
                                       ru.aritmos.dtt.json.branch.BranchDeviceType incomingType,
                                       MergeStrategy mergeStrategy) {
        if (!deviceTypes.containsKey(typeId)) {
            deviceTypes.put(typeId, incomingType);
            return;
        }
        final ru.aritmos.dtt.json.branch.BranchDeviceType existingType = deviceTypes.get(typeId);
        switch (mergeStrategy) {
            case FAIL_IF_EXISTS -> throw new IllegalArgumentException("Тип устройства '" + typeId + "' уже существует в отделении");
            case REPLACE -> deviceTypes.put(typeId, incomingType);
            case MERGE_NON_NULLS -> deviceTypes.put(typeId, mergeBranchNonNulls(existingType, incomingType));
            case MERGE_PRESERVE_EXISTING -> deviceTypes.put(typeId, mergeBranchPreserveExisting(existingType, incomingType));
            case CREATE_COPY_WITH_SUFFIX -> deviceTypes.put(nextCopyKey(deviceTypes, typeId), incomingType);
        }
    }

    private ru.aritmos.dtt.json.branch.BranchDeviceType mergeBranchNonNulls(ru.aritmos.dtt.json.branch.BranchDeviceType existing,
                                                                             ru.aritmos.dtt.json.branch.BranchDeviceType incoming) {
        final Map<String, ru.aritmos.dtt.json.branch.DeviceInstanceTemplate> mergedDevices = new LinkedHashMap<>(existing.devices());
        mergedDevices.putAll(incoming.devices());
        return new ru.aritmos.dtt.json.branch.BranchDeviceType(
                incoming.template() == null ? existing.template() : incoming.template(),
                mergedDevices,
                incoming.kind() == null ? existing.kind() : incoming.kind(),
                incoming.onStartEvent() == null ? existing.onStartEvent() : incoming.onStartEvent(),
                incoming.onStopEvent() == null ? existing.onStopEvent() : incoming.onStopEvent(),
                incoming.onPublicStartEvent() == null ? existing.onPublicStartEvent() : incoming.onPublicStartEvent(),
                incoming.onPublicFinishEvent() == null ? existing.onPublicFinishEvent() : incoming.onPublicFinishEvent(),
                incoming.deviceTypeFunctions() == null ? existing.deviceTypeFunctions() : incoming.deviceTypeFunctions(),
                mergeBranchScriptMaps(existing.eventHandlers(), incoming.eventHandlers(), true),
                mergeBranchScriptMaps(existing.commands(), incoming.commands(), true)
        );
    }

    private ru.aritmos.dtt.json.branch.BranchDeviceType mergeBranchPreserveExisting(ru.aritmos.dtt.json.branch.BranchDeviceType existing,
                                                                                      ru.aritmos.dtt.json.branch.BranchDeviceType incoming) {
        final Map<String, ru.aritmos.dtt.json.branch.DeviceInstanceTemplate> mergedDevices = new LinkedHashMap<>(incoming.devices());
        mergedDevices.putAll(existing.devices());
        return new ru.aritmos.dtt.json.branch.BranchDeviceType(
                existing.template() == null ? incoming.template() : existing.template(),
                mergedDevices,
                existing.kind() == null ? incoming.kind() : existing.kind(),
                existing.onStartEvent() == null ? incoming.onStartEvent() : existing.onStartEvent(),
                existing.onStopEvent() == null ? incoming.onStopEvent() : existing.onStopEvent(),
                existing.onPublicStartEvent() == null ? incoming.onPublicStartEvent() : existing.onPublicStartEvent(),
                existing.onPublicFinishEvent() == null ? incoming.onPublicFinishEvent() : existing.onPublicFinishEvent(),
                existing.deviceTypeFunctions() == null ? incoming.deviceTypeFunctions() : existing.deviceTypeFunctions(),
                mergeBranchScriptMaps(existing.eventHandlers(), incoming.eventHandlers(), false),
                mergeBranchScriptMaps(existing.commands(), incoming.commands(), false)
        );
    }

    private Map<String, BranchScript> mergeBranchScriptMaps(Map<String, BranchScript> existing,
                                                            Map<String, BranchScript> incoming,
                                                            boolean incomingWins) {
        final Map<String, BranchScript> merged = new LinkedHashMap<>();
        if (incomingWins) {
            if (existing != null) {
                merged.putAll(existing);
            }
            if (incoming != null) {
                merged.putAll(incoming);
            }
            return merged;
        }
        if (incoming != null) {
            merged.putAll(incoming);
        }
        if (existing != null) {
            merged.putAll(existing);
        }
        return merged;
    }

    private String nextCopyKey(Map<String, ?> result, String key) {
        int suffix = 1;
        String candidate = key + "_copy_" + suffix;
        while (result.containsKey(candidate)) {
            suffix++;
            candidate = key + "_copy_" + suffix;
        }
        return candidate;
    }

    private BranchScript toBranchScript(String scriptCode, Object metadataObject) {
        if (scriptCode == null && metadataObject == null) {
            return null;
        }
        final Map<String, Object> metadata = metadataObject instanceof Map<?, ?> map
                ? castToStringObjectMap(map)
                : Map.of();
        final Map<String, Object> input = metadata.get("inputParameters") instanceof Map<?, ?> inputMap
                ? castToStringObjectMap(inputMap)
                : Map.of();
        final List<Object> output = metadata.get("outputParameters") instanceof List<?> list
                ? new ArrayList<>(list)
                : List.of();
        return new BranchScript(input, output, scriptCode);
    }

    private Map<String, BranchScript> toBranchScriptMap(Map<String, String> scripts, Object metadataObject) {
        if ((scripts == null || scripts.isEmpty()) && !(metadataObject instanceof Map<?, ?>)) {
            return Map.of();
        }
        final Map<String, Object> metadataMap = metadataObject instanceof Map<?, ?> map
                ? castToStringObjectMap(map)
                : Map.of();
        final Map<String, BranchScript> result = new LinkedHashMap<>();
        if (scripts != null) {
            scripts.forEach((name, code) -> result.put(name, toBranchScript(code, metadataMap.get(name))));
        }
        metadataMap.forEach((name, metadata) -> result.putIfAbsent(name, toBranchScript(null, metadata)));
        return result;
    }

    private DeviceTypeMetadata toMetadataFromSnapshot(DeviceTypeMetadata fallback, Object metadataRaw) {
        if (!(metadataRaw instanceof Map<?, ?> metadataMapRaw)) {
            return fallback;
        }
        final Map<String, Object> metadata = castToStringObjectMap(metadataMapRaw);
        final String version = firstNonBlank(
                toNullableString(metadata.get("version")),
                fallback == null ? null : fallback.version()
        );
        final String iconBase64 = firstNonBlank(
                toNullableString(metadata.get("iconBase64")),
                toNullableString(metadata.get("imageBase64")),
                fallback == null ? null : fallback.iconBase64()
        );
        final String id = firstNonBlank(toNullableString(metadata.get("id")), fallback == null ? null : fallback.id());
        final String name = firstNonBlank(toNullableString(metadata.get("name")), fallback == null ? null : fallback.name(), id);
        final String displayName = firstNonBlank(
                toNullableString(metadata.get("displayName")),
                fallback == null ? null : fallback.displayName(),
                name
        );
        final String description = firstNonBlank(
                toNullableString(metadata.get("description")),
                fallback == null ? null : fallback.description(),
                name,
                ""
        );
        return new DeviceTypeMetadata(
                id,
                name,
                displayName,
                description,
                version,
                iconBase64
        );
    }

    private Map<String, Object> toSnapshotValues(Object valuesRaw) {
        if (!(valuesRaw instanceof Map<?, ?> map)) {
            return Map.of();
        }
        return castToStringObjectMap(map);
    }

    private List<DeviceInstanceImportRequest> toDeviceImportRequests(Object devicesRaw) {
        if (!(devicesRaw instanceof Map<?, ?> devicesMapRaw)) {
            return List.of();
        }
        final Map<String, Object> devicesMap = castToStringObjectMap(devicesMapRaw);
        final List<DeviceInstanceImportRequest> result = new ArrayList<>();
        devicesMap.forEach((deviceId, deviceRaw) -> {
            if (!(deviceRaw instanceof Map<?, ?> deviceMapRaw)) {
                return;
            }
            final Map<String, Object> deviceMap = castToStringObjectMap(deviceMapRaw);
            final Map<String, Object> params = deviceMap.get("deviceParamValues") == null
                    ? null
                    : toSnapshotValues(deviceMap.get("deviceParamValues"));
            result.add(new DeviceInstanceImportRequest(
                    toNullableString(deviceMap.getOrDefault("id", deviceId)),
                    toNullableString(deviceMap.get("name")),
                    toNullableString(deviceMap.get("displayName")),
                    toNullableString(deviceMap.get("description")),
                    params
            ));
        });
        return result;
    }

    private BranchScript toBranchScriptFromSnapshot(Object scriptRaw) {
        if (!(scriptRaw instanceof Map<?, ?> scriptMapRaw)) {
            return null;
        }
        final Map<String, Object> scriptMap = castToStringObjectMap(scriptMapRaw);
        final Map<String, Object> input = scriptMap.get("inputParameters") instanceof Map<?, ?> inputMapRaw
                ? castToStringObjectMap(inputMapRaw)
                : Map.of();
        final List<Object> output = scriptMap.get("outputParameters") instanceof List<?> outputList
                ? new ArrayList<>(outputList)
                : List.of();
        return new BranchScript(input, output, toNullableString(scriptMap.get("scriptCode")));
    }

    private Map<String, BranchScript> toBranchScriptMapFromSnapshot(Object scriptsRaw) {
        if (!(scriptsRaw instanceof Map<?, ?> scriptsMapRaw)) {
            return Map.of();
        }
        final Map<String, Object> scriptsMap = castToStringObjectMap(scriptsMapRaw);
        final Map<String, BranchScript> result = new LinkedHashMap<>();
        scriptsMap.forEach((name, scriptRaw) -> result.put(name, toBranchScriptFromSnapshot(scriptRaw)));
        result.values().removeIf(Objects::isNull);
        return result;
    }

    private Map<String, Object> castToStringObjectMap(Map<?, ?> source) {
        final Map<String, Object> result = new LinkedHashMap<>();
        source.forEach((key, value) -> result.put(String.valueOf(key), value));
        return result;
    }

    private Map<String, Object> mergeValues(Map<String, Object> base, Map<String, Object> overrides) {
        final Map<String, Object> merged = new LinkedHashMap<>();
        if (base != null) {
            merged.putAll(base);
        }
        if (overrides != null) {
            merged.putAll(overrides);
        }
        return merged;
    }

    private byte[] decodeBase64Archive(String archiveBase64) {
        if (archiveBase64 == null || archiveBase64.isBlank()) {
            throw new IllegalArgumentException("DTT archive payload must not be blank");
        }
        return Base64.getDecoder().decode(archiveBase64);
    }

    private String toNullableString(Object value) {
        return value == null ? null : String.valueOf(value);
    }

    private <T> T firstNonNull(T preferred, T fallback) {
        return preferred != null ? preferred : fallback;
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
        final ExportAllDttFromBranchResponse exported = exportAllDttFromBranch(
                branchEquipment,
                branchIds,
                List.of(deviceTypeId),
                mergeStrategy,
                dttVersion
        );
        final String archiveBase64 = exported.archivesBase64ByDeviceTypeId().get(deviceTypeId);
        if (archiveBase64 == null || archiveBase64.isBlank()) {
            throw new IllegalArgumentException("deviceTypeId not found in branch equipment: " + deviceTypeId);
        }
        return new ExportSingleDttResponse(deviceTypeId, archiveBase64);
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
        return Base64.getDecoder().decode(
                exportSingleDttFromBranch(branchEquipment, branchIds, deviceTypeId, mergeStrategy, dttVersion).archiveBase64()
        );
    }

    /**
     * Экспортирует один DTT-архив из строкового branch equipment JSON как бинарный payload.
     */
    public byte[] exportSingleDttFromBranchJsonToBytes(JsonNode branchJson,
                                                       List<String> branchIds,
                                                       String deviceTypeId,
                                                       MergeStrategy mergeStrategy,
                                                       String dttVersion) {
        return Base64.getDecoder().decode(
                exportSingleDttFromBranchJson(branchJson, branchIds, deviceTypeId, mergeStrategy, dttVersion).archiveBase64()
        );
    }

    /**
     * Выполняет preview single-export из branch equipment JSON.
     */
    public SingleDttExportPreviewResponse previewSingleDttExportFromBranch(BranchEquipment branchEquipment,
                                                                            List<String> branchIds,
                                                                            String deviceTypeId,
                                                                            MergeStrategy mergeStrategy,
                                                                            String dttVersion) {
        try {
            final byte[] payload = exportSingleDttFromBranchToBytes(branchEquipment, branchIds, deviceTypeId, mergeStrategy, dttVersion);
            return new SingleDttExportPreviewResponse(true, deviceTypeId, payload.length, List.of());
        } catch (RuntimeException exception) {
            return failedPreview(deviceTypeId, exception, true);
        }
    }

    /**
     * Выполняет preview single-export из строкового branch equipment JSON.
     */
    public SingleDttExportPreviewResponse previewSingleDttExportFromBranchJson(JsonNode branchJson,
                                                                                List<String> branchIds,
                                                                                String deviceTypeId,
                                                                                MergeStrategy mergeStrategy,
                                                                                String dttVersion) {
        try {
            final byte[] payload = exportSingleDttFromBranchJsonToBytes(branchJson, branchIds, deviceTypeId, mergeStrategy, dttVersion);
            return new SingleDttExportPreviewResponse(true, deviceTypeId, payload.length, List.of());
        } catch (RuntimeException exception) {
            return failedPreview(deviceTypeId, exception, true);
        }
    }

    private SingleDttExportPreviewResponse failedPreview(String deviceTypeId,
                                                         RuntimeException exception,
                                                         boolean preferMergeConflictCode) {
        final String message = exception.getMessage() == null ? exception.getClass().getSimpleName() : exception.getMessage();
        final String messageLower = message.toLowerCase();
        final boolean mergeLike = messageLower.contains("merge")
                || messageLower.contains("conflict")
                || messageLower.contains("already exists");
        final String code = (mergeLike || preferMergeConflictCode) ? "MERGE_CONFLICT" : "EXPORT_PREVIEW_ERROR";
        return new SingleDttExportPreviewResponse(
                false,
                deviceTypeId,
                null,
                List.of(new SingleDttExportPreviewIssueResponse(code, message))
        );
    }

    /**
     * Экспортирует все DTT-архивы из profile JSON в zip-файл (upload-download режим).
     */
    public byte[] exportProfileToZip(EquipmentProfile profile,
                                     JsonNode profileJson,
                                     List<String> deviceTypeIds,
                                     String dttVersion) {
        final var exported = profile != null
                ? facade.exportProfileToDttZip(new ProfileExportRequest(profile, deviceTypeIds, dttVersion))
                : facade.exportProfileToDttZip(new ProfileExportRequest(facade.parseProfileJson(toCompactJson(profileJson)), deviceTypeIds, dttVersion));
        return exported;
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
        final BranchEquipment source = branchEquipment != null ? branchEquipment : facade.parseBranchJson(toCompactJson(branchJson));
        return facade.exportBranchToDttZip(new BranchEquipmentExportRequest(
                source,
                branchIds,
                deviceTypeIds,
                mergeStrategy,
                dttVersion
        ));
    }


    private DeviceTypeMetadata applyMetadataOverride(DeviceTypeMetadata base, ImportDeviceTypeMetadataOverrideRequest override) {
        if (override == null) {
            return base;
        }
        final String id = firstNonBlank(override.id(), base == null ? null : base.id());
        final String name = firstNonBlank(override.name(), base == null ? null : base.name(), id);
        final String displayName = firstNonBlank(override.displayName(), base == null ? null : base.displayName(), name);
        final String description = firstNonBlank(
                override.description(),
                base == null ? null : base.description(),
                name,
                id,
                ""
        );
        return new DeviceTypeMetadata(
                id,
                name,
                displayName,
                description,
                firstNonBlank(override.version(), base == null ? null : base.version()),
                firstNonBlank(override.iconBase64(), base == null ? null : base.iconBase64())
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
