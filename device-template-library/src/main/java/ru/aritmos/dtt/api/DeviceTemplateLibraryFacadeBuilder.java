package ru.aritmos.dtt.api;

import ru.aritmos.dtt.archive.DefaultDttArchiveReader;
import ru.aritmos.dtt.archive.DefaultDttArchiveWriter;
import ru.aritmos.dtt.archive.DttArchiveReader;
import ru.aritmos.dtt.archive.DttArchiveWriter;
import ru.aritmos.dtt.assembly.DefaultTemplateAssemblyService;
import ru.aritmos.dtt.json.branch.DefaultDeviceManagerBranchJsonGenerator;
import ru.aritmos.dtt.json.branch.DeviceManagerBranchJsonGenerator;
import ru.aritmos.dtt.json.profile.DefaultEquipmentProfileJsonGenerator;
import ru.aritmos.dtt.json.profile.EquipmentProfileJsonGenerator;
import ru.aritmos.dtt.validation.DefaultTemplateValidationService;

import java.util.Objects;

/**
 * Builder фасада библиотеки для случаев, когда приложению нужно подменить отдельные реализации сервисов.
 */
public class DeviceTemplateLibraryFacadeBuilder {

    private DttArchiveReader archiveReader = new DefaultDttArchiveReader();
    private DttArchiveWriter archiveWriter = new DefaultDttArchiveWriter();
    private TemplateValidationService validationService = new DefaultTemplateValidationService();
    private TemplateAssemblyService assemblyService = new DefaultTemplateAssemblyService();
    private EquipmentProfileJsonGenerator profileJsonGenerator = new DefaultEquipmentProfileJsonGenerator();
    private DeviceManagerBranchJsonGenerator branchJsonGenerator = new DefaultDeviceManagerBranchJsonGenerator();

    /**
     * @param archiveReader реализация reader DTT
     * @return builder
     */
    public DeviceTemplateLibraryFacadeBuilder withArchiveReader(DttArchiveReader archiveReader) {
        this.archiveReader = Objects.requireNonNull(archiveReader, "archiveReader is required");
        return this;
    }

    /**
     * @param archiveWriter реализация writer DTT
     * @return builder
     */
    public DeviceTemplateLibraryFacadeBuilder withArchiveWriter(DttArchiveWriter archiveWriter) {
        this.archiveWriter = Objects.requireNonNull(archiveWriter, "archiveWriter is required");
        return this;
    }

    /**
     * @param validationService реализация валидации
     * @return builder
     */
    public DeviceTemplateLibraryFacadeBuilder withValidationService(TemplateValidationService validationService) {
        this.validationService = Objects.requireNonNull(validationService, "validationService is required");
        return this;
    }

    /**
     * @param assemblyService реализация сборки
     * @return builder
     */
    public DeviceTemplateLibraryFacadeBuilder withAssemblyService(TemplateAssemblyService assemblyService) {
        this.assemblyService = Objects.requireNonNull(assemblyService, "assemblyService is required");
        return this;
    }

    /**
     * @param profileJsonGenerator генератор profile JSON
     * @return builder
     */
    public DeviceTemplateLibraryFacadeBuilder withProfileJsonGenerator(EquipmentProfileJsonGenerator profileJsonGenerator) {
        this.profileJsonGenerator = Objects.requireNonNull(profileJsonGenerator, "profileJsonGenerator is required");
        return this;
    }

    /**
     * @param branchJsonGenerator генератор branch JSON
     * @return builder
     */
    public DeviceTemplateLibraryFacadeBuilder withBranchJsonGenerator(DeviceManagerBranchJsonGenerator branchJsonGenerator) {
        this.branchJsonGenerator = Objects.requireNonNull(branchJsonGenerator, "branchJsonGenerator is required");
        return this;
    }

    /**
     * Создаёт фасад с выбранными реализациями.
     *
     * @return фасад библиотеки
     */
    public DeviceTemplateLibraryFacade build() {
        return new DefaultDeviceTemplateLibraryFacade(
                archiveReader,
                archiveWriter,
                validationService,
                assemblyService,
                profileJsonGenerator,
                branchJsonGenerator
        );
    }
}
