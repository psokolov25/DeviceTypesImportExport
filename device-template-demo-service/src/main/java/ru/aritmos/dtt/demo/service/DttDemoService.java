package ru.aritmos.dtt.demo.service;

import ru.aritmos.dtt.archive.DefaultDttArchiveReader;
import ru.aritmos.dtt.archive.model.DttArchiveTemplate;
import ru.aritmos.dtt.validation.DefaultTemplateValidationService;
import ru.aritmos.dtt.demo.dto.DttInspectionResponse;
import ru.aritmos.dtt.demo.dto.DttValidationIssueResponse;
import ru.aritmos.dtt.demo.dto.DttValidationResponse;

import java.io.ByteArrayInputStream;
import java.util.List;

/**
 * Сервис demo-модуля для валидации и инспекции DTT-архивов.
 */
public class DttDemoService {

    private final DefaultDttArchiveReader archiveReader = new DefaultDttArchiveReader();
    private final DefaultTemplateValidationService validationService = new DefaultTemplateValidationService();

    /**
     * @param archiveBytes бинарное содержимое DTT-архива
     * @return результат валидации
     */
    public DttValidationResponse validate(byte[] archiveBytes) {
        final DttArchiveTemplate template = archiveReader.read(new ByteArrayInputStream(archiveBytes));
        final var result = validationService.validate(template);
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
        final DttArchiveTemplate template = archiveReader.read(new ByteArrayInputStream(archiveBytes));
        return new DttInspectionResponse(
                template.descriptor().formatName(),
                template.descriptor().formatVersion(),
                template.metadata().id(),
                template.metadata().name(),
                template.eventHandlers() == null ? 0 : template.eventHandlers().size(),
                template.commands() == null ? 0 : template.commands().size()
        );
    }
}
