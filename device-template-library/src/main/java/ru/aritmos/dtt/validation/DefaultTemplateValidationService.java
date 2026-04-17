package ru.aritmos.dtt.validation;

import org.codehaus.groovy.control.CompilationFailedException;
import ru.aritmos.dtt.api.TemplateValidationService;
import ru.aritmos.dtt.api.dto.ValidationIssue;
import ru.aritmos.dtt.api.dto.ValidationResult;
import ru.aritmos.dtt.archive.model.DttArchiveTemplate;
import ru.aritmos.dtt.exception.TemplateValidationException;

import groovy.lang.GroovyShell;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Базовая реализация валидации DTT, выполняющая проверку обязательных полей и синтаксиса Groovy-скриптов.
 */
public class DefaultTemplateValidationService implements TemplateValidationService {

    private static final String GROOVY_SYNTAX_ERROR = "GROOVY_SYNTAX_ERROR";
    private static final String REQUIRED_FIELD_MISSING = "REQUIRED_FIELD_MISSING";

    private final GroovyShell groovyShell;

    /**
     * Создаёт сервис с отдельным экземпляром {@link GroovyShell}.
     */
    public DefaultTemplateValidationService() {
        this.groovyShell = new GroovyShell();
    }

    @Override
    public ValidationResult validate(DttArchiveTemplate template) {
        Objects.requireNonNull(template, "template is required");
        try {
            final List<ValidationIssue> issues = new ArrayList<>();
            validateRequired(template, issues);
            validateGroovy("scripts/onStartEvent.groovy", template.onStartEvent(), issues);
            validateGroovy("scripts/onStopEvent.groovy", template.onStopEvent(), issues);
            validateGroovy("scripts/onPublicStartEvent.groovy", template.onPublicStartEvent(), issues);
            validateGroovy("scripts/onPublicFinishEvent.groovy", template.onPublicFinishEvent(), issues);
            validateGroovy("scripts/deviceTypeFunctions.groovy", template.deviceTypeFunctions(), issues);
            validateGroovyMap("scripts/event-handlers", template.eventHandlers(), issues);
            validateGroovyMap("scripts/commands", template.commands(), issues);
            return new ValidationResult(issues.isEmpty(), List.copyOf(issues));
        } catch (RuntimeException exception) {
            throw new TemplateValidationException("Ошибка валидации шаблона DTT", exception);
        }
    }

    private void validateRequired(DttArchiveTemplate template, List<ValidationIssue> issues) {
        if (template.descriptor() == null) {
            issues.add(new ValidationIssue(REQUIRED_FIELD_MISSING, "manifest.yml", "Отсутствует descriptor"));
            return;
        }
        if (isBlank(template.descriptor().formatName())) {
            issues.add(new ValidationIssue(REQUIRED_FIELD_MISSING, "manifest.yml:formatName", "Пустой formatName"));
        }
        if (isBlank(template.descriptor().formatVersion())) {
            issues.add(new ValidationIssue(REQUIRED_FIELD_MISSING, "manifest.yml:formatVersion", "Пустой formatVersion"));
        }
        if (template.metadata() == null || isBlank(template.metadata().id())) {
            issues.add(new ValidationIssue(REQUIRED_FIELD_MISSING, "template/device-type.yml:id", "Отсутствует id типа устройства"));
        }
    }

    private void validateGroovyMap(String directory, Map<String, String> scripts, List<ValidationIssue> issues) {
        if (scripts == null) {
            return;
        }
        scripts.forEach((name, script) -> validateGroovy(directory + "/" + name + ".groovy", script, issues));
    }

    private void validateGroovy(String path, String script, List<ValidationIssue> issues) {
        if (isBlank(script)) {
            return;
        }
        try {
            groovyShell.parse(script);
        } catch (CompilationFailedException exception) {
            issues.add(new ValidationIssue(
                    GROOVY_SYNTAX_ERROR,
                    path,
                    "Groovy-скрипт не компилируется: " + exception.getMessage()
            ));
        }
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
