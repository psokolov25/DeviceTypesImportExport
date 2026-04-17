package ru.aritmos.dtt.demo.controller;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class HealthControllerTest {

    @Test
    void shouldReturnUpStatus() {
        final HealthController controller = new HealthController();

        assertThat(controller.health().status()).isEqualTo("UP");
    }
}
