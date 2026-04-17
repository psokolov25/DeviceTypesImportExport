package ru.aritmos.dtt.api;

import ru.aritmos.dtt.api.dto.BatchDttExportResult;
import ru.aritmos.dtt.api.dto.DeviceTypeTemplate;
import ru.aritmos.dtt.api.dto.EquipmentProfileAssemblyRequest;
import ru.aritmos.dtt.api.dto.EquipmentProfileDeviceTypeRequest;
import ru.aritmos.dtt.api.dto.MergeStrategy;
import ru.aritmos.dtt.api.dto.ValidationResult;
import ru.aritmos.dtt.api.dto.branch.BranchEquipmentAssemblyRequest;
import ru.aritmos.dtt.archive.DefaultDttArchiveReader;
import ru.aritmos.dtt.archive.DefaultDttArchiveWriter;
import ru.aritmos.dtt.archive.DttArchiveReader;
import ru.aritmos.dtt.archive.DttArchiveWriter;
import ru.aritmos.dtt.archive.model.DttArchiveDescriptor;
import ru.aritmos.dtt.archive.model.DttArchiveTemplate;
import ru.aritmos.dtt.assembly.DefaultTemplateAssemblyService;
import ru.aritmos.dtt.json.branch.BranchEquipment;
import ru.aritmos.dtt.json.branch.DefaultDeviceManagerBranchJsonGenerator;
import ru.aritmos.dtt.json.branch.DeviceManagerBranchJsonGenerator;
import ru.aritmos.dtt.json.profile.DefaultEquipmentProfileJsonGenerator;
import ru.aritmos.dtt.json.profile.EquipmentProfile;
import ru.aritmos.dtt.json.profile.EquipmentProfileJsonGenerator;
import ru.aritmos.dtt.validation.DefaultTemplateValidationService;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Дефолтная фасадная реализация для удобного использования библиотеки как Maven-зависимости.
 */
public class DefaultDeviceTemplateLibraryFacade implements DeviceTemplateLibraryFacade {

    private final DttArchiveReader archiveReader;
    private final DttArchiveWriter archiveWriter;
    private final TemplateValidationService validationService;
    private final TemplateAssemblyService assemblyService;
    private final EquipmentProfileJsonGenerator profileJsonGenerator;
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
                new DefaultEquipmentProfileJsonGenerator(),
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
            EquipmentProfileJsonGenerator profileJsonGenerator,
            DeviceManagerBranchJsonGenerator branchJsonGenerator
    ) {
        this.archiveReader = Objects.requireNonNull(archiveReader, "archiveReader is required");
        this.archiveWriter = Objects.requireNonNull(archiveWriter, "archiveWriter is required");
        this.validationService = Objects.requireNonNull(validationService, "validationService is required");
        this.assemblyService = Objects.requireNonNull(assemblyService, "assemblyService is required");
        this.profileJsonGenerator = Objects.requireNonNull(profileJsonGenerator, "profileJsonGenerator is required");
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
    public String toProfileJson(EquipmentProfile profile) {
        return profileJsonGenerator.generate(profile);
    }

    @Override
    public String toBranchJson(BranchEquipment branchEquipment) {
        return branchJsonGenerator.generate(branchEquipment);
    }

    @Override
    public BatchDttExportResult exportDttSetFromProfile(EquipmentProfile profile) {
        final Map<String, byte[]> archives = new LinkedHashMap<>();
        profile.deviceTypes().forEach((typeId, deviceType) -> {
            final DttArchiveTemplate template = new DttArchiveTemplate(
                    new DttArchiveDescriptor("DTT", "1.0", typeId),
                    deviceType.metadata(),
                    Map.of(),
                    Map.of(),
                    Map.of(),
                    deviceType.deviceTypeParamValues(),
                    Map.of(),
                    null,
                    null,
                    null,
                    null,
                    null,
                    Map.of(),
                    Map.of()
            );
            archives.put(typeId, writeDtt(template));
        });
        return new BatchDttExportResult(archives);
    }

    @Override
    public EquipmentProfile importDttSetToProfile(List<byte[]> archives, MergeStrategy mergeStrategy) {
        final List<EquipmentProfileDeviceTypeRequest> requests = archives.stream()
                .map(this::readDtt)
                .map(this::toDeviceTypeTemplate)
                .map(template -> new EquipmentProfileDeviceTypeRequest(template, true))
                .toList();

        return assembleProfile(new EquipmentProfileAssemblyRequest(requests, List.of(), mergeStrategy));
    }

    private DeviceTypeTemplate toDeviceTypeTemplate(DttArchiveTemplate template) {
        return new DeviceTypeTemplate(template.metadata(), template.defaultValues() == null ? Map.of() : template.defaultValues());
    }
}
