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
import ru.aritmos.dtt.api.dto.branch.DeviceInstanceImportRequest;
import ru.aritmos.dtt.archive.DefaultDttArchiveReader;
import ru.aritmos.dtt.archive.DefaultDttArchiveWriter;
import ru.aritmos.dtt.archive.DttArchiveReader;
import ru.aritmos.dtt.archive.DttFileNames;
import ru.aritmos.dtt.archive.DttArchiveWriter;
import ru.aritmos.dtt.archive.model.DttArchiveDescriptor;
import ru.aritmos.dtt.archive.model.DttArchiveTemplate;
import ru.aritmos.dtt.assembly.DefaultTemplateAssemblyService;
import ru.aritmos.dtt.exception.TemplateAssemblyException;
import ru.aritmos.dtt.json.branch.BranchEquipment;
import ru.aritmos.dtt.json.branch.BranchScript;
import ru.aritmos.dtt.json.branch.BranchDeviceType;
import ru.aritmos.dtt.json.branch.DeviceInstanceTemplate;
import ru.aritmos.dtt.json.branch.DefaultDeviceManagerBranchJsonGenerator;
import ru.aritmos.dtt.json.branch.DefaultDeviceManagerBranchJsonParser;
import ru.aritmos.dtt.json.branch.DeviceManagerBranchJsonGenerator;
import ru.aritmos.dtt.json.branch.DeviceManagerBranchJsonParser;
import ru.aritmos.dtt.json.profile.DefaultEquipmentProfileJsonGenerator;
import ru.aritmos.dtt.json.profile.DefaultEquipmentProfileJsonParser;
import ru.aritmos.dtt.json.profile.EquipmentProfile;
import ru.aritmos.dtt.json.profile.EquipmentProfileJsonGenerator;
import ru.aritmos.dtt.json.profile.EquipmentProfileJsonParser;
import ru.aritmos.dtt.model.canonical.CanonicalBranchProjection;
import ru.aritmos.dtt.model.canonical.CanonicalProfileProjection;
import ru.aritmos.dtt.model.mapping.CanonicalProjectionMapper;
import ru.aritmos.dtt.model.mapping.CanonicalTemplateMapper;
import ru.aritmos.dtt.model.mapping.DefaultCanonicalProjectionMapper;
import ru.aritmos.dtt.model.mapping.DefaultCanonicalTemplateMapper;
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
    private final CanonicalTemplateMapper canonicalTemplateMapper = new DefaultCanonicalTemplateMapper();
    private final CanonicalProjectionMapper canonicalProjectionMapper = new DefaultCanonicalProjectionMapper();

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
    public Map<String, String> exportDttSetFromProfileBase64(ProfileExportRequest request) {
        return encodeArchives(exportDttSetFromProfile(request).archivesByDeviceTypeId());
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
        final MergeStrategy requestedMergeStrategy = request.mergeStrategy();
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
                final String key = resolveKeyForBranchExport(archives, typeId, branchId, requestedMergeStrategy);
                final DttArchiveTemplate incomingTemplate = toArchiveTemplate(key, branchType, branchId, request.dttVersion());
                if (archives.containsKey(key)) {
                    final DttArchiveTemplate existingTemplate = readDtt(archives.get(key));
                    final DttArchiveTemplate resolvedTemplate;
                    if (requestedMergeStrategy == null) {
                        resolvedTemplate = selectMostCompleteTemplateForBranchExport(existingTemplate, incomingTemplate);
                    } else if (requestedMergeStrategy == MergeStrategy.MERGE_NON_NULLS
                            || requestedMergeStrategy == MergeStrategy.MERGE_PRESERVE_EXISTING) {
                        resolvedTemplate = mergeTemplatesForBranchExport(existingTemplate, incomingTemplate, requestedMergeStrategy);
                    } else {
                        resolvedTemplate = incomingTemplate;
                    }
                    archives.put(key, writeDtt(resolvedTemplate));
                } else {
                    archives.put(key, writeDtt(incomingTemplate));
                }
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
    public Map<String, String> exportDttSetFromBranchBase64(BranchEquipmentExportRequest request) {
        return encodeArchives(exportDttSetFromBranch(request).archivesByDeviceTypeId());
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
        return assembleBranch(toBranchAssemblyRequest(archives, branchIds, mergeStrategy, false));
    }

    @Override
    public BranchEquipment importDttSetToExistingBranch(List<byte[]> archives,
                                                        BranchEquipment existingBranchEquipment,
                                                        List<String> branchIds,
                                                        MergeStrategy mergeStrategy) {
        Objects.requireNonNull(existingBranchEquipment, "existingBranchEquipment is required");
        final BranchEquipment imported = assembleBranch(toBranchAssemblyRequest(archives, branchIds, mergeStrategy, true));
        return assemblyService.mergeBranchEquipment(existingBranchEquipment, imported, mergeStrategy);
    }

    @Override
    public BranchEquipment previewDttSetToBranch(List<byte[]> archives, List<String> branchIds, MergeStrategy mergeStrategy) {
        return assemblyService.previewBranchEquipment(toBranchAssemblyRequest(archives, branchIds, mergeStrategy, false));
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
    public BranchEquipment importDttBase64SetToExistingBranch(List<String> archivesBase64,
                                                              BranchEquipment existingBranchEquipment,
                                                              List<String> branchIds,
                                                              MergeStrategy mergeStrategy) {
        return importDttSetToExistingBranch(
                decodeBase64Archives(archivesBase64),
                existingBranchEquipment,
                branchIds,
                mergeStrategy
        );
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
    public BranchEquipment importDttZipToExistingBranch(byte[] zipPayload,
                                                        BranchEquipment existingBranchEquipment,
                                                        List<String> branchIds,
                                                        MergeStrategy mergeStrategy) {
        return importDttSetToExistingBranch(readDttFilesFromZip(zipPayload), existingBranchEquipment, branchIds, mergeStrategy);
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
        final CanonicalProfileProjection profileProjection =
                canonicalProjectionMapper.toProfileProjection(canonicalTemplateMapper.toCanonical(template));
        return new DeviceTypeTemplate(template.metadata(), profileProjection.deviceTypeParamValues());
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
                                                                   MergeStrategy mergeStrategy,
                                                                   boolean strictBranchTopology) {
        Objects.requireNonNull(archives, "archives is required");
        if (archives.isEmpty()) {
            throw new IllegalArgumentException("archives must contain at least one DTT archive");
        }
        if (branchIds == null || branchIds.isEmpty()) {
            throw new IllegalArgumentException("branchIds must contain at least one branch id");
        }
        final List<BranchImportRequest> branches = branchIds.stream()
                .map(branchId -> new BranchImportRequest(
                        branchId,
                        branchId,
                        archives.stream()
                                .map(this::readDtt)
                                .map(template -> toBranchDeviceTypeImportRequest(template, branchId, strictBranchTopology))
                                .filter(Objects::nonNull)
                                .toList()
                ))
                .toList();
        return new BranchEquipmentAssemblyRequest(branches, mergeStrategy);
    }

    private DttArchiveTemplate toArchiveTemplate(String typeId, DeviceTypeTemplate deviceType, String dttVersion) {
        final String effectiveVersion = resolveEffectiveVersion(deviceType.metadata(), dttVersion);
        final DeviceTypeMetadata metadata = appendVersionToDescription(deviceType.metadata(), effectiveVersion);
        final Map<String, Object> deviceTypeSchema = buildDeviceTypeParameterSchema(deviceType.deviceTypeParamValues());
        final Map<String, Object> exampleValues = extractExampleValues(deviceType.deviceTypeParamValues());
        return new DttArchiveTemplate(
                new DttArchiveDescriptor("DTT", "1.0", typeId, effectiveVersion),
                metadata,
                deviceTypeSchema,
                Map.of(),
                Map.of(),
                extractDefaultValues(deviceType.deviceTypeParamValues()),
                exampleValues,
                buildTemplateOrigin("PROFILE_JSON", "export profile->dtt"),
                null,
                null,
                null,
                null,
                null,
                Map.of(),
                Map.of()
        );
    }

    private DttArchiveTemplate toArchiveTemplate(String typeId,
                                                 BranchDeviceType branchDeviceType,
                                                 String branchId,
                                                 String dttVersion) {
        final DeviceTypeTemplate deviceType = branchDeviceType.template();
        final String effectiveVersion = resolveEffectiveVersion(deviceType.metadata(), dttVersion);
        final DeviceTypeMetadata metadata = appendVersionToDescription(deviceType.metadata(), effectiveVersion);
        final Map<String, Object> deviceTypeSchema = buildDeviceTypeParameterSchema(deviceType.deviceTypeParamValues());
        final Map<String, Object> exampleValues = extractExampleValues(deviceType.deviceTypeParamValues());
        final Map<String, Object> templateOrigin = buildTemplateOrigin("BRANCH_EQUIPMENT_JSON", "export branch->dtt branchId=" + branchId);
        templateOrigin.put("branchTopologyByBranchId", Map.of(branchId, toBranchSnapshot(branchDeviceType)));
        return new DttArchiveTemplate(
                new DttArchiveDescriptor("DTT", "1.0", typeId, effectiveVersion),
                metadata,
                deviceTypeSchema,
                Map.of(),
                buildScriptBindingHints(branchDeviceType),
                extractDefaultValues(deviceType.deviceTypeParamValues()),
                exampleValues,
                templateOrigin,
                extractScriptCode(branchDeviceType.onStartEvent()),
                extractScriptCode(branchDeviceType.onStopEvent()),
                extractScriptCode(branchDeviceType.onPublicStartEvent()),
                extractScriptCode(branchDeviceType.onPublicFinishEvent()),
                branchDeviceType.deviceTypeFunctions(),
                extractScriptMap(branchDeviceType.eventHandlers()),
                extractScriptMap(branchDeviceType.commands())
        );
    }

    private Map<String, Object> buildScriptBindingHints(BranchDeviceType branchDeviceType) {
        final Map<String, Object> hints = new LinkedHashMap<>();
        if (branchDeviceType.kind() != null && !branchDeviceType.kind().isBlank()) {
            hints.put("deviceTypeKind", branchDeviceType.kind());
        }
        putLifecycleHint(hints, "onStartEvent", branchDeviceType.onStartEvent());
        putLifecycleHint(hints, "onStopEvent", branchDeviceType.onStopEvent());
        putLifecycleHint(hints, "onPublicStartEvent", branchDeviceType.onPublicStartEvent());
        putLifecycleHint(hints, "onPublicFinishEvent", branchDeviceType.onPublicFinishEvent());

        if (branchDeviceType.eventHandlers() != null && !branchDeviceType.eventHandlers().isEmpty()) {
            hints.put("eventHandlers", extractScriptMetadataMap(branchDeviceType.eventHandlers()));
        }
        if (branchDeviceType.commands() != null && !branchDeviceType.commands().isEmpty()) {
            hints.put("commands", extractScriptMetadataMap(branchDeviceType.commands()));
        }
        return hints;
    }

    private void putLifecycleHint(Map<String, Object> hints, String key, BranchScript script) {
        if (script == null) {
            return;
        }
        hints.put(key, scriptMetadata(script));
    }

    private Map<String, Object> extractScriptMetadataMap(Map<String, BranchScript> scripts) {
        final Map<String, Object> metadata = new LinkedHashMap<>();
        scripts.forEach((name, script) -> metadata.put(name, scriptMetadata(script)));
        return metadata;
    }

    private Map<String, Object> scriptMetadata(BranchScript script) {
        final Map<String, Object> metadata = new LinkedHashMap<>();
        metadata.put("inputParameters", script.inputParameters() == null ? Map.of() : script.inputParameters());
        metadata.put("outputParameters", script.outputParameters() == null ? List.of() : script.outputParameters());
        return metadata;
    }

    private Map<String, Object> extractDefaultValues(Map<String, Object> deviceTypeParamValues) {
        if (deviceTypeParamValues == null || deviceTypeParamValues.isEmpty()) {
            return Map.of();
        }
        final Map<String, Object> extracted = new LinkedHashMap<>();
        deviceTypeParamValues.forEach((key, value) -> extracted.put(key, extractCanonicalValue(value)));
        return extracted;
    }

    private Map<String, Object> extractExampleValues(Map<String, Object> deviceTypeParamValues) {
        if (deviceTypeParamValues == null || deviceTypeParamValues.isEmpty()) {
            return Map.of();
        }
        final Map<String, Object> examples = new LinkedHashMap<>();
        deviceTypeParamValues.forEach((key, value) -> {
            final Object extracted = extractExampleValue(value);
            if (extracted != null) {
                examples.put(key, extracted);
            }
        });
        return examples;
    }

    private Object extractExampleValue(Object value) {
        if (value instanceof Map<?, ?> mapValue) {
            if (mapValue.containsKey("exampleValue")) {
                return mapValue.get("exampleValue");
            }
            if (mapValue.containsKey("value")) {
                final Object nestedValue = extractExampleValue(mapValue.get("value"));
                if (nestedValue != null) {
                    return nestedValue;
                }
                if (looksLikeParameterDescriptor(mapValue)) {
                    return mapValue.get("value");
                }
                return null;
            }
            final Map<String, Object> nested = new LinkedHashMap<>();
            mapValue.forEach((nestedKey, nestedValue) -> {
                final Object extractedNested = extractExampleValue(nestedValue);
                if (extractedNested != null) {
                    nested.put(String.valueOf(nestedKey), extractedNested);
                }
            });
            return nested.isEmpty() ? null : nested;
        }
        if (value instanceof List<?> listValue) {
            final List<Object> extracted = new java.util.ArrayList<>();
            for (Object entry : listValue) {
                final Object nested = extractExampleValue(entry);
                if (nested != null) {
                    extracted.add(nested);
                }
            }
            return extracted.isEmpty() ? null : extracted;
        }
        return null;
    }

    private boolean looksLikeParameterDescriptor(Map<?, ?> mapValue) {
        return mapValue.containsKey("name")
                || mapValue.containsKey("type")
                || mapValue.containsKey("displayName")
                || mapValue.containsKey("description");
    }

    private BranchDeviceTypeImportRequest toBranchDeviceTypeImportRequest(DttArchiveTemplate template,
                                                                          String branchId,
                                                                          boolean strictBranchTopology) {
        final BranchDeviceTypeImportRequest branchSpecificImport = toBranchSpecificImportRequest(template, branchId);
        if (branchSpecificImport != null) {
            return branchSpecificImport;
        }
        if (strictBranchTopology && hasBranchTopology(template)) {
            return null;
        }
        final Map<String, Object> hints = template.bindingHints() == null ? Map.of() : template.bindingHints();
        final String kind = hints.get("deviceTypeKind") instanceof String kindHint && !kindHint.isBlank()
                ? kindHint
                : template.metadata().name();
        final CanonicalBranchProjection branchProjection =
                canonicalProjectionMapper.toBranchProjection(canonicalTemplateMapper.toCanonical(template), kind);
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

    private boolean hasBranchTopology(DttArchiveTemplate template) {
        final Map<String, Object> origin = template.templateOrigin() == null ? Map.of() : template.templateOrigin();
        return origin.get("branchTopologyByBranchId") instanceof Map<?, ?>;
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
                ? new java.util.ArrayList<>(list)
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

    private Map<String, Object> castToStringObjectMap(Map<?, ?> source) {
        final Map<String, Object> result = new LinkedHashMap<>();
        source.forEach((key, value) -> result.put(String.valueOf(key), value));
        return result;
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

    private String extractScriptCode(BranchScript scriptSection) {
        if (scriptSection == null) {
            return null;
        }
        return scriptSection.scriptCode();
    }

    private Map<String, String> extractScriptMap(Map<String, BranchScript> sections) {
        if (sections == null || sections.isEmpty()) {
            return Map.of();
        }
        final Map<String, String> scripts = new LinkedHashMap<>();
        sections.forEach((name, value) -> {
            final String script = extractScriptCode(value);
            if (script != null) {
                scripts.put(name, script);
            }
        });
        return scripts;
    }

    private String normalizeDttVersion(String dttVersion) {
        return DttVersionSupport.normalize(dttVersion);
    }

    private String resolveEffectiveVersion(DeviceTypeMetadata metadata, String requestedVersion) {
        final String requested = normalizeDttVersion(requestedVersion);
        final String fromTemplate = metadata == null ? null : metadata.version();
        return DttVersionSupport.max(requested, fromTemplate);
    }

    private Map<String, Object> buildTemplateOrigin(String sourceKind, String sourceSummary) {
        final Map<String, Object> origin = new LinkedHashMap<>();
        origin.put("sourceKind", sourceKind);
        origin.put("sourceSummary", sourceSummary);
        return origin;
    }

    private DttArchiveTemplate mergeTemplatesForBranchExport(DttArchiveTemplate existing,
                                                             DttArchiveTemplate incoming,
                                                             MergeStrategy mergeStrategy) {
        final boolean incomingWins = mergeStrategy == MergeStrategy.MERGE_NON_NULLS;
        final Map<String, Object> mergedOrigin = mergeTemplateOrigin(existing.templateOrigin(), incoming.templateOrigin());
        return new DttArchiveTemplate(
                existing.descriptor(),
                incomingWins ? incoming.metadata() : existing.metadata(),
                mergeMap(existing.deviceTypeParametersSchema(), incoming.deviceTypeParametersSchema(), incomingWins),
                mergeMap(existing.deviceParametersSchema(), incoming.deviceParametersSchema(), incomingWins),
                mergeMap(existing.bindingHints(), incoming.bindingHints(), incomingWins),
                mergeMap(existing.defaultValues(), incoming.defaultValues(), incomingWins),
                mergeMap(existing.exampleValues(), incoming.exampleValues(), incomingWins),
                mergedOrigin,
                incomingWins ? firstNonNull(incoming.onStartEvent(), existing.onStartEvent()) : firstNonNull(existing.onStartEvent(), incoming.onStartEvent()),
                incomingWins ? firstNonNull(incoming.onStopEvent(), existing.onStopEvent()) : firstNonNull(existing.onStopEvent(), incoming.onStopEvent()),
                incomingWins ? firstNonNull(incoming.onPublicStartEvent(), existing.onPublicStartEvent()) : firstNonNull(existing.onPublicStartEvent(), incoming.onPublicStartEvent()),
                incomingWins ? firstNonNull(incoming.onPublicFinishEvent(), existing.onPublicFinishEvent()) : firstNonNull(existing.onPublicFinishEvent(), incoming.onPublicFinishEvent()),
                incomingWins ? firstNonNull(incoming.deviceTypeFunctions(), existing.deviceTypeFunctions()) : firstNonNull(existing.deviceTypeFunctions(), incoming.deviceTypeFunctions()),
                mergeStringMap(existing.eventHandlers(), incoming.eventHandlers(), incomingWins),
                mergeStringMap(existing.commands(), incoming.commands(), incomingWins)
        );
    }

    private Map<String, Object> mergeTemplateOrigin(Map<String, Object> existing, Map<String, Object> incoming) {
        final Map<String, Object> merged = new LinkedHashMap<>();
        if (existing != null) {
            merged.putAll(existing);
        }
        if (incoming != null) {
            incoming.forEach((key, value) -> {
                if (!"branchTopologyByBranchId".equals(key)) {
                    merged.put(key, value);
                }
            });
        }
        final Map<String, Object> topology = new LinkedHashMap<>();
        if (existing != null && existing.get("branchTopologyByBranchId") instanceof Map<?, ?> existingTopologyRaw) {
            topology.putAll(castToStringObjectMap(existingTopologyRaw));
        }
        if (incoming != null && incoming.get("branchTopologyByBranchId") instanceof Map<?, ?> incomingTopologyRaw) {
            topology.putAll(castToStringObjectMap(incomingTopologyRaw));
        }
        if (!topology.isEmpty()) {
            merged.put("branchTopologyByBranchId", topology);
        }
        return merged;
    }

    private Map<String, Object> toBranchSnapshot(BranchDeviceType branchDeviceType) {
        final Map<String, Object> snapshot = new LinkedHashMap<>();
        snapshot.put("metadata", metadataToMap(branchDeviceType.template().metadata()));
        snapshot.put("deviceTypeParamValues", branchDeviceType.template().deviceTypeParamValues());
        snapshot.put("devices", toDeviceSnapshotMap(branchDeviceType.devices()));
        snapshot.put("kind", branchDeviceType.kind());
        snapshot.put("onStartEvent", toScriptSnapshot(branchDeviceType.onStartEvent()));
        snapshot.put("onStopEvent", toScriptSnapshot(branchDeviceType.onStopEvent()));
        snapshot.put("onPublicStartEvent", toScriptSnapshot(branchDeviceType.onPublicStartEvent()));
        snapshot.put("onPublicFinishEvent", toScriptSnapshot(branchDeviceType.onPublicFinishEvent()));
        snapshot.put("deviceTypeFunctions", branchDeviceType.deviceTypeFunctions());
        snapshot.put("eventHandlers", toScriptSnapshotMap(branchDeviceType.eventHandlers()));
        snapshot.put("commands", toScriptSnapshotMap(branchDeviceType.commands()));
        return snapshot;
    }

    private Map<String, Object> metadataToMap(DeviceTypeMetadata metadata) {
        final Map<String, Object> map = new LinkedHashMap<>();
        map.put("id", metadata.id());
        map.put("name", metadata.name());
        map.put("displayName", metadata.displayName());
        map.put("description", metadata.description());
        return map;
    }

    private Map<String, Object> toDeviceSnapshotMap(Map<String, DeviceInstanceTemplate> devices) {
        if (devices == null || devices.isEmpty()) {
            return Map.of();
        }
        final Map<String, Object> result = new LinkedHashMap<>();
        devices.forEach((deviceId, device) -> {
            final Map<String, Object> deviceMap = new LinkedHashMap<>();
            deviceMap.put("id", device.id());
            deviceMap.put("name", device.name());
            deviceMap.put("displayName", device.displayName());
            deviceMap.put("description", device.description());
            deviceMap.put("deviceParamValues", device.deviceParamValues());
            result.put(deviceId, deviceMap);
        });
        return result;
    }

    private Object toScriptSnapshot(BranchScript script) {
        if (script == null) {
            return null;
        }
        final Map<String, Object> map = new LinkedHashMap<>();
        map.put("scriptCode", script.scriptCode());
        map.put("inputParameters", script.inputParameters() == null ? Map.of() : script.inputParameters());
        map.put("outputParameters", script.outputParameters() == null ? List.of() : script.outputParameters());
        return map;
    }

    private Map<String, Object> toScriptSnapshotMap(Map<String, BranchScript> scripts) {
        if (scripts == null || scripts.isEmpty()) {
            return Map.of();
        }
        final Map<String, Object> result = new LinkedHashMap<>();
        scripts.forEach((name, script) -> result.put(name, toScriptSnapshot(script)));
        return result;
    }

    private DeviceTypeMetadata toMetadataFromSnapshot(DeviceTypeMetadata fallback, Object metadataRaw) {
        if (!(metadataRaw instanceof Map<?, ?> metadataMapRaw)) {
            return fallback;
        }
        final Map<String, Object> metadata = castToStringObjectMap(metadataMapRaw);
        return new DeviceTypeMetadata(
                firstNonNull(toNullableString(metadata.get("id")), fallback.id()),
                firstNonNull(toNullableString(metadata.get("name")), fallback.name()),
                firstNonNull(toNullableString(metadata.get("displayName")), fallback.displayName()),
                firstNonNull(toNullableString(metadata.get("description")), fallback.description()),
                firstNonNull(toNullableString(metadata.get("version")), fallback.version()),
                firstNonNull(toNullableString(metadata.get("iconBase64")), fallback.iconBase64())
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
        final List<DeviceInstanceImportRequest> result = new java.util.ArrayList<>();
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
                ? new java.util.ArrayList<>(outputList)
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

    private String toNullableString(Object value) {
        return value == null ? null : String.valueOf(value);
    }

    private <T> T firstNonNull(T preferred, T fallback) {
        return preferred != null ? preferred : fallback;
    }

    private Map<String, Object> mergeMap(Map<String, Object> existing, Map<String, Object> incoming, boolean incomingWins) {
        final Map<String, Object> merged = new LinkedHashMap<>();
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

    private Map<String, String> mergeStringMap(Map<String, String> existing, Map<String, String> incoming, boolean incomingWins) {
        final Map<String, String> merged = new LinkedHashMap<>();
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

    private Map<String, Object> buildDeviceTypeParameterSchema(Map<String, Object> values) {
        if (values == null || values.isEmpty()) {
            return Map.of();
        }
        final Map<String, Object> schema = new LinkedHashMap<>();
        values.forEach((key, value) -> schema.put(key, buildParameterDefinition(key, value)));
        return schema;
    }

    private Map<String, Object> buildParameterDefinition(String key, Object value) {
        final Map<String, Object> definition = new LinkedHashMap<>();
        definition.put("name", key);
        if (value instanceof Map<?, ?> mapValue) {
            mapValue.forEach((entryKey, entryValue) -> {
                final String stringKey = String.valueOf(entryKey);
                if (!"value".equals(stringKey)) {
                    definition.put(stringKey, entryValue);
                }
            });
            final Object rawValue = mapValue.get("value");
            if (!definition.containsKey("type")) {
                definition.put("type", inferType(rawValue));
            }
            if (rawValue instanceof Map<?, ?> nestedMap) {
                final Map<String, Object> nestedSchema = new LinkedHashMap<>();
                nestedMap.forEach((nestedKey, nestedValue) ->
                        nestedSchema.put(String.valueOf(nestedKey), buildParameterDefinition(String.valueOf(nestedKey), nestedValue))
                );
                definition.put("parametersMap", nestedSchema);
            }
            if (rawValue instanceof List<?> listValue && !listValue.isEmpty() && !definition.containsKey("items")) {
                definition.put("items", buildArrayItemDefinition(listValue));
            }
            return definition;
        }

        definition.put("type", inferType(value));
        return definition;
    }

    private Map<String, Object> buildArrayItemDefinition(List<?> listValue) {
        final Object firstNotNull = listValue.stream()
                .filter(java.util.Objects::nonNull)
                .findFirst()
                .orElse(null);
        if (firstNotNull == null) {
            return Map.of("type", "nullable");
        }
        final Map<String, Object> itemDefinition = buildParameterDefinition("item", firstNotNull);
        itemDefinition.remove("name");
        return itemDefinition;
    }

    private String inferType(Object value) {
        if (value == null) {
            return "nullable";
        }
        if (value instanceof Boolean) {
            return "Boolean";
        }
        if (value instanceof Number) {
            return "Number";
        }
        if (value instanceof List<?>) {
            return "Array";
        }
        if (value instanceof Map<?, ?>) {
            return "Object";
        }
        return "String";
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
                normalizedDescription,
                dttVersion,
                metadata.iconBase64()
        );
    }

    private String resolveKeyForBranchExport(Map<String, byte[]> result,
                                             String typeId,
                                             String branchId,
                                             MergeStrategy mergeStrategy) {
        if (!result.containsKey(typeId)) {
            return typeId;
        }
        if (mergeStrategy == null) {
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


    private DttArchiveTemplate selectMostCompleteTemplateForBranchExport(DttArchiveTemplate existing,
                                                                         DttArchiveTemplate incoming) {
        final int existingScore = templateCompletenessScore(existing);
        final int incomingScore = templateCompletenessScore(incoming);
        final DttArchiveTemplate preferred = incomingScore > existingScore ? incoming : existing;
        final Map<String, Object> mergedOrigin = mergeTemplateOrigin(existing.templateOrigin(), incoming.templateOrigin());
        return new DttArchiveTemplate(
                preferred.descriptor(),
                preferred.metadata(),
                preferred.deviceTypeParametersSchema(),
                preferred.deviceParametersSchema(),
                preferred.bindingHints(),
                preferred.defaultValues(),
                preferred.exampleValues(),
                mergedOrigin,
                preferred.onStartEvent(),
                preferred.onStopEvent(),
                preferred.onPublicStartEvent(),
                preferred.onPublicFinishEvent(),
                preferred.deviceTypeFunctions(),
                preferred.eventHandlers(),
                preferred.commands()
        );
    }

    private int templateCompletenessScore(DttArchiveTemplate template) {
        if (template == null) {
            return 0;
        }
        int score = 0;
        score += valueCompletenessScore(template.metadata() == null ? null : template.metadata().id());
        score += valueCompletenessScore(template.metadata() == null ? null : template.metadata().name());
        score += valueCompletenessScore(template.metadata() == null ? null : template.metadata().displayName());
        score += valueCompletenessScore(template.metadata() == null ? null : template.metadata().description());
        score += valueCompletenessScore(template.deviceTypeParametersSchema());
        score += valueCompletenessScore(template.deviceParametersSchema());
        score += valueCompletenessScore(template.bindingHints());
        score += valueCompletenessScore(template.defaultValues());
        score += valueCompletenessScore(template.exampleValues());
        score += valueCompletenessScore(template.templateOrigin());
        score += valueCompletenessScore(template.onStartEvent());
        score += valueCompletenessScore(template.onStopEvent());
        score += valueCompletenessScore(template.onPublicStartEvent());
        score += valueCompletenessScore(template.onPublicFinishEvent());
        score += valueCompletenessScore(template.deviceTypeFunctions());
        score += valueCompletenessScore(template.eventHandlers());
        score += valueCompletenessScore(template.commands());
        return score;
    }

    private int valueCompletenessScore(Object value) {
        if (value == null) {
            return 0;
        }
        if (value instanceof CharSequence sequence) {
            return sequence.toString().isBlank() ? 0 : 1;
        }
        if (value instanceof Number || value instanceof Boolean) {
            return 1;
        }
        if (value instanceof Map<?, ?> map) {
            int score = 0;
            for (Object entryValue : map.values()) {
                score += valueCompletenessScore(entryValue);
            }
            return score;
        }
        if (value instanceof Iterable<?> iterable) {
            int score = 0;
            for (Object item : iterable) {
                score += valueCompletenessScore(item);
            }
            return score;
        }
        return 1;
    }


    private <T> String nextCopyKey(Map<String, T> result, String baseKey) {
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
        final List<byte[]> decoded = new java.util.ArrayList<>(archivesBase64.size());
        for (int index = 0; index < archivesBase64.size(); index++) {
            try {
                decoded.add(Base64.getDecoder().decode(archivesBase64.get(index)));
            } catch (IllegalArgumentException exception) {
                throw new IllegalArgumentException("Invalid Base64 DTT archive payload at index " + index, exception);
            }
        }
        return decoded;
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
            final Set<String> usedEntryNames = new HashSet<>();
            for (Map.Entry<String, byte[]> entry : archivesByDeviceTypeId.entrySet()) {
                final DttArchiveTemplate template = readDtt(entry.getValue());
                final String preferredBaseName = DttFileNames.resolveBaseName(template.metadata(), entry.getKey());
                final String uniqueBaseName = ensureUniqueArchiveEntryBaseName(preferredBaseName, usedEntryNames);
                zip.putNextEntry(new ZipEntry(uniqueBaseName + ".dtt"));
                zip.write(entry.getValue());
                zip.closeEntry();
            }
            zip.finish();
            return output.toByteArray();
        } catch (IOException exception) {
            throw new IllegalStateException("Failed to build zip payload with DTT archives", exception);
        }
    }

    private String ensureUniqueArchiveEntryBaseName(String preferredBaseName, Set<String> usedEntryNames) {
        String candidate = preferredBaseName;
        int index = 2;
        while (!usedEntryNames.add(candidate)) {
            candidate = preferredBaseName + " (" + index + ")";
            index++;
        }
        return candidate;
    }

    private Map<String, String> encodeArchives(Map<String, byte[]> archivesByDeviceTypeId) {
        final Map<String, String> encoded = new LinkedHashMap<>();
        archivesByDeviceTypeId.forEach((deviceTypeId, archive) ->
                encoded.put(deviceTypeId, Base64.getEncoder().encodeToString(archive))
        );
        return encoded;
    }

}
