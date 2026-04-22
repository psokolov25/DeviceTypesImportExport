package ru.aritmos.dtt.archive;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DttIconSupportTest {

    @AfterEach
    void tearDown() {
        DttIconSupport.resetDefaultIconBase64();
    }

    @Test
    void shouldUseConfiguredDefaultIconWhenSourceValueMissing() {
        final String configured = "iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR4nGNgYAAAAAMAASsJTYQAAAAASUVORK5CYII=";
        DttIconSupport.configureDefaultIconBase64(configured);

        assertThat(DttIconSupport.resolveOrDefault(null)).isEqualTo(configured);
        assertThat(DttIconSupport.resolveOrDefault("   ")).isEqualTo(configured);
    }

    @Test
    void shouldRejectInvalidConfiguredIconBase64() {
        assertThatThrownBy(() -> DttIconSupport.configureDefaultIconBase64("not-base64"))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
