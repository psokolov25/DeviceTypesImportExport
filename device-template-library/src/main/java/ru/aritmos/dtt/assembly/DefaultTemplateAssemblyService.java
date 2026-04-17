package ru.aritmos.dtt.assembly;

import ru.aritmos.dtt.api.TemplateAssemblyService;
import ru.aritmos.dtt.api.dto.DeviceTypeTemplate;
import ru.aritmos.dtt.api.dto.EquipmentProfileAssemblyRequest;
import ru.aritmos.dtt.api.dto.EquipmentProfileDeviceTypeRequest;
import ru.aritmos.dtt.api.dto.MergeStrategy;
import ru.aritmos.dtt.api.dto.TemplateValueOverride;
import ru.aritmos.dtt.api.dto.branch.BranchDeviceTypeImportRequest;
import ru.aritmos.dtt.api.dto.branch.BranchEquipmentAssemblyRequest;
import ru.aritmos.dtt.api.dto.branch.BranchImportRequest;
import ru.aritmos.dtt.api.dto.branch.DeviceInstanceImportRequest;
import ru.aritmos.dtt.exception.TemplateAssemblyException;
import ru.aritmos.dtt.json.branch.BranchDeviceType;
import ru.aritmos.dtt.json.branch.BranchEquipment;
import ru.aritmos.dtt.json.branch.BranchNode;
import ru.aritmos.dtt.json.branch.DeviceInstanceTemplate;
import ru.aritmos.dtt.json.profile.EquipmentProfile;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Базовая реализация сервиса сборки profile JSON и branch equipment JSON из DTT-шаблонов.
 */
public class DefaultTemplateAssemblyService implements TemplateAssemblyService {

    @Override
    public EquipmentProfile assembleEquipmentProfile(EquipmentProfileAssemblyRequest request) {
        Objects.requireNonNull(request, "request is required");
        final Map<String, DeviceTypeTemplate> result = new LinkedHashMap<>();
        final MergeStrategy mergeStrategy = request.mergeStrategy() == null
                ? MergeStrategy.FAIL_IF_EXISTS
                : request.mergeStrategy();

        for (EquipmentProfileDeviceTypeRequest deviceTypeRequest : request.deviceTypes()) {
            final DeviceTypeTemplate template = withOverrides(deviceTypeRequest.template(), request.overrides());
            final String key = template.metadata().id();

            if (!result.containsKey(key)) {
                result.put(key, template);
                continue;
            }

            mergeProfileConflict(result, key, template, mergeStrategy);
        }
        return new EquipmentProfile(result);
    }

    @Override
    public BranchEquipment assembleBranchEquipment(BranchEquipmentAssemblyRequest request) {
        Objects.requireNonNull(request, "request is required");
        final MergeStrategy mergeStrategy = request.mergeStrategy() == null
                ? MergeStrategy.FAIL_IF_EXISTS
                : request.mergeStrategy();
        final Map<String, BranchNode> branches = new LinkedHashMap<>();
        for (BranchImportRequest branchRequest : request.branches()) {
            final Map<String, BranchDeviceType> deviceTypes = new LinkedHashMap<>();
            for (BranchDeviceTypeImportRequest deviceTypeImport : branchRequest.deviceTypes()) {
                final DeviceTypeTemplate template = deviceTypeImport.deviceTypeRequest().template();
                final String typeId = template.metadata().id();
                final BranchDeviceType incoming = new BranchDeviceType(template, toDeviceMap(deviceTypeImport.deviceInstances()));
                if (!deviceTypes.containsKey(typeId)) {
                    deviceTypes.put(typeId, incoming);
                    continue;
                }
                mergeBranchConflict(deviceTypes, typeId, incoming, mergeStrategy);
            }
            branches.put(
                    branchRequest.branchId(),
                    new BranchNode(branchRequest.branchId(), branchRequest.displayName(), deviceTypes)
            );
        }
        return new BranchEquipment(branches);
    }

    private Map<String, DeviceInstanceTemplate> toDeviceMap(List<DeviceInstanceImportRequest> deviceInstances) {
        final Map<String, DeviceInstanceTemplate> devices = new LinkedHashMap<>();
        if (deviceInstances == null) {
            return devices;
        }
        for (DeviceInstanceImportRequest instance : deviceInstances) {
            devices.put(
                    instance.id(),
                    new DeviceInstanceTemplate(
                            instance.id(),
                            instance.name(),
                            instance.displayName(),
                            instance.description(),
                            instance.deviceParamValues() == null ? Map.of() : instance.deviceParamValues()
                    )
            );
        }
        return devices;
    }

    private DeviceTypeTemplate withOverrides(DeviceTypeTemplate template, List<TemplateValueOverride> overrides) {
        if (overrides == null || overrides.isEmpty()) {
            return template;
        }
        final Map<String, Object> values = new LinkedHashMap<>(template.deviceTypeParamValues());
        overrides.stream()
                .filter(override -> template.metadata().id().equals(override.deviceTypeId()))
                .map(TemplateValueOverride::values)
                .forEach(values::putAll);
        return new DeviceTypeTemplate(template.metadata(), values);
    }

    private void mergeProfileConflict(Map<String, DeviceTypeTemplate> result,
                                      String key,
                                      DeviceTypeTemplate incoming,
                                      MergeStrategy mergeStrategy) {
        final DeviceTypeTemplate existing = result.get(key);
        switch (mergeStrategy) {
            case FAIL_IF_EXISTS -> throw new TemplateAssemblyException(
                    "Тип устройства '%s' уже существует в собираемом профиле".formatted(key));
            case REPLACE -> result.put(key, incoming);
            case MERGE_NON_NULLS -> result.put(key, mergeNonNulls(existing, incoming));
            case MERGE_PRESERVE_EXISTING -> result.put(key, mergePreserveExisting(existing, incoming));
            case CREATE_COPY_WITH_SUFFIX -> result.put(nextCopyKey(result, key), incoming);
        }
    }

    private void mergeBranchConflict(Map<String, BranchDeviceType> result,
                                     String key,
                                     BranchDeviceType incoming,
                                     MergeStrategy mergeStrategy) {
        switch (mergeStrategy) {
            case FAIL_IF_EXISTS -> throw new TemplateAssemblyException(
                    "Тип устройства '%s' уже существует в отделении".formatted(key));
            case REPLACE -> result.put(key, incoming);
            case MERGE_NON_NULLS, MERGE_PRESERVE_EXISTING -> result.put(key, mergeBranchDevices(result.get(key), incoming));
            case CREATE_COPY_WITH_SUFFIX -> result.put(nextCopyKey(result, key), incoming);
        }
    }

    private BranchDeviceType mergeBranchDevices(BranchDeviceType existing, BranchDeviceType incoming) {
        final Map<String, DeviceInstanceTemplate> merged = new LinkedHashMap<>(existing.devices());
        merged.putAll(incoming.devices());
        return new BranchDeviceType(existing.template(), merged);
    }

    private DeviceTypeTemplate mergeNonNulls(DeviceTypeTemplate existing, DeviceTypeTemplate incoming) {
        final Map<String, Object> merged = new HashMap<>(existing.deviceTypeParamValues());
        incoming.deviceTypeParamValues().forEach((k, v) -> {
            if (v != null) {
                merged.put(k, v);
            }
        });
        return new DeviceTypeTemplate(existing.metadata(), merged);
    }

    private DeviceTypeTemplate mergePreserveExisting(DeviceTypeTemplate existing, DeviceTypeTemplate incoming) {
        final Map<String, Object> merged = new HashMap<>(existing.deviceTypeParamValues());
        incoming.deviceTypeParamValues().forEach((k, v) -> merged.putIfAbsent(k, v));
        return new DeviceTypeTemplate(existing.metadata(), merged);
    }

    private <T> String nextCopyKey(Map<String, T> result, String key) {
        int suffix = 1;
        String candidate = key + "_copy_" + suffix;
        while (result.containsKey(candidate)) {
            suffix++;
            candidate = key + "_copy_" + suffix;
        }
        return candidate;
    }
}
