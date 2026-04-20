package ru.aritmos.dtt.demo.controller;

import io.micronaut.http.HttpResponse;
import io.micronaut.http.MediaType;
import io.micronaut.http.annotation.Body;
import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.Error;
import io.micronaut.http.annotation.Post;
import io.micronaut.http.annotation.QueryValue;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import ru.aritmos.dtt.api.dto.MergeStrategy;
import ru.aritmos.dtt.archive.DttFileNames;
import ru.aritmos.dtt.demo.dto.DemoErrorResponse;
import ru.aritmos.dtt.demo.dto.DttInspectionResponse;
import ru.aritmos.dtt.demo.dto.DttValidationResponse;
import ru.aritmos.dtt.demo.dto.ExportAllDttFromBranchRequest;
import ru.aritmos.dtt.demo.dto.ExportAllDttFromBranchResponse;
import ru.aritmos.dtt.demo.dto.ExportAllDttFromProfileRequest;
import ru.aritmos.dtt.demo.dto.ExportAllDttFromProfileResponse;
import ru.aritmos.dtt.demo.dto.ExportSingleDttFromBranchRequest;
import ru.aritmos.dtt.demo.dto.ExportSingleDttFromProfileRequest;
import ru.aritmos.dtt.demo.dto.ExportSingleDttResponse;
import ru.aritmos.dtt.demo.dto.ImportDttSetToBranchRequest;
import ru.aritmos.dtt.demo.dto.ImportDttSetToBranchResponse;
import ru.aritmos.dtt.demo.dto.ImportDttSetToExistingBranchRequest;
import ru.aritmos.dtt.demo.dto.ImportDttSetToProfileRequest;
import ru.aritmos.dtt.demo.dto.ImportDttSetToProfileResponse;
import ru.aritmos.dtt.demo.dto.SingleDttExportPreviewResponse;
import ru.aritmos.dtt.demo.openapi.DttSwaggerExamples;
import ru.aritmos.dtt.demo.service.DttDemoService;
import ru.aritmos.dtt.exception.DttFormatException;
import ru.aritmos.dtt.exception.TemplateAssemblyException;
import ru.aritmos.dtt.exception.TemplateExportException;
import ru.aritmos.dtt.exception.TemplateImportException;
import ru.aritmos.dtt.exception.TemplateValidationException;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
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
    @RequestBody(
            required = true,
            content = @Content(examples = @ExampleObject(value = """
                    {
                      "archivesBase64": [
                        "UEsDB..."
                      ],
                      "mergeStrategy": "FAIL_IF_EXISTS"
                    }
                    """))
    )
    @ApiResponse(
            responseCode = "200",
            description = "Собранный профиль оборудования",
            content = @Content(examples = @ExampleObject(value = "{\"deviceTypesCount\":1,\"profileJson\":\"{...}\"}"))
    )
    @ApiResponse(
            responseCode = "400",
            description = "Ошибка валидации входных данных",
            content = @Content(
                    schema = @Schema(implementation = DemoErrorResponse.class),
                    examples = @ExampleObject(value = "{\"code\":\"BAD_REQUEST\",\"message\":\"Invalid Base64 DTT archive payload at index 0\"}")
            )
    )
    public ImportDttSetToProfileResponse importToProfile(@Body ImportDttSetToProfileRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("request must not be null");
        }
        if (request.deviceTypes() != null && !request.deviceTypes().isEmpty()) {
            return demoService.importDttSetToProfile(request);
        }
        return demoService.importDttSetToProfileBase64(request.archivesBase64(), request.mergeStrategy());
    }

    /**
     * Выполняет preview-сборку profile JSON из одного или нескольких DTT-архивов.
     *
     * @param request запрос с Base64-представлением архивов и merge-стратегией
     * @return preview profile JSON и количество типов устройств
     */
    @Post(uri = "/preview/profile", consumes = MediaType.APPLICATION_JSON, produces = MediaType.APPLICATION_JSON)
    @Operation(summary = "Preview сборки profile JSON из набора DTT")
    @RequestBody(
            required = true,
            content = @Content(examples = @ExampleObject(value = """
                    {
                      "archivesBase64": [
                        "UEsDB..."
                      ],
                      "mergeStrategy": "MERGE_PRESERVE_EXISTING"
                    }
                    """))
    )
    @ApiResponse(responseCode = "200", description = "Preview profile JSON")
    @ApiResponse(
            responseCode = "400",
            description = "Ошибка валидации входных данных",
            content = @Content(
                    schema = @Schema(implementation = DemoErrorResponse.class),
                    examples = @ExampleObject(value = "{\"code\":\"BAD_REQUEST\",\"message\":\"Invalid Base64 DTT archive payload at index 0\"}")
            )
    )
    public ImportDttSetToProfileResponse previewProfile(@Body ImportDttSetToProfileRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("request must not be null");
        }
        if (request.deviceTypes() != null && !request.deviceTypes().isEmpty()) {
            return demoService.previewDttSetToProfile(request);
        }
        return demoService.previewDttSetToProfileBase64(request.archivesBase64(), request.mergeStrategy());
    }

    /**
     * Импортирует zip-архив с файлами .dtt в profile JSON (режим upload-download).
     *
     * @param zipPayload zip-архив с файлами .dtt
     * @param mergeStrategy стратегия merge
     * @return profile JSON и количество типов устройств
     */
    @Post(uri = "/import/profile/upload{?mergeStrategy}", consumes = MediaType.APPLICATION_OCTET_STREAM, produces = MediaType.APPLICATION_JSON)
    @Operation(summary = "Импортировать zip DTT в profile JSON (upload-download)")
    public ImportDttSetToProfileResponse importToProfileUpload(@Body byte[] zipPayload,
                                                               @QueryValue(defaultValue = "FAIL_IF_EXISTS") MergeStrategy mergeStrategy) {
        return demoService.importDttZipToProfile(zipPayload, mergeStrategy);
    }

    /**
     * Выполняет preview-сборку profile JSON из zip-архива с файлами .dtt (режим upload-download).
     *
     * @param zipPayload zip-архив с файлами .dtt
     * @param mergeStrategy стратегия merge
     * @return preview profile JSON и количество типов устройств
     */
    @Post(uri = "/preview/profile/upload{?mergeStrategy}", consumes = MediaType.APPLICATION_OCTET_STREAM, produces = MediaType.APPLICATION_JSON)
    @Operation(summary = "Preview profile JSON из zip DTT (upload-download)")
    public ImportDttSetToProfileResponse previewProfileUpload(@Body byte[] zipPayload,
                                                              @QueryValue(defaultValue = "FAIL_IF_EXISTS") MergeStrategy mergeStrategy) {
        return demoService.previewDttZipToProfile(zipPayload, mergeStrategy);
    }

    /**
     * Импортирует один или несколько DTT-архивов в branch equipment JSON.
     *
     * @param request запрос с Base64-архивами, branch и merge-стратегией
     * @return branch JSON и количество branch
     */
    @Post(uri = "/import/branch", consumes = MediaType.APPLICATION_JSON, produces = MediaType.APPLICATION_JSON)
    @Operation(summary = "Импортировать набор DTT в branch equipment JSON")
    @RequestBody(
            required = true,
            content = @Content(examples = {
                    @ExampleObject(name = "mergeNonNulls", value = DttSwaggerExamples.IMPORT_BRANCH_REQUEST_MERGE_NON_NULLS),
                    @ExampleObject(name = "createCopyWithSuffix", value = DttSwaggerExamples.IMPORT_BRANCH_REQUEST_CREATE_COPY)
            })
    )
    @ApiResponse(
            responseCode = "200",
            description = "Собранное оборудование отделений",
            content = @Content(examples = @ExampleObject(value = DttSwaggerExamples.IMPORT_BRANCH_RESPONSE_EXAMPLE))
    )
    @ApiResponse(
            responseCode = "400",
            description = "Ошибка входных данных",
            content = @Content(
                    schema = @Schema(implementation = DemoErrorResponse.class),
                    examples = @ExampleObject(value = "{\"code\":\"BAD_REQUEST\",\"message\":\"branchIds must contain at least one branch id\"}")
            )
    )
    public ImportDttSetToBranchResponse importToBranch(@Body ImportDttSetToBranchRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("request must not be null");
        }
        if (request.branches() != null && !request.branches().isEmpty()) {
            return demoService.importDttSetToBranch(request);
        }
        if (request.branchIds() == null || request.branchIds().isEmpty()) {
            throw new IllegalArgumentException("branchIds must contain at least one branch id");
        }
        return demoService.importDttSetToBranchBase64(request.archivesBase64(), request.branchIds(), request.mergeStrategy());
    }

    /**
     * Импортирует один или несколько DTT-архивов в уже существующий branch equipment JSON.
     *
     * @param request запрос с исходным branch JSON, Base64-архивами, branch и merge-стратегией
     * @return branch JSON после merge-импорта и количество branch
     */
    @Post(uri = "/import/branch/merge", consumes = MediaType.APPLICATION_JSON, produces = MediaType.APPLICATION_JSON)
    @Operation(summary = "Импортировать набор DTT в существующий branch equipment JSON")
    @RequestBody(
            required = true,
            content = @Content(examples = @ExampleObject(value = DttSwaggerExamples.IMPORT_BRANCH_MERGE_REQUEST_EXAMPLE))
    )
    @ApiResponse(
            responseCode = "200",
            description = "Обновлённое оборудование отделений",
            content = @Content(examples = @ExampleObject(value = DttSwaggerExamples.IMPORT_BRANCH_RESPONSE_EXAMPLE))
    )
    @ApiResponse(
            responseCode = "400",
            description = "Ошибка входных данных",
            content = @Content(
                    schema = @Schema(implementation = DemoErrorResponse.class),
                    examples = @ExampleObject(value = "{\"code\":\"BAD_REQUEST\",\"message\":\"existingBranchJson must not be blank\"}")
            )
    )
    public ImportDttSetToBranchResponse importToExistingBranch(@Body ImportDttSetToExistingBranchRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("request must not be null");
        }
        if (request.existingBranchJson() == null || request.existingBranchJson().isBlank()) {
            throw new IllegalArgumentException("existingBranchJson must not be blank");
        }
        if (request.branches() != null && !request.branches().isEmpty()) {
            return demoService.importDttSetToExistingBranch(request);
        }
        if (request.branchIds() == null || request.branchIds().isEmpty()) {
            throw new IllegalArgumentException("branchIds must contain at least one branch id");
        }
        return demoService.importDttSetToExistingBranchBase64(
                request.archivesBase64(),
                request.existingBranchJson(),
                request.branchIds(),
                request.mergeStrategy()
        );
    }

    /**
     * Выполняет preview-сборку branch equipment JSON из одного или нескольких DTT-архивов.
     *
     * @param request запрос с Base64-архивами, branch и merge-стратегией
     * @return preview branch equipment JSON и количество branch
     */
    @Post(uri = "/preview/branch", consumes = MediaType.APPLICATION_JSON, produces = MediaType.APPLICATION_JSON)
    @Operation(summary = "Preview сборки branch equipment JSON из набора DTT")
    @RequestBody(
            required = true,
            content = @Content(examples = {
                    @ExampleObject(name = "mergeNonNulls", value = DttSwaggerExamples.IMPORT_BRANCH_REQUEST_MERGE_NON_NULLS),
                    @ExampleObject(name = "createCopyWithSuffix", value = DttSwaggerExamples.IMPORT_BRANCH_REQUEST_CREATE_COPY)
            })
    )
    @ApiResponse(
            responseCode = "200",
            description = "Preview branch equipment JSON",
            content = @Content(examples = @ExampleObject(value = DttSwaggerExamples.IMPORT_BRANCH_RESPONSE_EXAMPLE))
    )
    @ApiResponse(
            responseCode = "400",
            description = "Ошибка входных данных",
            content = @Content(
                    schema = @Schema(implementation = DemoErrorResponse.class),
                    examples = @ExampleObject(value = "{\"code\":\"BAD_REQUEST\",\"message\":\"branchIds must contain at least one branch id\"}")
            )
    )
    public ImportDttSetToBranchResponse previewBranch(@Body ImportDttSetToBranchRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("request must not be null");
        }
        if (request.branches() != null && !request.branches().isEmpty()) {
            return demoService.previewDttSetToBranch(request);
        }
        if (request.branchIds() == null || request.branchIds().isEmpty()) {
            throw new IllegalArgumentException("branchIds must contain at least one branch id");
        }
        return demoService.previewDttSetToBranchBase64(request.archivesBase64(), request.branchIds(), request.mergeStrategy());
    }

    /**
     * Импортирует zip-архив с файлами .dtt в branch equipment JSON (режим upload-download).
     *
     * @param zipPayload zip-архив с файлами .dtt
     * @param branchIds branch-ы назначения
     * @param mergeStrategy стратегия merge
     * @return branch JSON и количество branch
     */
    @Post(uri = "/import/branch/upload{?branchIds,mergeStrategy}", consumes = MediaType.APPLICATION_OCTET_STREAM, produces = MediaType.APPLICATION_JSON)
    @Operation(summary = "Импортировать zip DTT в branch equipment JSON (upload-download)")
    public ImportDttSetToBranchResponse importToBranchUpload(@Body byte[] zipPayload,
                                                             @QueryValue List<String> branchIds,
                                                             @QueryValue(defaultValue = "FAIL_IF_EXISTS") MergeStrategy mergeStrategy) {
        if (branchIds == null || branchIds.isEmpty()) {
            throw new IllegalArgumentException("branchIds must contain at least one branch id");
        }
        return demoService.importDttZipToBranch(zipPayload, branchIds, mergeStrategy);
    }

    /**
     * Выполняет preview-сборку branch equipment JSON из zip-архива с файлами .dtt (режим upload-download).
     *
     * @param zipPayload zip-архив с файлами .dtt
     * @param branchIds branch-ы назначения
     * @param mergeStrategy стратегия merge
     * @return preview branch JSON и количество branch
     */
    @Post(uri = "/preview/branch/upload{?branchIds,mergeStrategy}", consumes = MediaType.APPLICATION_OCTET_STREAM, produces = MediaType.APPLICATION_JSON)
    @Operation(summary = "Preview branch equipment JSON из zip DTT (upload-download)")
    public ImportDttSetToBranchResponse previewBranchUpload(@Body byte[] zipPayload,
                                                            @QueryValue List<String> branchIds,
                                                            @QueryValue(defaultValue = "FAIL_IF_EXISTS") MergeStrategy mergeStrategy) {
        if (branchIds == null || branchIds.isEmpty()) {
            throw new IllegalArgumentException("branchIds must contain at least one branch id");
        }
        return demoService.previewDttZipToBranch(zipPayload, branchIds, mergeStrategy);
    }


    /**
     * Экспортирует один DTT-архив из profile JSON.
     *
     * @param request запрос с profile JSON и идентификатором типа устройства
     * @return экспортированный DTT-архив в Base64
     */
    @Post(uri = "/export/profile/one", consumes = MediaType.APPLICATION_JSON, produces = MediaType.APPLICATION_JSON)
    @Operation(summary = "Экспортировать один DTT из profile JSON")
    @ApiResponse(responseCode = "200", description = "Экспортированный DTT")
    @ApiResponse(
            responseCode = "400",
            description = "Ошибка валидации входных данных",
            content = @Content(
                    schema = @Schema(implementation = DemoErrorResponse.class),
                    examples = @ExampleObject(value = "{\"code\":\"BAD_REQUEST\",\"message\":\"deviceTypeId must not be blank\"}")
            )
    )
    public ExportSingleDttResponse exportSingleFromProfile(@Body ExportSingleDttFromProfileRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("request must not be null");
        }
        if (request.deviceTypeId() == null || request.deviceTypeId().isBlank()) {
            throw new IllegalArgumentException("deviceTypeId must not be blank");
        }
        if (request.profile() != null) {
            return demoService.exportSingleDttFromProfile(request.profile(), request.deviceTypeId(), request.dttVersion());
        }
        if (request.profileJson() != null && !request.profileJson().isBlank()) {
            return demoService.exportSingleDttFromProfileJson(request.profileJson(), request.deviceTypeId(), request.dttVersion());
        }
        throw new IllegalArgumentException("Either profile or profileJson must be provided");
    }

    /**
     * Выполняет preview single-export из profile JSON с диагностикой конфликтов.
     *
     * @param request запрос с profile JSON и идентификатором типа устройства
     * @return результат preview с размером архива и/или диагностическими проблемами
     */
    @Post(uri = "/preview/export/profile/one", consumes = MediaType.APPLICATION_JSON, produces = MediaType.APPLICATION_JSON)
    @Operation(summary = "Preview single-export DTT из profile JSON")
    @ApiResponse(responseCode = "200", description = "Preview результата single-export")
    @ApiResponse(
            responseCode = "400",
            description = "Ошибка валидации входных данных",
            content = @Content(
                    schema = @Schema(implementation = DemoErrorResponse.class),
                    examples = @ExampleObject(value = "{\"code\":\"BAD_REQUEST\",\"message\":\"deviceTypeId must not be blank\"}")
            )
    )
    public SingleDttExportPreviewResponse previewSingleExportFromProfile(@Body ExportSingleDttFromProfileRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("request must not be null");
        }
        if (request.deviceTypeId() == null || request.deviceTypeId().isBlank()) {
            throw new IllegalArgumentException("deviceTypeId must not be blank");
        }
        if (request.profile() != null) {
            return demoService.previewSingleDttExportFromProfile(request.profile(), request.deviceTypeId(), request.dttVersion());
        }
        if (request.profileJson() != null && !request.profileJson().isBlank()) {
            return demoService.previewSingleDttExportFromProfileJson(request.profileJson(), request.deviceTypeId(), request.dttVersion());
        }
        throw new IllegalArgumentException("Either profile or profileJson must be provided");
    }

    /**
     * Экспортирует один DTT-архив из profile JSON в бинарном виде (upload-download режим).
     *
     * @param request запрос с profile JSON и идентификатором типа устройства
     * @return бинарный payload одного файла .dtt
     */
    @Post(uri = "/export/profile/one/download", consumes = MediaType.APPLICATION_JSON, produces = MediaType.APPLICATION_OCTET_STREAM)
    @Operation(summary = "Экспортировать один DTT из profile JSON в download-режиме")
    public HttpResponse<byte[]> exportSingleFromProfileDownload(@Body ExportSingleDttFromProfileRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("request must not be null");
        }
        if (request.deviceTypeId() == null || request.deviceTypeId().isBlank()) {
            throw new IllegalArgumentException("deviceTypeId must not be blank");
        }
        final byte[] payload;
        if (request.profile() != null) {
            payload = demoService.exportSingleDttFromProfileToBytes(request.profile(), request.deviceTypeId(), request.dttVersion());
        } else if (request.profileJson() != null && !request.profileJson().isBlank()) {
            payload = demoService.exportSingleDttFromProfileJsonToBytes(request.profileJson(), request.deviceTypeId(), request.dttVersion());
        } else {
            throw new IllegalArgumentException("Either profile or profileJson must be provided");
        }
        return HttpResponse.ok(payload)
                .header("Content-Disposition", buildDttContentDisposition(payload, request.deviceTypeId()));
    }

    /**
     * Экспортирует все DTT-архивы из profile JSON.
     *
     * @param request запрос с profile JSON
     * @return карта Base64-архивов по deviceTypeId
     */
    @Post(uri = "/export/profile/all", consumes = MediaType.APPLICATION_JSON, produces = MediaType.APPLICATION_JSON)
    @Operation(summary = "Экспортировать все DTT из profile JSON")
    @RequestBody(
            required = true,
            content = @Content(examples = {
                    @ExampleObject(name = "objectModel", value = """
                            {
                              "profile": {
                                "deviceTypes": {
                                  "ed650d7d-6201-42fb-a4c3-b9efb93dda0c": {
                                    "metadata": {
                                      "id": "ed650d7d-6201-42fb-a4c3-b9efb93dda0c",
                                      "name": "Terminal",
                                      "displayName": "Терминал (Киоск)",
                                      "description": "Терминал (Киоск)"
                                    },
                                    "deviceTypeParamValues": {
                                      "printerServiceURL": "http://192.168.7.20:8084",
                                      "prefix": "SSS"
                                    }
                                  }
                                }
                              }
                            }
                            """),
                    @ExampleObject(name = "jsonString", value = """
                            {
                              "profileJson": "{\\"ed650d7d-6201-42fb-a4c3-b9efb93dda0c\\":{\\"metadata\\":{\\"id\\":\\"ed650d7d-6201-42fb-a4c3-b9efb93dda0c\\",\\"name\\":\\"Terminal\\",\\"displayName\\":\\"Терминал (Киоск)\\",\\"description\\":\\"Терминал (Киоск)\\"},\\"deviceTypeParamValues\\":{\\"prefix\\":\\"SSS\\"}}}",
                              "deviceTypeIds": [
                                "ed650d7d-6201-42fb-a4c3-b9efb93dda0c"
                              ]
                            }
                            """)
            })
    )
    @ApiResponse(responseCode = "200", description = "Набор экспортированных DTT")
    @ApiResponse(
            responseCode = "400",
            description = "Ошибка валидации входных данных",
            content = @Content(
                    schema = @Schema(implementation = DemoErrorResponse.class),
                    examples = @ExampleObject(value = "{\"code\":\"BAD_REQUEST\",\"message\":\"Either profile or profileJson must be provided\"}")
            )
    )
    public ExportAllDttFromProfileResponse exportAllFromProfile(@Body ExportAllDttFromProfileRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("request must not be null");
        }
        if (request.profile() != null) {
            return demoService.exportAllDttFromProfile(request.profile(), request.deviceTypeIds(), request.dttVersion());
        }
        if (request.profileJson() != null && !request.profileJson().isBlank()) {
            return demoService.exportAllDttFromProfileJson(request.profileJson(), request.deviceTypeIds(), request.dttVersion());
        }
        throw new IllegalArgumentException("Either profile or profileJson must be provided");
    }

    /**
     * Экспортирует набор DTT из profile JSON в zip-архив (режим upload-download).
     */
    @Post(uri = "/export/profile/all/download", consumes = MediaType.APPLICATION_JSON, produces = MediaType.APPLICATION_OCTET_STREAM)
    @Operation(summary = "Экспортировать все DTT из profile JSON в zip (upload-download)")
    public HttpResponse<byte[]> exportAllFromProfileDownload(@Body ExportAllDttFromProfileRequest request) {
        if (request == null || (request.profile() == null && (request.profileJson() == null || request.profileJson().isBlank()))) {
            throw new IllegalArgumentException("Either profile or profileJson must be provided");
        }
        final byte[] payload = demoService.exportProfileToZip(
                request.profile(),
                request.profileJson(),
                request.deviceTypeIds(),
                request.dttVersion()
        );
        return HttpResponse.ok(payload)
                .header("Content-Disposition", "attachment; filename=\"profile-dtt-set.zip\"");
    }

    /**
     * Экспортирует один DTT-архив из branch equipment JSON.
     *
     * @param request запрос с branch equipment JSON и идентификатором типа устройства
     * @return экспортированный DTT-архив в Base64
     */
    @Post(uri = "/export/branch/one", consumes = MediaType.APPLICATION_JSON, produces = MediaType.APPLICATION_JSON)
    @Operation(summary = "Экспортировать один DTT из branch equipment JSON")
    @ApiResponse(responseCode = "200", description = "Экспортированный DTT")
    @ApiResponse(
            responseCode = "400",
            description = "Ошибка валидации входных данных",
            content = @Content(
                    schema = @Schema(implementation = DemoErrorResponse.class),
                    examples = @ExampleObject(value = "{\"code\":\"BAD_REQUEST\",\"message\":\"deviceTypeId must not be blank\"}")
            )
    )
    public ExportSingleDttResponse exportSingleFromBranch(@Body ExportSingleDttFromBranchRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("request must not be null");
        }
        if (request.deviceTypeId() == null || request.deviceTypeId().isBlank()) {
            throw new IllegalArgumentException("deviceTypeId must not be blank");
        }
        if (request.branchEquipment() != null) {
            return demoService.exportSingleDttFromBranch(
                    request.branchEquipment(),
                    request.branchIds(),
                    request.deviceTypeId(),
                    request.mergeStrategy(),
                    request.dttVersion()
            );
        }
        if (request.branchJson() != null && !request.branchJson().isBlank()) {
            return demoService.exportSingleDttFromBranchJson(
                    request.branchJson(),
                    request.branchIds(),
                    request.deviceTypeId(),
                    request.mergeStrategy(),
                    request.dttVersion()
            );
        }
        throw new IllegalArgumentException("Either branchEquipment or branchJson must be provided");
    }

    /**
     * Выполняет preview single-export из branch equipment JSON с диагностикой merge-конфликтов.
     *
     * @param request запрос с branch equipment JSON и идентификатором типа устройства
     * @return результат preview с размером архива и/или диагностическими проблемами
     */
    @Post(uri = "/preview/export/branch/one", consumes = MediaType.APPLICATION_JSON, produces = MediaType.APPLICATION_JSON)
    @Operation(summary = "Preview single-export DTT из branch equipment JSON")
    @ApiResponse(responseCode = "200", description = "Preview результата single-export")
    @ApiResponse(
            responseCode = "400",
            description = "Ошибка валидации входных данных",
            content = @Content(
                    schema = @Schema(implementation = DemoErrorResponse.class),
                    examples = @ExampleObject(value = "{\"code\":\"BAD_REQUEST\",\"message\":\"deviceTypeId must not be blank\"}")
            )
    )
    public SingleDttExportPreviewResponse previewSingleExportFromBranch(@Body ExportSingleDttFromBranchRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("request must not be null");
        }
        if (request.deviceTypeId() == null || request.deviceTypeId().isBlank()) {
            throw new IllegalArgumentException("deviceTypeId must not be blank");
        }
        if (request.branchEquipment() != null) {
            return demoService.previewSingleDttExportFromBranch(
                    request.branchEquipment(),
                    request.branchIds(),
                    request.deviceTypeId(),
                    request.mergeStrategy(),
                    request.dttVersion()
            );
        }
        if (request.branchJson() != null && !request.branchJson().isBlank()) {
            return demoService.previewSingleDttExportFromBranchJson(
                    request.branchJson(),
                    request.branchIds(),
                    request.deviceTypeId(),
                    request.mergeStrategy(),
                    request.dttVersion()
            );
        }
        throw new IllegalArgumentException("Either branchEquipment or branchJson must be provided");
    }

    /**
     * Экспортирует один DTT-архив из branch equipment JSON в бинарном виде (upload-download режим).
     *
     * @param request запрос с branch equipment JSON и идентификатором типа устройства
     * @return бинарный payload одного файла .dtt
     */
    @Post(uri = "/export/branch/one/download", consumes = MediaType.APPLICATION_JSON, produces = MediaType.APPLICATION_OCTET_STREAM)
    @Operation(summary = "Экспортировать один DTT из branch equipment JSON в download-режиме")
    public HttpResponse<byte[]> exportSingleFromBranchDownload(@Body ExportSingleDttFromBranchRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("request must not be null");
        }
        if (request.deviceTypeId() == null || request.deviceTypeId().isBlank()) {
            throw new IllegalArgumentException("deviceTypeId must not be blank");
        }
        final byte[] payload;
        if (request.branchEquipment() != null) {
            payload = demoService.exportSingleDttFromBranchToBytes(
                    request.branchEquipment(),
                    request.branchIds(),
                    request.deviceTypeId(),
                    request.mergeStrategy(),
                    request.dttVersion()
            );
        } else if (request.branchJson() != null && !request.branchJson().isBlank()) {
            payload = demoService.exportSingleDttFromBranchJsonToBytes(
                    request.branchJson(),
                    request.branchIds(),
                    request.deviceTypeId(),
                    request.mergeStrategy(),
                    request.dttVersion()
            );
        } else {
            throw new IllegalArgumentException("Either branchEquipment or branchJson must be provided");
        }
        return HttpResponse.ok(payload)
                .header("Content-Disposition", buildDttContentDisposition(payload, request.deviceTypeId()));
    }

    /**
     * Экспортирует все DTT-архивы из branch equipment JSON.
     *
     * @param request запрос с branch JSON и merge-стратегией
     * @return карта Base64-архивов по deviceTypeId
     */
    @Post(uri = "/export/branch/all", consumes = MediaType.APPLICATION_JSON, produces = MediaType.APPLICATION_JSON)
    @Operation(summary = "Экспортировать все DTT из branch equipment JSON")
    @RequestBody(
            required = true,
            content = @Content(examples = {
                    @ExampleObject(name = "autoResolveMostComplete", value = DttSwaggerExamples.BRANCH_EXPORT_OBJECT_AUTO_RESOLVE),
                    @ExampleObject(name = "failIfExists", value = DttSwaggerExamples.BRANCH_EXPORT_OBJECT_FAIL_IF_EXISTS),
                    @ExampleObject(name = "jsonStringFiltered", value = DttSwaggerExamples.BRANCH_EXPORT_JSON_STRING_FILTERED)
            })
    )
    @ApiResponse(responseCode = "200", description = "Набор экспортированных DTT")
    @ApiResponse(
            responseCode = "400",
            description = "Ошибка валидации входных данных",
            content = @Content(
                    schema = @Schema(implementation = DemoErrorResponse.class),
                    examples = @ExampleObject(value = "{\"code\":\"BAD_REQUEST\",\"message\":\"Either branchEquipment or branchJson must be provided\"}")
            )
    )
    public ExportAllDttFromBranchResponse exportAllFromBranch(@Body ExportAllDttFromBranchRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("request must not be null");
        }
        if (request.branchEquipment() != null) {
            return demoService.exportAllDttFromBranch(
                    request.branchEquipment(),
                    request.branchIds(),
                    request.deviceTypeIds(),
                    request.mergeStrategy(),
                    request.dttVersion()
            );
        }
        if (request.branchJson() != null && !request.branchJson().isBlank()) {
            return demoService.exportAllDttFromBranchJson(
                    request.branchJson(),
                    request.branchIds(),
                    request.deviceTypeIds(),
                    request.mergeStrategy(),
                    request.dttVersion()
            );
        }
        throw new IllegalArgumentException("Either branchEquipment or branchJson must be provided");
    }

    /**
     * Экспортирует набор DTT из branch equipment JSON в zip-архив (режим upload-download).
     */
    @Post(uri = "/export/branch/all/download", consumes = MediaType.APPLICATION_JSON, produces = MediaType.APPLICATION_OCTET_STREAM)
    @Operation(summary = "Экспортировать все DTT из branch equipment JSON в zip (upload-download)")
    @RequestBody(
            required = true,
            content = @Content(examples = {
                    @ExampleObject(name = "autoResolveMostComplete", value = DttSwaggerExamples.BRANCH_EXPORT_OBJECT_AUTO_RESOLVE),
                    @ExampleObject(name = "failIfExists", value = DttSwaggerExamples.BRANCH_EXPORT_OBJECT_FAIL_IF_EXISTS),
                    @ExampleObject(name = "jsonStringFiltered", value = DttSwaggerExamples.BRANCH_EXPORT_JSON_STRING_FILTERED)
            })
    )
    public HttpResponse<byte[]> exportAllFromBranchDownload(@Body ExportAllDttFromBranchRequest request) {
        if (request == null || (request.branchEquipment() == null && (request.branchJson() == null || request.branchJson().isBlank()))) {
            throw new IllegalArgumentException("Either branchEquipment or branchJson must be provided");
        }
        final byte[] payload = demoService.exportBranchToZip(
                request.branchEquipment(),
                request.branchJson(),
                request.branchIds(),
                request.deviceTypeIds(),
                request.mergeStrategy(),
                request.dttVersion()
        );
        return HttpResponse.ok(payload)
                .header("Content-Disposition", "attachment; filename=\"branch-dtt-set.zip\"");
    }

    private String buildDttContentDisposition(byte[] payload, String fallbackBaseName) {
        final String baseName = demoService.resolveDeviceTypeArchiveBaseName(payload, fallbackBaseName);
        final String unicodeBaseName = DttFileNames.sanitizeBaseName(baseName);
        final String asciiBaseName = DttFileNames.toAsciiFallbackBaseName(unicodeBaseName, fallbackBaseName);
        final String encodedFileName = URLEncoder.encode(unicodeBaseName + ".dtt", StandardCharsets.UTF_8)
                .replace("+", "%20");
        return "attachment; filename=\"" + asciiBaseName + ".dtt\"; filename*=UTF-8''" + encodedFileName;
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

    /**
     * Возвращает структурированную ошибку для проблем валидации шаблона.
     */
    @Error(exception = TemplateValidationException.class)
    public HttpResponse<DemoErrorResponse> handleValidationError(TemplateValidationException exception) {
        return HttpResponse.badRequest(new DemoErrorResponse("BAD_REQUEST", exception.getMessage()));
    }

    /**
     * Возвращает структурированную ошибку для проблем импорта шаблона.
     */
    @Error(exception = TemplateImportException.class)
    public HttpResponse<DemoErrorResponse> handleImportError(TemplateImportException exception) {
        return HttpResponse.badRequest(new DemoErrorResponse("BAD_REQUEST", exception.getMessage()));
    }

    /**
     * Возвращает структурированную ошибку для проблем экспорта шаблона.
     */
    @Error(exception = TemplateExportException.class)
    public HttpResponse<DemoErrorResponse> handleExportError(TemplateExportException exception) {
        return HttpResponse.badRequest(new DemoErrorResponse("BAD_REQUEST", exception.getMessage()));
    }

    /**
     * Возвращает структурированную ошибку для проблем merge/assembly сценариев.
     */
    @Error(exception = TemplateAssemblyException.class)
    public HttpResponse<DemoErrorResponse> handleAssemblyError(TemplateAssemblyException exception) {
        return HttpResponse.badRequest(new DemoErrorResponse("BAD_REQUEST", exception.getMessage()));
    }

    /**
     * Возвращает структурированную ошибку по умолчанию для неожиданных исключений.
     */
    @Error(exception = Throwable.class)
    public HttpResponse<DemoErrorResponse> handleUnexpectedError(Throwable exception) {
        return HttpResponse.serverError(new DemoErrorResponse("INTERNAL_ERROR", exception.getMessage()));
    }

}
