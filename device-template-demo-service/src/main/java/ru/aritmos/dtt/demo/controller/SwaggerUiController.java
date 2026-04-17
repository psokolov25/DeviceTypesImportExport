package ru.aritmos.dtt.demo.controller;

import io.micronaut.http.HttpResponse;
import io.micronaut.http.HttpStatus;
import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.Get;
import io.swagger.v3.oas.annotations.Hidden;

/**
 * Контроллер маршрутизации для открытия Swagger UI по короткому адресу `/swagger-ui`.
 *
 * <p>Фронтенд Swagger UI берется из webjar-зависимости, а этот контроллер только
 * выдает redirect на стандартный `index.html`, который инициализируется локальным `swagger-initializer.js`.
 */
@Controller
@Hidden
public class SwaggerUiController {

    private static final String SWAGGER_UI_LOCATION = "/swagger-ui/index.html";

    /**
     * Перенаправляет запросы `/swagger-ui` на страницу Swagger UI из webjar.
     *
     * @return redirect-ответ с Location на swagger-ui index
     */
    @Get("/swagger-ui")
    public HttpResponse<?> openSwaggerUi() {
        return HttpResponse.status(HttpStatus.FOUND).header("Location", SWAGGER_UI_LOCATION);
    }
}
