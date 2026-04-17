package ru.aritmos.dtt.archive;

import org.junit.jupiter.api.Test;
import ru.aritmos.dtt.api.dto.DeviceTypeMetadata;
import ru.aritmos.dtt.archive.model.DttArchiveDescriptor;
import ru.aritmos.dtt.archive.model.DttArchiveTemplate;
import ru.aritmos.dtt.export.DefaultDeviceTypeTemplateExportService;
import ru.aritmos.dtt.importing.DefaultDeviceTypeTemplateImportService;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class DeviceTypeTemplateImportExportServiceTest {

    @Test
    void shouldExportAndImportTemplate() {
        final DttArchiveTemplate template = new DttArchiveTemplate(
                new DttArchiveDescriptor("DTT", "1.0", "terminal"),
                new DeviceTypeMetadata("terminal", "Terminal", "Терминал", "desc"),
                Map.of(),
                Map.of(),
                Map.of(),
                Map.of(),
                Map.of(),
                "println 's'",
                "println 'e'",
                null,
                null,
                null,
                Map.of(),
                Map.of()
        );

        final DefaultDeviceTypeTemplateExportService exportService =
                new DefaultDeviceTypeTemplateExportService(new DefaultDttArchiveWriter());
        final DefaultDeviceTypeTemplateImportService importService =
                new DefaultDeviceTypeTemplateImportService(new DefaultDttArchiveReader());

        final ByteArrayOutputStream output = new ByteArrayOutputStream();
        exportService.exportOne(template, output);

        final DttArchiveTemplate restored = importService.importOne(new ByteArrayInputStream(output.toByteArray()));

        assertThat(restored.metadata().id()).isEqualTo("terminal");
        assertThat(restored.onStartEvent()).isEqualTo("println 's'");
    }
}
