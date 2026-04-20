package ru.aritmos.dtt.demo;

import io.micronaut.context.ApplicationContext;
import io.micronaut.runtime.server.EmbeddedServer;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Интеграционный контрактный тест OpenAPI-спеки demo-service.
 *
 * <p>Проверяет, что спецификация содержит ключевые endpoint-ы импорта/экспорта и
 * структурированные схемы ошибок, ожидаемые клиентами.</p>
 */
class OpenApiSpecContractTest {

    @Test
    void shouldExposeExpectedDttEndpointsAndErrorSchemasInOpenApiSpec() throws IOException, InterruptedException {
        try (EmbeddedServer server = ApplicationContext.run(EmbeddedServer.class, Map.of("micronaut.server.port", -1))) {
            final HttpClient client = HttpClient.newBuilder()
                    .connectTimeout(Duration.ofSeconds(5))
                    .build();

            final HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(server.getURL() + "/swagger/device-template-demo.yml"))
                    .GET()
                    .timeout(Duration.ofSeconds(5))
                    .build();

            final HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            assertThat(response.statusCode()).isEqualTo(200);

            final String spec = response.body();
            assertThat(spec).contains("openapi:");
            assertThat(spec).contains("/api/dtt/import/profile");
            assertThat(spec).contains("/api/dtt/import/profile/upload");
            assertThat(spec).contains("/api/dtt/import/branch");
            assertThat(spec).contains("/api/dtt/import/branch/merge");
            assertThat(spec).contains("/api/dtt/export/profile/one");
            assertThat(spec).contains("/api/dtt/export/profile/one/download");
            assertThat(spec).contains("/api/dtt/preview/export/profile/one");
            assertThat(spec).contains("/api/dtt/export/profile/all");
            assertThat(spec).contains("/api/dtt/export/branch/one");
            assertThat(spec).contains("/api/dtt/export/branch/one/download");
            assertThat(spec).contains("/api/dtt/preview/export/branch/one");
            assertThat(spec).contains("/api/dtt/export/branch/all");

            assertThat(spec).contains("DemoErrorResponse:");
            assertThat(spec).contains("DttValidationResponse:");
            assertThat(spec).contains("DttInspectionResponse:");
            assertThat(spec).contains("SingleDttExportPreviewResponse:");
            assertThat(spec).contains("BAD_REQUEST");
        }
    }
}
