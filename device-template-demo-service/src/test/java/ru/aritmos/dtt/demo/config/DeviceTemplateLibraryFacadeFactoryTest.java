package ru.aritmos.dtt.demo.config;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import ru.aritmos.dtt.archive.DttIconSupport;

import static org.assertj.core.api.Assertions.assertThat;

class DeviceTemplateLibraryFacadeFactoryTest {

    @AfterEach
    void tearDown() {
        DttIconSupport.resetDefaultIconBase64();
    }

    @Test
    void shouldApplyConfiguredDefaultIcon() {
        final String custom = "iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR4nGNgYAAAAAMAASsJTYQAAAAASUVORK5CYII=";
        final DeviceTemplateLibraryFacadeFactory factory = new DeviceTemplateLibraryFacadeFactory();

        factory.applyConfiguredDefaultIcon(custom);

        assertThat(DttIconSupport.resolveOrDefault(null)).isEqualTo(custom);
    }
}
