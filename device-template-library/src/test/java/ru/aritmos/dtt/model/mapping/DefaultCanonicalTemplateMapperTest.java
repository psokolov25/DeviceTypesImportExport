package ru.aritmos.dtt.model.mapping;

import org.junit.jupiter.api.Test;
import ru.aritmos.dtt.api.dto.DeviceTypeMetadata;
import ru.aritmos.dtt.archive.model.DttArchiveDescriptor;
import ru.aritmos.dtt.archive.model.DttArchiveTemplate;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class DefaultCanonicalTemplateMapperTest {

    private final CanonicalTemplateMapper mapper = new DefaultCanonicalTemplateMapper();

    @Test
    void shouldMapArchiveToCanonicalAndBackWithoutLosingScripts() {
        final DttArchiveTemplate archive = new DttArchiveTemplate(
                new DttArchiveDescriptor("DTT", "1.2", "display"),
                new DeviceTypeMetadata("display", "Display", "Дисплей", "desc"),
                Map.of("p", Map.of("type", "String")),
                Map.of("ip", Map.of("type", "String")),
                Map.of("h", "v"),
                Map.of("a", "1"),
                Map.of("a", "2"),
                "println 'start'",
                "println 'stop'",
                "println 'publicStart'",
                "println 'publicFinish'",
                "def f() { 1 }",
                Map.of("VISIT", "println 'visit'"),
                Map.of("RESET", "println 'reset'")
        );

        final var canonical = mapper.toCanonical(archive);
        final var restored = mapper.toArchive(canonical);

        assertThat(restored.metadata().id()).isEqualTo("display");
        assertThat(restored.descriptor().formatVersion()).isEqualTo("1.2");
        assertThat(restored.onStartEvent()).isEqualTo("println 'start'");
        assertThat(restored.eventHandlers()).containsEntry("VISIT", "println 'visit'");
        assertThat(restored.commands()).containsEntry("RESET", "println 'reset'");
    }
}
