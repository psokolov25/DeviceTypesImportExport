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
import ru.aritmos.dtt.json.branch.BranchScript;
import ru.aritmos.dtt.json.branch.BranchDeviceType;
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
                archives.put(key, writeDtt(toArchiveTemplate(key, branchType, branchId, request.dttVersion())));
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
        return assembleBranch(toBranchAssemblyRequest(archives, branchIds, mergeStrategy));
    }

    @Override
    public BranchEquipment importDttSetToExistingBranch(List<byte[]> archives,
                                                        BranchEquipment existingBranchEquipment,
                                                        List<String> branchIds,
                                                        MergeStrategy mergeStrategy) {
        Objects.requireNonNull(existingBranchEquipment, "existingBranchEquipment is required");
        final BranchEquipment imported = importDttSetToBranch(archives, branchIds, mergeStrategy);
        return assemblyService.mergeBranchEquipment(existingBranchEquipment, imported, mergeStrategy);
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
                                                                   MergeStrategy mergeStrategy) {
        Objects.requireNonNull(archives, "archives is required");
        if (archives.isEmpty()) {
            throw new IllegalArgumentException("archives must contain at least one DTT archive");
        }
        if (branchIds == null || branchIds.isEmpty()) {
            throw new IllegalArgumentException("branchIds must contain at least one branch id");
        }
        final List<BranchDeviceTypeImportRequest> deviceTypeRequests = archives.stream()
                .map(this::readDtt)
                .map(this::toBranchDeviceTypeImportRequest)
                .toList();
        final List<BranchImportRequest> branches = branchIds.stream()
                .map(branchId -> new BranchImportRequest(
                        branchId,
                        branchId,
                        deviceTypeRequests
                ))
                .toList();
        return new BranchEquipmentAssemblyRequest(branches, mergeStrategy);
    }

    private DttArchiveTemplate toArchiveTemplate(String typeId, DeviceTypeTemplate deviceType, String dttVersion) {
        final String effectiveVersion = normalizeDttVersion(dttVersion);
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
        final String effectiveVersion = normalizeDttVersion(dttVersion);
        final DeviceTypeMetadata metadata = appendVersionToDescription(deviceType.metadata(), effectiveVersion);
        final Map<String, Object> deviceTypeSchema = buildDeviceTypeParameterSchema(deviceType.deviceTypeParamValues());
        final Map<String, Object> exampleValues = extractExampleValues(deviceType.deviceTypeParamValues());
        return new DttArchiveTemplate(
                new DttArchiveDescriptor("DTT", "1.0", typeId, effectiveVersion),
                metadata,
                deviceTypeSchema,
                Map.of(),
                buildScriptBindingHints(branchDeviceType),
                extractDefaultValues(deviceType.deviceTypeParamValues()),
                exampleValues,
                buildTemplateOrigin("BRANCH_EQUIPMENT_JSON", "export branch->dtt branchId=" + branchId),
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

    private BranchDeviceTypeImportRequest toBranchDeviceTypeImportRequest(DttArchiveTemplate template) {
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
        if (dttVersion == null || dttVersion.isBlank()) {
            return "1.0";
        }
        return dttVersion.trim();
    }

    private Map<String, Object> buildTemplateOrigin(String sourceKind, String sourceSummary) {
        final Map<String, Object> origin = new LinkedHashMap<>();
        origin.put("sourceKind", sourceKind);
        origin.put("sourceSummary", sourceSummary);
        return origin;
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

    private Map<String, String> encodeArchives(Map<String, byte[]> archivesByDeviceTypeId) {
        final Map<String, String> encoded = new LinkedHashMap<>();
        archivesByDeviceTypeId.forEach((deviceTypeId, archive) ->
                encoded.put(deviceTypeId, Base64.getEncoder().encodeToString(archive))
        );
        return encoded;
    }

}
