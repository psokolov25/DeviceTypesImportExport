package ru.aritmos.dtt.demo.service;

import jakarta.inject.Singleton;
import ru.aritmos.dtt.api.DeviceTemplateLibrary;
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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

/**
 * Сервис demo-модуля для валидации и инспекции DTT-архивов.
 */
@Singleton
public class DttDemoService {

    private final DeviceTemplateLibraryFacade facade;

    /**
     * Создаёт demo-сервис с дефолтной конфигурацией фасада библиотеки.
     */
    public DttDemoService() {
        this(DeviceTemplateLibrary.createDefaultFacade());
    }

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
     * Экспортирует все DTT-архивы из profile JSON.
     *
     * @param profile модель profile JSON
     * @return Base64-представление экспортированных DTT-архивов
     */
    public ExportAllDttFromProfileResponse exportAllDttFromProfile(EquipmentProfile profile, List<String> deviceTypeIds) {
        final var exported = facade.exportDttSetFromProfile(new ProfileExportRequest(profile, deviceTypeIds));
        final Map<String, String> encoded = exported.archivesByDeviceTypeId().entrySet().stream()
                .collect(java.util.stream.Collectors.toMap(
                        Map.Entry::getKey,
                        entry -> Base64.getEncoder().encodeToString(entry.getValue()),
                        (left, right) -> right,
                        java.util.LinkedHashMap::new
                ));
        return new ExportAllDttFromProfileResponse(encoded, encoded.size());
    }

    /**
     * Экспортирует все DTT-архивы из строкового profile JSON.
     *
     * @param profileJson строковое представление profile JSON
     * @param deviceTypeIds опциональный фильтр типов устройств
     * @return Base64-представление экспортированных DTT-архивов
     */
    public ExportAllDttFromProfileResponse exportAllDttFromProfileJson(String profileJson, List<String> deviceTypeIds) {
        return exportAllDttFromProfile(facade.parseProfileJson(profileJson), deviceTypeIds);
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
     * Импортирует zip-набор DTT архивов в profile JSON.
     *
     * @param zipBytes zip-архив с файлами .dtt
     * @param mergeStrategy стратегия merge
     * @return результат импорта профиля
     */
    public ImportDttSetToProfileResponse importDttZipToProfile(byte[] zipBytes, MergeStrategy mergeStrategy) {
        return importDttSetToProfile(readDttFilesFromZip(zipBytes), mergeStrategy);
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
        return importDttSetToBranch(readDttFilesFromZip(zipBytes), branchIds, mergeStrategy);
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
                                                                 MergeStrategy mergeStrategy) {
        final var exported = facade.exportDttSetFromBranch(new BranchEquipmentExportRequest(
                branchEquipment,
                branchIds,
                deviceTypeIds,
                mergeStrategy
        ));
        final Map<String, String> encoded = exported.archivesByDeviceTypeId().entrySet().stream()
                .collect(java.util.stream.Collectors.toMap(
                        Map.Entry::getKey,
                        entry -> Base64.getEncoder().encodeToString(entry.getValue()),
                        (left, right) -> right,
                        java.util.LinkedHashMap::new
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
                                                                     MergeStrategy mergeStrategy) {
        return exportAllDttFromBranch(facade.parseBranchJson(branchJson), branchIds, deviceTypeIds, mergeStrategy);
    }

    /**
     * Экспортирует все DTT-архивы из profile JSON в zip-файл (upload-download режим).
     */
    public byte[] exportProfileToZip(EquipmentProfile profile, String profileJson, List<String> deviceTypeIds) {
        final var exported = profile != null
                ? facade.exportDttSetFromProfile(new ProfileExportRequest(profile, deviceTypeIds))
                : facade.exportDttSetFromProfile(new ProfileExportRequest(facade.parseProfileJson(profileJson), deviceTypeIds));
        return writeDttZip(exported.archivesByDeviceTypeId());
    }

    /**
     * Экспортирует все DTT-архивы из branch equipment JSON в zip-файл (upload-download режим).
     */
    public byte[] exportBranchToZip(BranchEquipment branchEquipment,
                                    String branchJson,
                                    List<String> branchIds,
                                    List<String> deviceTypeIds,
                                    MergeStrategy mergeStrategy) {
        final BranchEquipment source = branchEquipment != null ? branchEquipment : facade.parseBranchJson(branchJson);
        final var exported = facade.exportDttSetFromBranch(new BranchEquipmentExportRequest(
                source,
                branchIds,
                deviceTypeIds,
                mergeStrategy
        ));
        return writeDttZip(exported.archivesByDeviceTypeId());
    }

    private List<byte[]> readDttFilesFromZip(byte[] zipBytes) {
        try (ZipInputStream input = new ZipInputStream(new ByteArrayInputStream(zipBytes))) {
            final List<byte[]> result = new java.util.ArrayList<>();
            ZipEntry entry;
            while ((entry = input.getNextEntry()) != null) {
                if (entry.isDirectory() || !entry.getName().endsWith(".dtt")) {
                    continue;
                }
                result.add(input.readAllBytes());
            }
            if (result.isEmpty()) {
                throw new IllegalArgumentException("Zip must contain at least one .dtt file");
            }
            return result;
        } catch (IOException exception) {
            throw new IllegalArgumentException("Invalid DTT zip payload", exception);
        }
    }

    private byte[] writeDttZip(Map<String, byte[]> archivesByDeviceTypeId) {
        try (ByteArrayOutputStream output = new ByteArrayOutputStream();
             ZipOutputStream zipOutput = new ZipOutputStream(output)) {
            final Map<String, byte[]> ordered = new LinkedHashMap<>(archivesByDeviceTypeId);
            for (Map.Entry<String, byte[]> entry : ordered.entrySet()) {
                zipOutput.putNextEntry(new ZipEntry(entry.getKey() + ".dtt"));
                zipOutput.write(entry.getValue());
                zipOutput.closeEntry();
            }
            zipOutput.finish();
            return output.toByteArray();
        } catch (IOException exception) {
            throw new IllegalStateException("Unable to build DTT zip payload", exception);
        }
    }
}
