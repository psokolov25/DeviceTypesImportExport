package ru.aritmos.dtt.demo.service;

import jakarta.inject.Singleton;
import ru.aritmos.dtt.api.DeviceTemplateLibraryFacade;
import ru.aritmos.dtt.api.dto.MergeStrategy;
import ru.aritmos.dtt.api.dto.ProfileExportRequest;
import ru.aritmos.dtt.api.dto.branch.BranchEquipmentExportRequest;
import ru.aritmos.dtt.archive.model.DttArchiveTemplate;
import ru.aritmos.dtt.demo.dto.DttInspectionResponse;
import ru.aritmos.dtt.demo.dto.DttValidationIssueResponse;
import ru.aritmos.dtt.demo.dto.DttValidationResponse;
import ru.aritmos.dtt.demo.dto.ExportAllDttFromBranchResponse;
import ru.aritmos.dtt.demo.dto.ExportAllDttFromProfileResponse;
import ru.aritmos.dtt.demo.dto.ImportDttSetToBranchResponse;
import ru.aritmos.dtt.demo.dto.ImportDttSetToProfileResponse;
import ru.aritmos.dtt.json.branch.BranchEquipment;
import ru.aritmos.dtt.json.profile.EquipmentProfile;

import java.util.List;
import java.util.Map;

/**
 * Сервис demo-модуля для валидации и инспекции DTT-архивов.
 */
@Singleton
public class DttDemoService {

    private final DeviceTemplateLibraryFacade facade;

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
                template.eventHandlers() == null ? 0 : template.eventHandlers().size(),
                template.commands() == null ? 0 : template.commands().size()
        );
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
        return new ImportDttSetToProfileResponse(profileJson, deviceTypesCount);
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
        return new ImportDttSetToProfileResponse(profileJson, deviceTypesCount);
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
    public ExportAllDttFromProfileResponse exportAllDttFromProfileJson(String profileJson,
                                                                       List<String> deviceTypeIds,
                                                                       String dttVersion) {
        return exportAllDttFromProfile(facade.parseProfileJson(profileJson), deviceTypeIds, dttVersion);
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
        final String branchJson = facade.toBranchJson(branch);
        final int branchesCount = branch.branches() == null ? 0 : branch.branches().size();
        return new ImportDttSetToBranchResponse(branchJson, branchesCount);
    }

    /**
     * Импортирует набор DTT-архивов в branch equipment JSON из Base64-представления.
     */
    public ImportDttSetToBranchResponse importDttSetToBranchBase64(List<String> archivesBase64,
                                                                   List<String> branchIds,
                                                                   MergeStrategy mergeStrategy) {
        final var branch = facade.importDttBase64SetToBranch(archivesBase64, branchIds, mergeStrategy);
        final String branchJson = facade.toBranchJson(branch);
        final int branchesCount = branch.branches() == null ? 0 : branch.branches().size();
        return new ImportDttSetToBranchResponse(branchJson, branchesCount);
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
        final String branchJson = facade.toBranchJson(branch);
        final int branchesCount = branch.branches() == null ? 0 : branch.branches().size();
        return new ImportDttSetToBranchResponse(branchJson, branchesCount);
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
        return new ImportDttSetToProfileResponse(profileJson, deviceTypesCount);
    }

    /**
     * Выполняет preview-сборку profile JSON из Base64-представления набора DTT-архивов.
     */
    public ImportDttSetToProfileResponse previewDttSetToProfileBase64(List<String> archivesBase64, MergeStrategy mergeStrategy) {
        final var profile = facade.previewDttBase64SetToProfile(archivesBase64, mergeStrategy);
        final String profileJson = facade.toProfileJson(profile);
        final int deviceTypesCount = profile.deviceTypes() == null ? 0 : profile.deviceTypes().size();
        return new ImportDttSetToProfileResponse(profileJson, deviceTypesCount);
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
        final String branchJson = facade.toBranchJson(branch);
        final int branchesCount = branch.branches() == null ? 0 : branch.branches().size();
        return new ImportDttSetToBranchResponse(branchJson, branchesCount);
    }

    /**
     * Выполняет preview-сборку branch equipment JSON из Base64-представления набора DTT-архивов.
     */
    public ImportDttSetToBranchResponse previewDttSetToBranchBase64(List<String> archivesBase64,
                                                                    List<String> branchIds,
                                                                    MergeStrategy mergeStrategy) {
        final var branch = facade.previewDttBase64SetToBranch(archivesBase64, branchIds, mergeStrategy);
        final String branchJson = facade.toBranchJson(branch);
        final int branchesCount = branch.branches() == null ? 0 : branch.branches().size();
        return new ImportDttSetToBranchResponse(branchJson, branchesCount);
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
        return new ImportDttSetToProfileResponse(profileJson, deviceTypesCount);
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
        final String branchJson = facade.toBranchJson(branch);
        final int branchesCount = branch.branches() == null ? 0 : branch.branches().size();
        return new ImportDttSetToBranchResponse(branchJson, branchesCount);
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
        return new ImportDttSetToProfileResponse(profileJson, deviceTypesCount);
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
        final String branchJson = facade.toBranchJson(branch);
        final int branchesCount = branch.branches() == null ? 0 : branch.branches().size();
        return new ImportDttSetToBranchResponse(branchJson, branchesCount);
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
    public ExportAllDttFromBranchResponse exportAllDttFromBranchJson(String branchJson,
                                                                     List<String> branchIds,
                                                                     List<String> deviceTypeIds,
                                                                     MergeStrategy mergeStrategy,
                                                                     String dttVersion) {
        return exportAllDttFromBranch(facade.parseBranchJson(branchJson), branchIds, deviceTypeIds, mergeStrategy, dttVersion);
    }

    /**
     * Экспортирует все DTT-архивы из profile JSON в zip-файл (upload-download режим).
     */
    public byte[] exportProfileToZip(EquipmentProfile profile,
                                     String profileJson,
                                     List<String> deviceTypeIds,
                                     String dttVersion) {
        final var exported = profile != null
                ? facade.exportProfileToDttZip(new ProfileExportRequest(profile, deviceTypeIds, dttVersion))
                : facade.exportProfileToDttZip(new ProfileExportRequest(facade.parseProfileJson(profileJson), deviceTypeIds, dttVersion));
        return exported;
    }

    /**
     * Экспортирует все DTT-архивы из branch equipment JSON в zip-файл (upload-download режим).
     */
    public byte[] exportBranchToZip(BranchEquipment branchEquipment,
                                    String branchJson,
                                    List<String> branchIds,
                                    List<String> deviceTypeIds,
                                    MergeStrategy mergeStrategy,
                                    String dttVersion) {
        final BranchEquipment source = branchEquipment != null ? branchEquipment : facade.parseBranchJson(branchJson);
        return facade.exportBranchToDttZip(new BranchEquipmentExportRequest(
                source,
                branchIds,
                deviceTypeIds,
                mergeStrategy,
                dttVersion
        ));
    }
}
