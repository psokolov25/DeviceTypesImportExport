package ru.aritmos.dtt.demo.controller;

import io.micronaut.http.MediaType;
import io.micronaut.http.annotation.Body;
import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.Post;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import ru.aritmos.dtt.demo.dto.DttInspectionResponse;
import ru.aritmos.dtt.demo.dto.DttValidationResponse;
import ru.aritmos.dtt.demo.service.DttDemoService;

/**
 * Demo API для валидации и инспекции одного DTT-архива.
 */
@Controller("/api/dtt")
@Tag(name = "DTT")
public class DttController {

    private final DttDemoService demoService = new DttDemoService();

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
}
