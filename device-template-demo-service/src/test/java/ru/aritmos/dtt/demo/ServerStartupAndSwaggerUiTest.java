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
 * Интеграционный тест запуска demo-service и проверки доступности Swagger UI.
 */
class ServerStartupAndSwaggerUiTest {

    @Test
    void shouldStartServerAndExposeHealthAndSwaggerUi() throws IOException, InterruptedException {
        try (EmbeddedServer server = ApplicationContext.run(EmbeddedServer.class, Map.of("micronaut.server.port", -1))) {
            assertThat(server.isRunning()).isTrue();

            final HttpClient client = HttpClient.newBuilder()
                    .connectTimeout(Duration.ofSeconds(5))
                    .build();

            final HttpResponse<String> healthResponse = sendGet(client, server.getURL() + "/api/system/health");
            assertThat(healthResponse.statusCode()).isEqualTo(200);
            assertThat(healthResponse.body()).contains("UP");

            final HttpResponse<String> swaggerSpecResponse = sendGet(client, server.getURL() + "/swagger/device-template-demo.yml");
            assertThat(swaggerSpecResponse.statusCode()).isEqualTo(200);
            assertThat(swaggerSpecResponse.body()).contains("openapi: 3.0.3");

            final HttpResponse<String> swaggerUiRedirectResponse = sendGetWithoutRedirects(server.getURL() + "/swagger-ui");
            assertThat(swaggerUiRedirectResponse.statusCode()).isEqualTo(302);
            assertThat(swaggerUiRedirectResponse.headers().firstValue("location"))
                    .contains("/swagger-ui-assets/index.html?url=/swagger/device-template-demo.yml");

            final HttpResponse<String> swaggerUiPageResponse = sendGet(client, server.getURL() + "/swagger-ui-assets/index.html?url=/swagger/device-template-demo.yml");
            assertThat(swaggerUiPageResponse.statusCode()).isEqualTo(200);
            assertThat(swaggerUiPageResponse.body()).contains("<div id=\"swagger-ui\"></div>");
            assertThat(swaggerUiPageResponse.body()).contains("./swagger-ui-bundle.js");

            final HttpResponse<String> swaggerInitializerResponse = sendGet(client, server.getURL() + "/swagger-ui-assets/swagger-initializer.js?url=/swagger/device-template-demo.yml");
            assertThat(swaggerInitializerResponse.statusCode()).isEqualTo(200);
            assertThat(swaggerInitializerResponse.body()).contains("SwaggerUIBundle");
        }
    }

    private HttpResponse<String> sendGet(HttpClient client, String url) throws IOException, InterruptedException {
        final HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .GET()
                .timeout(Duration.ofSeconds(5))
                .build();
        return client.send(request, HttpResponse.BodyHandlers.ofString());
    }

    private HttpResponse<String> sendGetWithoutRedirects(String url) throws IOException, InterruptedException {
        final HttpClient noRedirectClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(5))
                .followRedirects(HttpClient.Redirect.NEVER)
                .build();
        final HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .GET()
                .timeout(Duration.ofSeconds(5))
                .build();
        return noRedirectClient.send(request, HttpResponse.BodyHandlers.ofString());
    }
}
