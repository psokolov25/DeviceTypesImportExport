package ru.aritmos.dtt.demo.controller;

import io.micronaut.http.HttpResponse;
import io.micronaut.http.HttpStatus;
import io.micronaut.http.MediaType;
import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.Get;
import io.swagger.v3.oas.annotations.Hidden;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

/**
 * Контроллер выдачи OpenAPI YAML со строго заданной UTF-8 кодировкой.
 *
 * <p>Нужен для корректного отображения кириллических описаний в браузере при открытии
 * {@code /swagger/device-template-demo.yml}. Статическая раздача ресурсов может не проставлять
 * charset в {@code Content-Type}, из-за чего некоторые браузеры интерпретируют YAML в неверной кодировке.
 */
@Hidden
@Controller("/swagger")
public class SwaggerSpecController {

    private static final String SPEC_CLASSPATH_PATH = "META-INF/swagger/device-template-demo.yml";
    private static final MediaType YAML_UTF8 = MediaType.of("application/x-yaml; charset=UTF-8");

    /**
     * Возвращает OpenAPI-спецификацию demo-service в YAML c charset UTF-8.
     *
     * @return HTTP-ответ с YAML-телом и явной UTF-8 кодировкой
     */
    @Get(uri = "/device-template-demo.yml", produces = "application/x-yaml")
    public HttpResponse<String> spec() {
        try (InputStream input = Thread.currentThread()
                .getContextClassLoader()
                .getResourceAsStream(SPEC_CLASSPATH_PATH)) {
            if (input == null) {
                return HttpResponse.status(HttpStatus.NOT_FOUND);
            }
            final String body = new String(input.readAllBytes(), StandardCharsets.UTF_8);
            return HttpResponse.ok(body).contentType(YAML_UTF8);
        } catch (IOException exception) {
            return HttpResponse.serverError();
        }
    }
}
