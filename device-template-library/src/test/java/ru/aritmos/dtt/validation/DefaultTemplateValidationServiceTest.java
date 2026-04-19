package ru.aritmos.dtt.validation;

import org.junit.jupiter.api.Test;
import ru.aritmos.dtt.api.TemplateValidationService;
import ru.aritmos.dtt.api.dto.DeviceTypeMetadata;
import ru.aritmos.dtt.archive.model.DttArchiveDescriptor;
import ru.aritmos.dtt.archive.model.DttArchiveTemplate;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class DefaultTemplateValidationServiceTest {

    private final TemplateValidationService validationService = new DefaultTemplateValidationService();

    @Test
    void shouldReturnValidResultForCorrectGroovyScripts() {
        final var result = validationService.validate(templateWithScripts("println 'ok'", "println 'handler'"));

        assertThat(result.valid()).isTrue();
        assertThat(result.issues()).isEmpty();
    }

    @Test
    void shouldDetectSyntaxErrorsInLifecycleScripts() {
        final var result = validationService.validate(templateWithScripts("if (", "println 'handler'"));

        assertThat(result.valid()).isFalse();
        assertThat(result.issues())
                .anySatisfy(issue -> {
                    assertThat(issue.code()).isEqualTo("GROOVY_SYNTAX_ERROR");
                    assertThat(issue.path()).isEqualTo("scripts/onStartEvent.groovy");
                });
    }

    @Test
    void shouldDetectSyntaxErrorsInEventHandlersAndCommands() {
        final DttArchiveTemplate template = new DttArchiveTemplate(
                new DttArchiveDescriptor("DTT", "1.0", "display", null),
                new DeviceTypeMetadata("display", "Display", "Дисплей", "desc"),
                Map.of(),
                Map.of(),
                Map.of(),
                Map.of(),
                Map.of(),
                Map.of(),
                "println 'ok'",
                null,
                null,
                null,
                null,
                Map.of("EVENT", "if ("),
                Map.of("CMD", "if (")
        );

        final var result = validationService.validate(template);

        assertThat(result.valid()).isFalse();
        assertThat(result.issues())
                .extracting("path")
                .contains("scripts/event-handlers/EVENT.groovy", "scripts/commands/CMD.groovy");
    }

    private DttArchiveTemplate templateWithScripts(String onStart, String handler) {
        return new DttArchiveTemplate(
                new DttArchiveDescriptor("DTT", "1.0", "display", null),
                new DeviceTypeMetadata("display", "Display", "Дисплей", "desc"),
                Map.of(),
                Map.of(),
                Map.of(),
                Map.of(),
                Map.of(),
                Map.of(),
                onStart,
                "println 'stop'",
                "println 'publicStart'",
                "println 'publicFinish'",
                "def f() { 1 }",
                Map.of("EVENT", handler),
                Map.of("CMD", "println 'cmd'")
        );
    }
}
