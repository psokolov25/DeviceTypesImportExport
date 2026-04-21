package ru.aritmos.dtt.api;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class DttVersionSupportTest {

    @Test
    void shouldCompareNumericVersions() {
        assertThat(DttVersionSupport.compare("2.1.0", "2.0.9")).isGreaterThan(0);
        assertThat(DttVersionSupport.compare("1.0", "1.0.0")).isEqualTo(0);
    }

    @Test
    void shouldReturnMaxVersion() {
        assertThat(DttVersionSupport.max("3.0.0", "2.9.9")).isEqualTo("3.0.0");
        assertThat(DttVersionSupport.max(null, "2.0.0")).isEqualTo("2.0.0");
    }
}
