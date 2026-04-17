package ru.aritmos.dtt.api;

import ru.aritmos.dtt.api.dto.BatchDttExportResult;
import ru.aritmos.dtt.api.dto.DeviceTypeMetadata;
import ru.aritmos.dtt.api.dto.DeviceTypeTemplate;
import ru.aritmos.dtt.api.dto.EquipmentProfileAssemblyRequest;
import ru.aritmos.dtt.api.dto.EquipmentProfileDeviceTypeRequest;
import ru.aritmos.dtt.api.dto.MergeStrategy;
import ru.aritmos.dtt.api.dto.ProfileExportRequest;
import ru.aritmos.dtt.api.dto.ValidationResult;
import ru.aritmos.dtt.api.dto.branch.BranchDeviceTypeImportRequest;
import ru.aritmos.dtt.api.dto.branch.BranchEquipmentAssemblyRequest;
import ru.aritmos.dtt.api.dto.branch.BranchEquipmentExportRequest;
import ru.aritmos.dtt.api.dto.branch.BranchImportRequest;
import ru.aritmos.dtt.archive.DefaultDttArchiveReader;
import ru.aritmos.dtt.archive.DefaultDttArchiveWriter;
import ru.aritmos.dtt.archive.DttArchiveReader;
import ru.aritmos.dtt.archive.DttArchiveWriter;
import ru.aritmos.dtt.archive.model.DttArchiveDescriptor;
import ru.aritmos.dtt.archive.model.DttArchiveTemplate;
import ru.aritmos.dtt.assembly.DefaultTemplateAssemblyService;
import ru.aritmos.dtt.exception.TemplateAssemblyException;
import ru.aritmos.dtt.json.branch.BranchEquipment;
import ru.aritmos.dtt.json.branch.DefaultDeviceManagerBranchJsonGenerator;
import ru.aritmos.dtt.json.branch.DefaultDeviceManagerBranchJsonParser;
import ru.aritmos.dtt.json.branch.DeviceManagerBranchJsonGenerator;
import ru.aritmos.dtt.json.branch.DeviceManagerBranchJsonParser;
import ru.aritmos.dtt.json.profile.DefaultEquipmentProfileJsonGenerator;
import ru.aritmos.dtt.json.profile.DefaultEquipmentProfileJsonParser;
import ru.aritmos.dtt.json.profile.EquipmentProfile;
import ru.aritmos.dtt.json.profile.EquipmentProfileJsonGenerator;
import ru.aritmos.dtt.json.profile.EquipmentProfileJsonParser;
import ru.aritmos.dtt.validation.DefaultTemplateValidationService;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Base64;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

/**
 * Дефолтная фасадная реализация для удобного использования библиотеки как Maven-зависимости.
 */
public class DefaultDeviceTemplateLibraryFacade implements DeviceTemplateLibraryFacade {

    private final DttArchiveReader archiveReader;
    private final DttArchiveWriter archiveWriter;
    private final TemplateValidationService validationService;
    private final TemplateAssemblyService assemblyService;
    private final EquipmentProfileJsonParser profileJsonParser;
    private final EquipmentProfileJsonGenerator profileJsonGenerator;
    private final DeviceManagerBranchJsonParser branchJsonParser;
    private final DeviceManagerBranchJsonGenerator branchJsonGenerator;

    /**
     * Создаёт фасад со стандартными реализациями сервисов библиотеки.
     */
    public DefaultDeviceTemplateLibraryFacade() {
        this(
                new DefaultDttArchiveReader(),
                new DefaultDttArchiveWriter(),
                new DefaultTemplateValidationService(),
                new DefaultTemplateAssemblyService(),
                new DefaultEquipmentProfileJsonParser(),
                new DefaultEquipmentProfileJsonGenerator(),
                new DefaultDeviceManagerBranchJsonParser(),
                new DefaultDeviceManagerBranchJsonGenerator()
        );
    }

    /**
     * Создаёт фасад с явно переданными зависимостями.
     */
    public DefaultDeviceTemplateLibraryFacade(
            DttArchiveReader archiveReader,
            DttArchiveWriter archiveWriter,
            TemplateValidationService validationService,
            TemplateAssemblyService assemblyService,
            EquipmentProfileJsonParser profileJsonParser,
            EquipmentProfileJsonGenerator profileJsonGenerator,
            DeviceManagerBranchJsonParser branchJsonParser,
            DeviceManagerBranchJsonGenerator branchJsonGenerator
    ) {
        this.archiveReader = Objects.requireNonNull(archiveReader, "archiveReader is required");
        this.archiveWriter = Objects.requireNonNull(archiveWriter, "archiveWriter is required");
        this.validationService = Objects.requireNonNull(validationService, "validationService is required");
        this.assemblyService = Objects.requireNonNull(assemblyService, "assemblyService is required");
        this.profileJsonParser = Objects.requireNonNull(profileJsonParser, "profileJsonParser is required");
        this.profileJsonGenerator = Objects.requireNonNull(profileJsonGenerator, "profileJsonGenerator is required");
        this.branchJsonParser = Objects.requireNonNull(branchJsonParser, "branchJsonParser is required");
        this.branchJsonGenerator = Objects.requireNonNull(branchJsonGenerator, "branchJsonGenerator is required");
    }

    @Override
    public DttArchiveTemplate readDtt(byte[] archiveBytes) {
        Objects.requireNonNull(archiveBytes, "archiveBytes is required");
        return archiveReader.read(new ByteArrayInputStream(archiveBytes));
    }

    @Override
    public byte[] writeDtt(DttArchiveTemplate template) {
        final ByteArrayOutputStream output = new ByteArrayOutputStream();
        archiveWriter.write(template, output);
        return output.toByteArray();
    }

    @Override
    public ValidationResult validate(DttArchiveTemplate template) {
        return validationService.validate(template);
    }

    @Override
    public ValidationResult validate(byte[] archiveBytes) {
        return validate(readDtt(archiveBytes));
    }

    @Override
    public EquipmentProfile assembleProfile(EquipmentProfileAssemblyRequest request) {
        return assemblyService.assembleEquipmentProfile(request);
    }

    @Override
    public BranchEquipment assembleBranch(BranchEquipmentAssemblyRequest request) {
        return assemblyService.assembleBranchEquipment(request);
    }

    @Override
    public EquipmentProfile parseProfileJson(String json) {
        return profileJsonParser.parse(json);
    }

    @Override
    public BranchEquipment parseBranchJson(String json) {
        return branchJsonParser.parse(json);
    }

    @Override
    public String toProfileJson(EquipmentProfile profile) {
        return profileJsonGenerator.generate(profile);
    }

    @Override
    public String toBranchJson(BranchEquipment branchEquipment) {
        return branchJsonGenerator.generate(branchEquipment);
    }

    @Override
    public BatchDttExportResult exportDttSetFromProfile(EquipmentProfile profile) {
        return exportDttSetFromProfile(new ProfileExportRequest(profile, List.of(), null));
    }

    @Override
    public BatchDttExportResult exportDttSetFromProfile(ProfileExportRequest request) {
        Objects.requireNonNull(request, "request is required");
        Objects.requireNonNull(request.profile(), "request.profile is required");
        final Map<String, DeviceTypeTemplate> deviceTypes = request.profile().deviceTypes();
        final List<String> filterIds = request.deviceTypeIds();
        final Map<String, byte[]> archives = new LinkedHashMap<>();

        if (filterIds == null || filterIds.isEmpty()) {
            deviceTypes.forEach((typeId, deviceType) -> archives.put(typeId, writeDtt(toArchiveTemplate(typeId, deviceType, request.dttVersion()))));
            return new BatchDttExportResult(archives);
        }

        for (String typeId : filterIds) {
            final DeviceTypeTemplate deviceType = deviceTypes.get(typeId);
            if (deviceType == null) {
                throw new IllegalArgumentException("Unknown deviceTypeId in profile export request: " + typeId);
            }
            archives.put(typeId, writeDtt(toArchiveTemplate(typeId, deviceType, request.dttVersion())));
        }
        return new BatchDttExportResult(archives);
    }

    @Override
    public BatchDttExportResult exportDttSetFromProfileJson(String profileJson, List<String> deviceTypeIds, String dttVersion) {
        return exportDttSetFromProfile(new ProfileExportRequest(parseProfileJson(profileJson), deviceTypeIds, dttVersion));
    }

    @Override
    public EquipmentProfile importDttSetToProfile(List<byte[]> archives, MergeStrategy mergeStrategy) {
        return assembleProfile(toProfileAssemblyRequest(archives, mergeStrategy));
    }

    @Override
    public EquipmentProfile previewDttSetToProfile(List<byte[]> archives, MergeStrategy mergeStrategy) {
        return assemblyService.previewEquipmentProfile(toProfileAssemblyRequest(archives, mergeStrategy));
    }

    @Override
    public BatchDttExportResult exportDttSetFromBranch(BranchEquipment branchEquipment, MergeStrategy mergeStrategy) {
        return exportDttSetFromBranch(new BranchEquipmentExportRequest(branchEquipment, List.of(), List.of(), mergeStrategy, null));
    }

    @Override
    public BatchDttExportResult exportDttSetFromBranch(BranchEquipmentExportRequest request) {
        Objects.requireNonNull(request, "request is required");
        Objects.requireNonNull(request.branchEquipment(), "request.branchEquipment is required");
        final Map<String, byte[]> archives = new LinkedHashMap<>();
        final MergeStrategy effectiveMergeStrategy = request.mergeStrategy() == null ? MergeStrategy.FAIL_IF_EXISTS : request.mergeStrategy();
        final Set<String> requestedBranches = resolveRequestedBranches(request.branchEquipment(), request.branchIds());
        final Set<String> requestedDeviceTypeIds = request.deviceTypeIds() == null ? Set.of() : Set.copyOf(request.deviceTypeIds());
        final Set<String> exportedDeviceTypeIds = new HashSet<>();

        request.branchEquipment().branches().forEach((branchId, branch) -> {
            if (!requestedBranches.contains(branchId)) {
                return;
            }
            branch.deviceTypes().forEach((typeId, branchType) -> {
                if (!requestedDeviceTypeIds.isEmpty() && !requestedDeviceTypeIds.contains(typeId)) {
                    return;
                }
                final String key = resolveKeyForBranchExport(archives, typeId, branchId, effectiveMergeStrategy);
                archives.put(key, writeDtt(toArchiveTemplate(key, branchType.template(), request.dttVersion())));
                exportedDeviceTypeIds.add(typeId);
            });
        });

        if (!requestedDeviceTypeIds.isEmpty() && !exportedDeviceTypeIds.containsAll(requestedDeviceTypeIds)) {
            final Set<String> missing = new HashSet<>(requestedDeviceTypeIds);
            missing.removeAll(exportedDeviceTypeIds);
            throw new IllegalArgumentException("Requested deviceTypeIds were not found in selected branches: " + missing);
        }

        return new BatchDttExportResult(archives);
    }

    @Override
    public BatchDttExportResult exportDttSetFromBranchJson(String branchJson,
                                                           List<String> branchIds,
                                                           List<String> deviceTypeIds,
                                                           MergeStrategy mergeStrategy,
                                                           String dttVersion) {
        return exportDttSetFromBranch(new BranchEquipmentExportRequest(
                parseBranchJson(branchJson),
                branchIds,
                deviceTypeIds,
                mergeStrategy,
                dttVersion
        ));
    }

    @Override
    public BranchEquipment importDttSetToBranch(List<byte[]> archives, List<String> branchIds, MergeStrategy mergeStrategy) {
        return assembleBranch(toBranchAssemblyRequest(archives, branchIds, mergeStrategy));
    }

    @Override
    public BranchEquipment previewDttSetToBranch(List<byte[]> archives, List<String> branchIds, MergeStrategy mergeStrategy) {
        return assemblyService.previewBranchEquipment(toBranchAssemblyRequest(archives, branchIds, mergeStrategy));
    }

    @Override
    public EquipmentProfile importDttBase64SetToProfile(List<String> archivesBase64, MergeStrategy mergeStrategy) {
        return importDttSetToProfile(decodeBase64Archives(archivesBase64), mergeStrategy);
    }

    @Override
    public EquipmentProfile previewDttBase64SetToProfile(List<String> archivesBase64, MergeStrategy mergeStrategy) {
        return previewDttSetToProfile(decodeBase64Archives(archivesBase64), mergeStrategy);
    }

    @Override
    public BranchEquipment importDttBase64SetToBranch(List<String> archivesBase64,
                                                      List<String> branchIds,
                                                      MergeStrategy mergeStrategy) {
        return importDttSetToBranch(decodeBase64Archives(archivesBase64), branchIds, mergeStrategy);
    }

    @Override
    public BranchEquipment previewDttBase64SetToBranch(List<String> archivesBase64,
                                                       List<String> branchIds,
                                                       MergeStrategy mergeStrategy) {
        return previewDttSetToBranch(decodeBase64Archives(archivesBase64), branchIds, mergeStrategy);
    }

    @Override
    public EquipmentProfile importDttZipToProfile(byte[] zipPayload, MergeStrategy mergeStrategy) {
        return importDttSetToProfile(readDttFilesFromZip(zipPayload), mergeStrategy);
    }

    @Override
    public EquipmentProfile previewDttZipToProfile(byte[] zipPayload, MergeStrategy mergeStrategy) {
        return previewDttSetToProfile(readDttFilesFromZip(zipPayload), mergeStrategy);
    }

    @Override
    public BranchEquipment importDttZipToBranch(byte[] zipPayload, List<String> branchIds, MergeStrategy mergeStrategy) {
        return importDttSetToBranch(readDttFilesFromZip(zipPayload), branchIds, mergeStrategy);
    }

    @Override
    public BranchEquipment previewDttZipToBranch(byte[] zipPayload, List<String> branchIds, MergeStrategy mergeStrategy) {
        return previewDttSetToBranch(readDttFilesFromZip(zipPayload), branchIds, mergeStrategy);
    }

    @Override
    public byte[] exportProfileToDttZip(ProfileExportRequest request) {
        return writeDttZip(exportDttSetFromProfile(request).archivesByDeviceTypeId());
    }

    @Override
    public byte[] exportBranchToDttZip(BranchEquipmentExportRequest request) {
        return writeDttZip(exportDttSetFromBranch(request).archivesByDeviceTypeId());
    }

    @Override
    public String exportProfileToDttZipBase64(ProfileExportRequest request) {
        return Base64.getEncoder().encodeToString(exportProfileToDttZip(request));
    }

    @Override
    public String exportBranchToDttZipBase64(BranchEquipmentExportRequest request) {
        return Base64.getEncoder().encodeToString(exportBranchToDttZip(request));
    }

    private DeviceTypeTemplate toDeviceTypeTemplate(DttArchiveTemplate template) {
        return new DeviceTypeTemplate(template.metadata(), template.defaultValues() == null ? Map.of() : template.defaultValues());
    }

    private EquipmentProfileAssemblyRequest toProfileAssemblyRequest(List<byte[]> archives, MergeStrategy mergeStrategy) {
        Objects.requireNonNull(archives, "archives is required");
        if (archives.isEmpty()) {
            throw new IllegalArgumentException("archives must contain at least one DTT archive");
        }
        final List<EquipmentProfileDeviceTypeRequest> requests = archives.stream()
                .map(this::readDtt)
                .map(this::toDeviceTypeTemplate)
                .map(template -> new EquipmentProfileDeviceTypeRequest(template, true))
                .toList();
        return new EquipmentProfileAssemblyRequest(requests, List.of(), mergeStrategy);
    }

    private BranchEquipmentAssemblyRequest toBranchAssemblyRequest(List<byte[]> archives,
                                                                   List<String> branchIds,
                                                                   MergeStrategy mergeStrategy) {
        Objects.requireNonNull(archives, "archives is required");
        if (archives.isEmpty()) {
            throw new IllegalArgumentException("archives must contain at least one DTT archive");
        }
        if (branchIds == null || branchIds.isEmpty()) {
            throw new IllegalArgumentException("branchIds must contain at least one branch id");
        }
        final List<EquipmentProfileDeviceTypeRequest> deviceTypeRequests = archives.stream()
                .map(this::readDtt)
                .map(this::toDeviceTypeTemplate)
                .map(template -> new EquipmentProfileDeviceTypeRequest(template, true))
                .toList();
        final List<BranchImportRequest> branches = branchIds.stream()
                .map(branchId -> new BranchImportRequest(
                        branchId,
                        branchId,
                        deviceTypeRequests.stream()
                                .map(request -> new BranchDeviceTypeImportRequest(request, List.of()))
                                .toList()
                ))
                .toList();
        return new BranchEquipmentAssemblyRequest(branches, mergeStrategy);
    }

    private DttArchiveTemplate toArchiveTemplate(String typeId, DeviceTypeTemplate deviceType, String dttVersion) {
        final String effectiveVersion = normalizeDttVersion(dttVersion);
        final DeviceTypeMetadata metadata = appendVersionToDescription(deviceType.metadata(), effectiveVersion);
        return new DttArchiveTemplate(
                new DttArchiveDescriptor("DTT", "1.0", typeId, effectiveVersion),
                metadata,
                Map.of(),
                Map.of(),
                Map.of(),
                extractDefaultValues(deviceType.deviceTypeParamValues()),
                Map.of(),
                null,
                null,
                null,
                null,
                null,
                Map.of(),
                Map.of()
        );
    }

    private Map<String, Object> extractDefaultValues(Map<String, Object> deviceTypeParamValues) {
        if (deviceTypeParamValues == null || deviceTypeParamValues.isEmpty()) {
            return Map.of();
        }
        final Map<String, Object> extracted = new LinkedHashMap<>();
        deviceTypeParamValues.forEach((key, value) -> extracted.put(key, extractCanonicalValue(value)));
        return extracted;
    }

    private Object extractCanonicalValue(Object value) {
        if (value instanceof Map<?, ?> mapValue) {
            if (mapValue.containsKey("value")) {
                return extractCanonicalValue(mapValue.get("value"));
            }
            final Map<String, Object> nested = new LinkedHashMap<>();
            mapValue.forEach((nestedKey, nestedValue) ->
                    nested.put(String.valueOf(nestedKey), extractCanonicalValue(nestedValue))
            );
            return nested;
        }
        if (value instanceof List<?> listValue) {
            final List<Object> extracted = new java.util.ArrayList<>(listValue.size());
            for (Object entry : listValue) {
                extracted.add(extractCanonicalValue(entry));
            }
            return extracted;
        }
        return value;
    }

    private String normalizeDttVersion(String dttVersion) {
        if (dttVersion == null || dttVersion.isBlank()) {
            return "1.0";
        }
        return dttVersion.trim();
    }

    private DeviceTypeMetadata appendVersionToDescription(DeviceTypeMetadata metadata, String dttVersion) {
        final String description = metadata.description() == null ? "" : metadata.description();
        final String versionSuffix = " " + dttVersion;
        final String normalizedDescription = description.endsWith(versionSuffix)
                ? description
                : description + versionSuffix;
        return new DeviceTypeMetadata(
                metadata.id(),
                metadata.name(),
                metadata.displayName(),
                normalizedDescription
        );
    }

    private String resolveKeyForBranchExport(Map<String, byte[]> result,
                                             String typeId,
                                             String branchId,
                                             MergeStrategy mergeStrategy) {
        if (!result.containsKey(typeId)) {
            return typeId;
        }

        return switch (mergeStrategy) {
            case FAIL_IF_EXISTS -> throw new TemplateAssemblyException(
                    "Device type '" + typeId + "' already exported from another branch (branchId='" + branchId + "')"
            );
            case REPLACE, MERGE_NON_NULLS, MERGE_PRESERVE_EXISTING -> typeId;
            case CREATE_COPY_WITH_SUFFIX -> nextCopyKey(result, typeId);
        };
    }

    private String nextCopyKey(Map<String, byte[]> result, String baseKey) {
        int counter = 1;
        String candidate = baseKey + "_copy" + counter;
        while (result.containsKey(candidate)) {
            counter++;
            candidate = baseKey + "_copy" + counter;
        }
        return candidate;
    }

    private Set<String> resolveRequestedBranches(BranchEquipment branchEquipment, List<String> branchIds) {
        if (branchIds == null || branchIds.isEmpty()) {
            return branchEquipment.branches().keySet();
        }
        final Set<String> requested = Set.copyOf(branchIds);
        final Set<String> existing = branchEquipment.branches().keySet();
        if (!existing.containsAll(requested)) {
            final Set<String> missing = new HashSet<>(requested);
            missing.removeAll(existing);
            throw new IllegalArgumentException("Unknown branchIds in export request: " + missing);
        }
        return requested;
    }

    private List<byte[]> decodeBase64Archives(List<String> archivesBase64) {
        Objects.requireNonNull(archivesBase64, "archivesBase64 is required");
        if (archivesBase64.isEmpty()) {
            throw new IllegalArgumentException("archivesBase64 must contain at least one archive");
        }
        return archivesBase64.stream().map(value -> {
            try {
                return Base64.getDecoder().decode(value);
            } catch (IllegalArgumentException exception) {
                throw new IllegalArgumentException("Invalid Base64 DTT archive payload", exception);
            }
        }).toList();
    }

    private List<byte[]> readDttFilesFromZip(byte[] zipPayload) {
        Objects.requireNonNull(zipPayload, "zipPayload is required");
        try (ZipInputStream input = new ZipInputStream(new ByteArrayInputStream(zipPayload))) {
            final List<byte[]> archives = new java.util.ArrayList<>();
            ZipEntry entry;
            while ((entry = input.getNextEntry()) != null) {
                if (entry.isDirectory() || !entry.getName().endsWith(".dtt")) {
                    continue;
                }
                archives.add(input.readAllBytes());
            }
            if (archives.isEmpty()) {
                throw new IllegalArgumentException("Zip payload must contain at least one .dtt file");
            }
            return archives;
        } catch (IOException exception) {
            throw new IllegalArgumentException("Invalid zip payload", exception);
        }
    }

    private byte[] writeDttZip(Map<String, byte[]> archivesByDeviceTypeId) {
        try (ByteArrayOutputStream output = new ByteArrayOutputStream();
             ZipOutputStream zip = new ZipOutputStream(output)) {
            for (Map.Entry<String, byte[]> entry : archivesByDeviceTypeId.entrySet()) {
                zip.putNextEntry(new ZipEntry(entry.getKey() + ".dtt"));
                zip.write(entry.getValue());
                zip.closeEntry();
            }
            zip.finish();
            return output.toByteArray();
        } catch (IOException exception) {
            throw new IllegalStateException("Failed to build zip payload with DTT archives", exception);
        }
    }
}
