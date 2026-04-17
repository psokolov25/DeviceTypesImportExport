package ru.aritmos.dtt.demo.controller;

import io.micronaut.http.HttpResponse;
import io.micronaut.http.MediaType;
import io.micronaut.http.annotation.Body;
import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.Error;
import io.micronaut.http.annotation.Post;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import ru.aritmos.dtt.demo.dto.DemoErrorResponse;
import ru.aritmos.dtt.demo.dto.DttInspectionResponse;
import ru.aritmos.dtt.demo.dto.DttValidationResponse;
import ru.aritmos.dtt.demo.dto.ExportAllDttFromProfileRequest;
import ru.aritmos.dtt.demo.dto.ExportAllDttFromProfileResponse;
import ru.aritmos.dtt.demo.dto.ImportDttSetToProfileRequest;
import ru.aritmos.dtt.demo.dto.ImportDttSetToProfileResponse;
import ru.aritmos.dtt.demo.service.DttDemoService;
import ru.aritmos.dtt.exception.DttFormatException;

import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

/**
 * Demo API для валидации, инспекции и импорта DTT-архивов.
 */
@Controller("/api/dtt")
@Tag(name = "DTT")
public class DttController {

    private final DttDemoService demoService;

    /**
     * @param demoService сервис демо-сценариев DTT
     */
    public DttController(DttDemoService demoService) {
        this.demoService = demoService;
    }

    /**
     * Валидирует DTT-архив, включая синтаксис Groovy-скриптов.
     *
     * @param archiveBytes бинарный DTT-архив
     * @return результат валидации
     */
    @Post(uri = "/validate", consumes = MediaType.APPLICATION_OCTET_STREAM, produces = MediaType.APPLICATION_JSON)
    @Operation(summary = "Валидировать DTT")
    @ApiResponse(responseCode = "200", description = "Результат валидации")
    public DttValidationResponse validate(@Body byte[] archiveBytes) {
        return demoService.validate(archiveBytes);
    }

    /**
     * Возвращает краткую инспекцию DTT-архива.
     *
     * @param archiveBytes бинарный DTT-архив
     * @return сводка по шаблону
     */
    @Post(uri = "/inspect", consumes = MediaType.APPLICATION_OCTET_STREAM, produces = MediaType.APPLICATION_JSON)
    @Operation(summary = "Инспектировать DTT")
    @ApiResponse(responseCode = "200", description = "Краткая информация о DTT")
    public DttInspectionResponse inspect(@Body byte[] archiveBytes) {
        return demoService.inspect(archiveBytes);
    }

    /**
     * Импортирует один или несколько DTT-архивов в JSON профиля оборудования.
     *
     * @param request запрос с Base64-представлением архивов и merge-стратегией
     * @return JSON профиля и количество типов устройств
     */
    @Post(uri = "/import/profile", consumes = MediaType.APPLICATION_JSON, produces = MediaType.APPLICATION_JSON)
    @Operation(summary = "Импортировать набор DTT в profile JSON")
    @ApiResponse(
            responseCode = "200",
            description = "Собранный профиль оборудования",
            content = @Content(examples = @ExampleObject(value = "{\"deviceTypesCount\":1,\"profileJson\":\"{...}\"}"))
    )
    @ApiResponse(
            responseCode = "400",
            description = "Ошибка валидации входных данных",
            content = @Content(examples = @ExampleObject(value = "{\"code\":\"BAD_REQUEST\",\"message\":\"Invalid Base64 archive at index 0\"}"))
    )
    public ImportDttSetToProfileResponse importToProfile(@Body ImportDttSetToProfileRequest request) {
        final List<byte[]> archives = decodeArchives(request);
        return demoService.importDttSetToProfile(archives, request.mergeStrategy());
    }


    /**
     * Экспортирует все DTT-архивы из profile JSON.
     *
     * @param request запрос с profile JSON
     * @return карта Base64-архивов по deviceTypeId
     */
    @Post(uri = "/export/profile/all", consumes = MediaType.APPLICATION_JSON, produces = MediaType.APPLICATION_JSON)
    @Operation(summary = "Экспортировать все DTT из profile JSON")
    @ApiResponse(responseCode = "200", description = "Набор экспортированных DTT")
    @ApiResponse(
            responseCode = "400",
            description = "Ошибка валидации входных данных",
            content = @Content(examples = @ExampleObject(value = "{\"code\":\"BAD_REQUEST\",\"message\":\"Ошибка парсинга profile JSON\"}"))
    )
    public ExportAllDttFromProfileResponse exportAllFromProfile(@Body ExportAllDttFromProfileRequest request) {
        if (request == null || request.profileJson() == null || request.profileJson().isBlank()) {
            throw new IllegalArgumentException("profileJson must not be blank");
        }
        return demoService.exportAllDttFromProfile(request.profileJson());
    }

    /**
     * Возвращает структурированную ошибку для нарушений входной валидации.
     *
     * @param exception исключение входных данных
     * @return JSON ошибки с кодом и сообщением
     */
    @Error(exception = IllegalArgumentException.class)
    public HttpResponse<DemoErrorResponse> handleBadRequest(IllegalArgumentException exception) {
        return HttpResponse.badRequest(new DemoErrorResponse("BAD_REQUEST", exception.getMessage()));
    }


    /**
     * Возвращает структурированную ошибку для проблем формата входного JSON.
     *
     * @param exception исключение формата
     * @return JSON ошибки с кодом и сообщением
     */
    @Error(exception = DttFormatException.class)
    public HttpResponse<DemoErrorResponse> handleFormatError(DttFormatException exception) {
        return HttpResponse.badRequest(new DemoErrorResponse("BAD_REQUEST", exception.getMessage()));
    }

    private List<byte[]> decodeArchives(ImportDttSetToProfileRequest request) {
        if (request == null || request.archivesBase64() == null || request.archivesBase64().isEmpty()) {
            throw new IllegalArgumentException("archivesBase64 must contain at least one DTT archive");
        }
        final List<byte[]> decoded = new ArrayList<>(request.archivesBase64().size());
        for (int index = 0; index < request.archivesBase64().size(); index++) {
            try {
                decoded.add(Base64.getDecoder().decode(request.archivesBase64().get(index)));
            } catch (IllegalArgumentException ex) {
                throw new IllegalArgumentException("Invalid Base64 archive at index " + index, ex);
            }
        }
        return decoded;
    }
}
