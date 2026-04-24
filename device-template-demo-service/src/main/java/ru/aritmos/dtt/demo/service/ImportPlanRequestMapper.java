package ru.aritmos.dtt.demo.service;

import jakarta.inject.Singleton;
import ru.aritmos.dtt.api.dto.DeviceTypeMetadata;
import ru.aritmos.dtt.api.dto.importplan.BranchDeviceTypeMetadataOverrideImportRequest;
import ru.aritmos.dtt.api.dto.importplan.BranchImportPlanRequest;
import ru.aritmos.dtt.api.dto.importplan.BranchMetadataImportRequest;
import ru.aritmos.dtt.api.dto.importplan.ProfileBranchMetadataImportPlanRequest;
import ru.aritmos.dtt.api.dto.importplan.ProfileDeviceTypeImportSourceRequest;
import ru.aritmos.dtt.api.dto.importplan.ProfileImportPlanRequest;
import ru.aritmos.dtt.demo.dto.ImportBranchMetadataRequest;
import ru.aritmos.dtt.demo.dto.ImportDeviceTypeMetadataOverrideRequest;
import ru.aritmos.dtt.demo.dto.ImportDttSetToBranchRequest;
import ru.aritmos.dtt.demo.dto.ImportDttSetToExistingBranchRequest;
import ru.aritmos.dtt.demo.dto.ImportDttSetToProfileRequest;
import ru.aritmos.dtt.demo.dto.ImportDttZipToBranchUploadRequest;
import ru.aritmos.dtt.demo.dto.ImportDttZipToExistingBranchUploadRequest;
import ru.aritmos.dtt.demo.dto.ImportDttZipToProfileUploadRequest;
import ru.aritmos.dtt.demo.dto.ImportProfileBranchWithMetadataRequest;
import ru.aritmos.dtt.demo.dto.ImportProfileDeviceTypeRequest;

import java.util.List;

/**
 * Адаптер преобразования transport DTO demo-service в import-plan DTO библиотеки.
 *
 * <p>Класс вынесен из {@link DttDemoService}, чтобы сервис оставался тонким оркестратором
 * и не содержал дублирующую mapping-логику для structured/zip/base64 сценариев.
 */
@Singleton
public class ImportPlanRequestMapper {

    /**
     * Преобразует запрос импорта profile+branch с metadata inheritance в DTO фасада библиотеки.
     */
    public ProfileBranchMetadataImportPlanRequest toLibraryProfileBranchMetadataImportPlan(ImportProfileBranchWithMetadataRequest request) {
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

    /**
     * Преобразует запрос structured profile-импорта.
     */
    public ProfileImportPlanRequest toLibraryProfileImportPlan(ImportDttSetToProfileRequest request) {
        return new ProfileImportPlanRequest(
                request == null ? null : request.archivesBase64(),
                request == null ? null : request.mergeStrategy(),
                request == null || request.deviceTypes() == null ? List.of() : request.deviceTypes()
        );
    }

    /**
     * Преобразует zip upload запрос structured profile-импорта.
     */
    public ProfileImportPlanRequest toLibraryProfileImportPlan(ImportDttZipToProfileUploadRequest request) {
        return new ProfileImportPlanRequest(
                null,
                request == null ? null : request.mergeStrategy(),
                request == null || request.deviceTypes() == null ? List.of() : request.deviceTypes()
        );
    }

    /**
     * Преобразует запрос structured branch-импорта.
     */
    public BranchImportPlanRequest toLibraryBranchImportPlan(ImportDttSetToBranchRequest request) {
        return new BranchImportPlanRequest(
                request == null ? null : request.archivesBase64(),
                request == null ? null : request.branchIds(),
                request == null ? null : request.mergeStrategy(),
                request == null || request.branches() == null ? List.of() : request.branches()
        );
    }

    /**
     * Преобразует запрос structured merge-импорта в existing branch JSON.
     */
    public BranchImportPlanRequest toLibraryBranchImportPlan(ImportDttSetToExistingBranchRequest request) {
        return new BranchImportPlanRequest(
                request == null ? null : request.archivesBase64(),
                request == null ? null : request.branchIds(),
                request == null ? null : request.mergeStrategy(),
                request == null || request.branches() == null ? List.of() : request.branches()
        );
    }

    /**
     * Преобразует zip upload запрос structured branch-импорта.
     */
    public BranchImportPlanRequest toLibraryBranchImportPlan(ImportDttZipToBranchUploadRequest request) {
        return new BranchImportPlanRequest(
                null,
                request == null ? null : request.branchIds(),
                request == null ? null : request.mergeStrategy(),
                request == null || request.branches() == null ? List.of() : request.branches()
        );
    }

    /**
     * Преобразует zip upload запрос structured merge-импорта в existing branch JSON.
     */
    public BranchImportPlanRequest toLibraryBranchImportPlan(ImportDttZipToExistingBranchUploadRequest request) {
        return new BranchImportPlanRequest(
                null,
                request == null ? null : request.branchIds(),
                request == null ? null : request.mergeStrategy(),
                request == null || request.branches() == null ? List.of() : request.branches()
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

    private ProfileDeviceTypeImportSourceRequest toLibraryProfileSource(ImportProfileDeviceTypeRequest request) {
        return new ProfileDeviceTypeImportSourceRequest(
                request.archiveBase64(),
                null,
                request.deviceTypeParamValues(),
                toLibraryMetadataOverride(request.metadataOverride())
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
}
