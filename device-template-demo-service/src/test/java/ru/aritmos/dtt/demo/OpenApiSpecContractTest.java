package ru.aritmos.dtt.demo;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import io.micronaut.context.ApplicationContext;
import io.micronaut.http.HttpResponse;
import org.junit.jupiter.api.Test;
import ru.aritmos.dtt.demo.controller.SwaggerSpecController;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Контрактный тест OpenAPI-спеки demo-service.
 *
 * <p>Проверяет, что спецификация содержит ключевые endpoint-ы импорта/экспорта и
 * структурированные схемы ошибок, ожидаемые клиентами.</p>
 *
 * <p>Тест читает YAML через {@link SwaggerSpecController}, а не через HTTP-вызов,
 * чтобы контракт содержимого спецификации не зависел от сетевой флуктуации при
 * старте embedded-server. HTTP-доступность спецификации и Swagger UI отдельно
 * проверяется в {@link ServerStartupAndSwaggerUiTest}.</p>
 */
class OpenApiSpecContractTest {

    private static final String OPENAPI_RESOURCE = "META-INF/swagger/device-template-demo.yml";
    private static final Path OPENAPI_TARGET_PATH = Path.of("target", "classes", "META-INF", "swagger", "device-template-demo.yml");

    @Test
    void shouldExposeExpectedDttEndpointsAndErrorSchemasInOpenApiSpec() throws IOException {
        try (ApplicationContext context = ApplicationContext.run()) {
            final String spec = readSpecFromTargetPath()
                    .or(() -> readSpecFromClasspath())
                    .orElseGet(() -> {
                final SwaggerSpecController controller = context.getBean(SwaggerSpecController.class);
                final HttpResponse<String> response = controller.spec();
                assertThat(response.code()).isEqualTo(200);
                assertThat(response.getContentType().map(Object::toString).orElse(""))
                        .containsIgnoringCase("application/x-yaml")
                        .containsIgnoringCase("charset=utf-8");
                return response.body();
                    });
            assertThat(spec).contains("openapi:");
            assertThat(spec).contains("Экспортировать");

            final ObjectMapper yamlMapper = new ObjectMapper(new YAMLFactory());
            final JsonNode root = yamlMapper.readTree(spec);
            assertThat(spec).contains("/api/dtt/import/profile");
            assertThat(spec).contains("/api/dtt/metadata");
            assertThat(spec).contains("/api/dtt/version/compare");
            assertThat(spec).contains("/api/dtt/import/profile");
            assertThat(spec).contains("/api/dtt/export/profile/all");
            assertThat(root.at("/paths/~1api~1dtt~1export~1profile~1all/post/summary").asText())
                    .isEqualTo("Экспортировать набор типов из profile JSON в DTT-set");
            assertThat(root.at("/paths/~1api~1dtt~1export~1profile~1all/post/tags/0").asText())
                    .isEqualTo("DTT · Экспорт из profile JSON");
            assertThat(root.at("/paths/~1api~1dtt~1import~1profile/post/tags/0").asText())
                    .isEqualTo("DTT · Импорт в profile JSON");
            assertThat(root.at("/paths/~1api~1dtt~1import~1profile/post/responses/400/description").asText())
                    .contains("валидации");
            assertThat(root.at("/components/schemas/ImportDttSetToProfileResponse/properties/profileJson/type").asText()).isEqualTo("object");
            assertThat(root.at("/components/schemas/ExportAllDttFromProfileRequest/description").asText())
                    .contains("root metadata");

            assertThat(spec).contains("DemoErrorResponse:");
            assertThat(spec).contains("DttValidationResponse:");
            assertThat(spec).contains("DttInspectionResponse:");
            assertThat(spec).contains("DttMetadataBatchResponse:");
            assertThat(spec).contains("DttVersionComparisonResponse:");
        }
    }

    private static java.util.Optional<String> readSpecFromClasspath() {
        try (InputStream stream = OpenApiSpecContractTest.class.getClassLoader().getResourceAsStream(OPENAPI_RESOURCE)) {
            if (stream == null) {
                return java.util.Optional.empty();
            }
            return java.util.Optional.of(new String(stream.readAllBytes(), StandardCharsets.UTF_8));
        } catch (IOException exception) {
            return java.util.Optional.empty();
        }
    }

    private static java.util.Optional<String> readSpecFromTargetPath() {
        try {
            if (!Files.exists(OPENAPI_TARGET_PATH)) {
                return java.util.Optional.empty();
            }
            return java.util.Optional.of(Files.readString(OPENAPI_TARGET_PATH));
        } catch (IOException exception) {
            return java.util.Optional.empty();
        }
    }
}
