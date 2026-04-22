package ru.aritmos.dtt.demo.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.HttpStatus;
import io.micronaut.http.MediaType;
import io.micronaut.http.annotation.Body;
import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.Error;
import io.micronaut.http.annotation.Post;
import io.micronaut.http.annotation.Part;
import io.micronaut.http.annotation.QueryValue;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import ru.aritmos.dtt.api.dto.MergeStrategy;
import ru.aritmos.dtt.archive.DttFileNames;
import ru.aritmos.dtt.demo.dto.DemoErrorResponse;
import ru.aritmos.dtt.demo.dto.DemoErrorCode;
import ru.aritmos.dtt.demo.dto.DttInspectionResponse;
import ru.aritmos.dtt.demo.dto.DttMetadataBatchResponse;
import ru.aritmos.dtt.demo.dto.DttVersionComparisonResponse;
import ru.aritmos.dtt.demo.dto.ProfilePreviewDetailedResponse;
import ru.aritmos.dtt.demo.dto.BranchPreviewDetailedResponse;
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
import ru.aritmos.dtt.demo.dto.ImportDttZipToBranchUploadRequest;
import ru.aritmos.dtt.demo.dto.ImportDttZipToExistingBranchUploadRequest;
import ru.aritmos.dtt.demo.dto.ImportDttSetToExistingBranchRequest;
import ru.aritmos.dtt.demo.dto.ImportDttSetToProfileRequest;
import ru.aritmos.dtt.demo.dto.ImportDttSetToProfileResponse;
import ru.aritmos.dtt.demo.dto.ImportDttZipToProfileUploadRequest;
import ru.aritmos.dtt.demo.dto.ImportProfileBranchWithMetadataResponse;
import ru.aritmos.dtt.demo.dto.ImportProfileBranchWithMetadataRequest;
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
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Demo API для валидации, инспекции и импорта DTT-архивов.
 */
@Controller("/api/dtt")
@Tag(name = "DTT")
@ApiResponses({
        @ApiResponse(
                responseCode = "400",
                description = "Некорректные входные данные/формат",
                content = @Content(schema = @Schema(implementation = DemoErrorResponse.class))
        ),
        @ApiResponse(
                responseCode = "409",
                description = "Конфликт импорта/экспорта/сборки",
                content = @Content(schema = @Schema(implementation = DemoErrorResponse.class))
        ),
        @ApiResponse(
                responseCode = "422",
                description = "Ошибка валидации шаблона",
                content = @Content(schema = @Schema(implementation = DemoErrorResponse.class))
        ),
        @ApiResponse(
                responseCode = "500",
                description = "Внутренняя ошибка сервера",
                content = @Content(schema = @Schema(implementation = DemoErrorResponse.class))
        )
})
public class DttController {

    private final DttDemoService demoService;
    private final ObjectMapper objectMapper = new ObjectMapper();

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
     * Возвращает базовые метаданные одного DTT или zip-набора DTT.
     *
     * @param payload бинарный .dtt или zip-архив, содержащий один/несколько .dtt
     * @return список базовых метаданных типов устройств
     */
    @Post(uri = "/metadata", consumes = MediaType.APPLICATION_OCTET_STREAM, produces = MediaType.APPLICATION_JSON)
    @Operation(summary = "Извлечь базовые metadata из DTT или DTT-set zip")
    @ApiResponse(responseCode = "200", description = "Список metadata типов устройств")
    public DttMetadataBatchResponse metadata(@Body byte[] payload) {
        return demoService.extractMetadata(payload);
    }

    /**
     * Сравнивает введённую версию и версию, сохранённую в DTT.
     *
     * @param archiveBytes бинарный DTT-архив
     * @param inputVersion версия из входного параметра
     * @return результат сравнения версий
     */
    @Post(uri = "/version/compare{?inputVersion}", consumes = MediaType.APPLICATION_OCTET_STREAM, produces = MediaType.APPLICATION_JSON)
    @Operation(summary = "Сравнить введённую версию и версию из DTT")
    @ApiResponse(responseCode = "200", description = "Результат сравнения версий")
    public DttVersionComparisonResponse compareVersion(@Body byte[] archiveBytes,
                                                       @QueryValue(defaultValue = "1.0") String inputVersion) {
        return demoService.compareDttVersion(archiveBytes, inputVersion);
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
            content = @Content(examples = {
                    @ExampleObject(name = "legacyBase64Full", value = DttSwaggerExamples.IMPORT_PROFILE_REQUEST_BASE64_FULL),
                    @ExampleObject(name = "structuredWithOverrides", value = DttSwaggerExamples.IMPORT_PROFILE_REQUEST_STRUCTURED_FULL)
            })
    )
    @ApiResponse(
            responseCode = "200",
            description = "Собранный профиль оборудования",
            content = @Content(examples = @ExampleObject(value = DttSwaggerExamples.IMPORT_PROFILE_RESPONSE_EXAMPLE))
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
            content = @Content(examples = {
                    @ExampleObject(name = "legacyBase64Full", value = DttSwaggerExamples.IMPORT_PROFILE_REQUEST_BASE64_FULL),
                    @ExampleObject(name = "structuredWithOverrides", value = DttSwaggerExamples.IMPORT_PROFILE_REQUEST_STRUCTURED_FULL)
            })
    )
    @ApiResponse(responseCode = "200", description = "Preview profile JSON", content = @Content(examples = @ExampleObject(value = DttSwaggerExamples.IMPORT_PROFILE_RESPONSE_EXAMPLE)))
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

    @Post(uri = "/preview/profile/detailed", consumes = MediaType.APPLICATION_JSON, produces = MediaType.APPLICATION_JSON)
    @Operation(summary = "Детальный preview profile JSON с defaults/overrides")
    @ApiResponse(responseCode = "200", description = "Preview profile JSON с расчётом defaults/overrides")
    public ProfilePreviewDetailedResponse previewProfileDetailed(@Body ImportDttSetToProfileRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("request must not be null");
        }
        return demoService.previewProfileDetailed(request);
    }


    /**
     * Импортирует один/несколько DTT сразу в profile JSON и branch equipment JSON с наследованием metadata.
     */
    @Post(uri = "/import/profile-branch", consumes = MediaType.APPLICATION_JSON, produces = MediaType.APPLICATION_JSON)
    @Operation(summary = "Импортировать DTT сразу в profile и branch с metadata inheritance")
    @RequestBody(
            required = true,
            content = @Content(examples = @ExampleObject(name = "profileBranchWithMetadata", value = DttSwaggerExamples.IMPORT_PROFILE_BRANCH_REQUEST_EXAMPLE))
    )
    @ApiResponse(responseCode = "200", description = "Собранные profile JSON и branch equipment JSON")
    public ImportProfileBranchWithMetadataResponse importProfileBranchWithMetadata(@Body ImportProfileBranchWithMetadataRequest request) {
        return demoService.importProfileAndBranchWithMetadata(request);
    }

    /**
     * Импортирует zip-архив с файлами .dtt в profile JSON (режим upload-download).
     *
     * @param zipPayload zip-архив с файлами .dtt
     * @param mergeStrategy стратегия merge
     * @return profile JSON и количество типов устройств
     */
    @Post(uri = "/import/profile/upload{?mergeStrategy,metadataJson}", consumes = MediaType.APPLICATION_OCTET_STREAM, produces = MediaType.APPLICATION_JSON)
    @Operation(summary = "Импортировать zip DTT в profile JSON (upload-download)")
    public ImportDttSetToProfileResponse importToProfileUpload(@Body byte[] zipPayload,
                                                               @QueryValue(defaultValue = "FAIL_IF_EXISTS") MergeStrategy mergeStrategy,
                                                               @QueryValue(defaultValue = "") String metadataJson) {
        if (metadataJson != null && !metadataJson.isBlank()) {
            return demoService.importDttZipToProfile(zipPayload, parseProfileUploadMetadata(metadataJson));
        }
        return demoService.importDttZipToProfile(zipPayload, mergeStrategy);
    }

    /**
     * Выполняет preview-сборку profile JSON из zip-архива с файлами .dtt (режим upload-download).
     *
     * @param zipPayload zip-архив с файлами .dtt
     * @param mergeStrategy стратегия merge
     * @return preview profile JSON и количество типов устройств
     */
    @Post(uri = "/preview/profile/upload{?mergeStrategy,metadataJson}", consumes = MediaType.APPLICATION_OCTET_STREAM, produces = MediaType.APPLICATION_JSON)
    @Operation(summary = "Preview profile JSON из zip DTT (upload-download)")
    public ImportDttSetToProfileResponse previewProfileUpload(@Body byte[] zipPayload,
                                                              @QueryValue(defaultValue = "FAIL_IF_EXISTS") MergeStrategy mergeStrategy,
                                                              @QueryValue(defaultValue = "") String metadataJson) {
        if (metadataJson != null && !metadataJson.isBlank()) {
            return demoService.previewDttZipToProfile(zipPayload, parseProfileUploadMetadata(metadataJson));
        }
        return demoService.previewDttZipToProfile(zipPayload, mergeStrategy);
    }


    /**
     * Импортирует zip-архив с файлами .dtt в profile JSON через multipart/form-data.
     *
     * @param zipPayload zip-архив с .dtt файлами
     * @param metadataJson JSON-метаданные импорта profile upload
     * @return profile JSON и количество типов устройств
     */
    @Post(uri = "/import/profile/upload/multipart", consumes = MediaType.MULTIPART_FORM_DATA, produces = MediaType.APPLICATION_JSON)
    @Operation(summary = "Импортировать zip DTT в profile JSON через multipart/form-data")
    public ImportDttSetToProfileResponse importToProfileUploadMultipart(
            @Part("zipPayload") @Schema(type = "string", format = "binary", description = "ZIP-архив с .dtt файлами")
            byte[] zipPayload,
            @Part("metadataJson") @Schema(description = "JSON-метаданные импорта profile upload", example = DttSwaggerExamples.IMPORT_PROFILE_UPLOAD_METADATA_EXAMPLE)
            String metadataJson) {
        return demoService.importDttZipToProfile(zipPayload, parseProfileUploadMetadata(metadataJson));
    }

    /**
     * Выполняет preview-сборку profile JSON из zip-архива с файлами .dtt через multipart/form-data.
     *
     * @param zipPayload zip-архив с .dtt файлами
     * @param metadataJson JSON-метаданные preview-импорта
     * @return preview profile JSON и количество типов устройств
     */
    @Post(uri = "/preview/profile/upload/multipart", consumes = MediaType.MULTIPART_FORM_DATA, produces = MediaType.APPLICATION_JSON)
    @Operation(summary = "Preview profile JSON из zip DTT через multipart/form-data")
    public ImportDttSetToProfileResponse previewProfileUploadMultipart(
            @Part("zipPayload") @Schema(type = "string", format = "binary", description = "ZIP-архив с .dtt файлами")
            byte[] zipPayload,
            @Part("metadataJson") @Schema(description = "JSON-метаданные preview profile upload", example = DttSwaggerExamples.IMPORT_PROFILE_UPLOAD_METADATA_EXAMPLE)
            String metadataJson) {
        return demoService.previewDttZipToProfile(zipPayload, parseProfileUploadMetadata(metadataJson));
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
                    @ExampleObject(name = "mergeNonNullsLegacyBase64Full", value = DttSwaggerExamples.IMPORT_BRANCH_REQUEST_MERGE_NON_NULLS),
                    @ExampleObject(name = "createCopyWithSuffixLegacyBase64Full", value = DttSwaggerExamples.IMPORT_BRANCH_REQUEST_CREATE_COPY),
                    @ExampleObject(name = "structuredWithOverrides", value = DttSwaggerExamples.IMPORT_BRANCH_REQUEST_STRUCTURED_FULL)
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
                    @ExampleObject(name = "mergeNonNullsLegacyBase64Full", value = DttSwaggerExamples.IMPORT_BRANCH_REQUEST_MERGE_NON_NULLS),
                    @ExampleObject(name = "createCopyWithSuffixLegacyBase64Full", value = DttSwaggerExamples.IMPORT_BRANCH_REQUEST_CREATE_COPY),
                    @ExampleObject(name = "structuredWithOverrides", value = DttSwaggerExamples.IMPORT_BRANCH_REQUEST_STRUCTURED_FULL)
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

    @Post(uri = "/preview/branch/detailed", consumes = MediaType.APPLICATION_JSON, produces = MediaType.APPLICATION_JSON)
    @Operation(summary = "Детальный preview branch JSON с defaults/overrides")
    @ApiResponse(responseCode = "200", description = "Preview branch JSON с расчётом defaults/overrides")
    public BranchPreviewDetailedResponse previewBranchDetailed(@Body ImportDttSetToBranchRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("request must not be null");
        }
        return demoService.previewBranchDetailed(request);
    }

    /**
     * Импортирует zip-архив с файлами .dtt в branch equipment JSON (режим upload-download).
     *
     * @param zipPayload zip-архив с файлами .dtt
     * @param branchIds branch-ы назначения
     * @param mergeStrategy стратегия merge
     * @return branch JSON и количество branch
     */
    @Post(uri = "/import/branch/upload{?branchIds,mergeStrategy,metadataJson}", consumes = MediaType.APPLICATION_OCTET_STREAM, produces = MediaType.APPLICATION_JSON)
    @Operation(summary = "Импортировать zip DTT в branch equipment JSON (upload-download)")
    public ImportDttSetToBranchResponse importToBranchUpload(@Body byte[] zipPayload,
                                                             @QueryValue List<String> branchIds,
                                                             @QueryValue(defaultValue = "FAIL_IF_EXISTS") MergeStrategy mergeStrategy,
                                                             @QueryValue(defaultValue = "") String metadataJson) {
        if (metadataJson != null && !metadataJson.isBlank()) {
            return demoService.importDttZipToBranch(zipPayload, parseBranchUploadMetadata(metadataJson));
        }
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
    @Post(uri = "/preview/branch/upload{?branchIds,mergeStrategy,metadataJson}", consumes = MediaType.APPLICATION_OCTET_STREAM, produces = MediaType.APPLICATION_JSON)
    @Operation(summary = "Preview branch equipment JSON из zip DTT (upload-download)")
    public ImportDttSetToBranchResponse previewBranchUpload(@Body byte[] zipPayload,
                                                            @QueryValue List<String> branchIds,
                                                            @QueryValue(defaultValue = "FAIL_IF_EXISTS") MergeStrategy mergeStrategy,
                                                            @QueryValue(defaultValue = "") String metadataJson) {
        if (metadataJson != null && !metadataJson.isBlank()) {
            return demoService.previewDttZipToBranch(zipPayload, parseBranchUploadMetadata(metadataJson));
        }
        if (branchIds == null || branchIds.isEmpty()) {
            throw new IllegalArgumentException("branchIds must contain at least one branch id");
        }
        return demoService.previewDttZipToBranch(zipPayload, branchIds, mergeStrategy);
    }

    /**
     * Выполняет merge-импорт zip-архива с файлами .dtt в существующий DeviceManager.json без multipart.
     *
     * @param zipPayload zip-архив с .dtt файлами
     * @param metadataJson JSON-метаданные merge-импорта
     * @return branch JSON после merge-импорта
     */
    @Post(uri = "/import/branch/merge/upload{?metadataJson}", consumes = MediaType.APPLICATION_OCTET_STREAM, produces = MediaType.APPLICATION_JSON)
    @Operation(summary = "Импортировать zip DTT в существующий branch equipment JSON (upload-download, без multipart)")
    public ImportDttSetToBranchResponse importToExistingBranchUpload(@Body byte[] zipPayload,
                                                                     @QueryValue String metadataJson) {
        return demoService.importDttZipToExistingBranch(zipPayload, parseExistingBranchUploadMetadata(metadataJson));
    }


    /**
     * Импортирует zip-архив с файлами .dtt в branch equipment JSON через multipart/form-data.
     *
     * @param zipPayload zip-архив с .dtt файлами
     * @param metadataJson JSON-метаданные импорта branch upload
     * @return branch JSON и количество branch
     */
    @Post(uri = "/import/branch/upload/multipart", consumes = MediaType.MULTIPART_FORM_DATA, produces = MediaType.APPLICATION_JSON)
    @Operation(summary = "Импортировать zip DTT в branch equipment JSON через multipart/form-data")
    public ImportDttSetToBranchResponse importToBranchUploadMultipart(
            @Part("zipPayload") @Schema(type = "string", format = "binary", description = "ZIP-архив с .dtt файлами")
            byte[] zipPayload,
            @Part("metadataJson") @Schema(description = "JSON-метаданные импорта branch upload", example = DttSwaggerExamples.IMPORT_BRANCH_UPLOAD_METADATA_EXAMPLE)
            String metadataJson) {
        return demoService.importDttZipToBranch(zipPayload, parseBranchUploadMetadata(metadataJson));
    }

    /**
     * Выполняет preview-сборку branch equipment JSON из zip-архива с файлами .dtt через multipart/form-data.
     *
     * @param zipPayload zip-архив с .dtt файлами
     * @param metadataJson JSON-метаданные preview-импорта branch upload
     * @return preview branch JSON и количество branch
     */
    @Post(uri = "/preview/branch/upload/multipart", consumes = MediaType.MULTIPART_FORM_DATA, produces = MediaType.APPLICATION_JSON)
    @Operation(summary = "Preview branch equipment JSON из zip DTT через multipart/form-data")
    public ImportDttSetToBranchResponse previewBranchUploadMultipart(
            @Part("zipPayload") @Schema(type = "string", format = "binary", description = "ZIP-архив с .dtt файлами")
            byte[] zipPayload,
            @Part("metadataJson") @Schema(description = "JSON-метаданные preview branch upload", example = DttSwaggerExamples.IMPORT_BRANCH_UPLOAD_METADATA_EXAMPLE)
            String metadataJson) {
        return demoService.previewDttZipToBranch(zipPayload, parseBranchUploadMetadata(metadataJson));
    }

    /**
     * Выполняет merge-импорт zip-архива с файлами .dtt в существующий DeviceManager.json через multipart/form-data.
     *
     * @param zipPayload zip-архив с .dtt файлами
     * @param metadataJson JSON-метаданные merge-импорта
     * @return branch JSON после merge-импорта
     */
    @Post(uri = "/import/branch/merge/upload/multipart", consumes = MediaType.MULTIPART_FORM_DATA, produces = MediaType.APPLICATION_JSON)
    @Operation(summary = "Импортировать zip DTT в существующий branch equipment JSON через multipart/form-data")
    public ImportDttSetToBranchResponse importToExistingBranchUploadMultipart(
            @Part("zipPayload") @Schema(type = "string", format = "binary", description = "ZIP-архив с .dtt файлами")
            byte[] zipPayload,
            @Part("metadataJson") @Schema(description = "JSON-метаданные merge-импорта branch upload", example = DttSwaggerExamples.IMPORT_BRANCH_MERGE_UPLOAD_METADATA_EXAMPLE)
            String metadataJson) {
        return demoService.importDttZipToExistingBranch(zipPayload, parseExistingBranchUploadMetadata(metadataJson));
    }

    private ImportDttZipToProfileUploadRequest parseProfileUploadMetadata(String metadataJson) {
        return parseUploadMetadata(
                metadataJson,
                ImportDttZipToProfileUploadRequest.class,
                "Invalid metadata JSON for profile upload"
        );
    }

    private ImportDttZipToBranchUploadRequest parseBranchUploadMetadata(String metadataJson) {
        return parseUploadMetadata(
                metadataJson,
                ImportDttZipToBranchUploadRequest.class,
                "Invalid metadata JSON for branch upload"
        );
    }

    private ImportDttZipToExistingBranchUploadRequest parseExistingBranchUploadMetadata(String metadataJson) {
        return parseUploadMetadata(
                metadataJson,
                ImportDttZipToExistingBranchUploadRequest.class,
                "Invalid metadata JSON for branch merge upload"
        );
    }

    private <T> T parseUploadMetadata(String metadataJson, Class<T> targetType, String errorMessage) {
        if (metadataJson == null || metadataJson.isBlank()) {
            throw new IllegalArgumentException("metadata must not be blank");
        }
        final String normalized = normalizeMetadataJson(metadataJson);
        try {
            return objectMapper.readValue(normalized, targetType);
        } catch (Exception primaryException) {
            try {
                final Object parsed = new LenientMapLikeParser(normalized).parseValue();
                return objectMapper.convertValue(parsed, targetType);
            } catch (Exception secondaryException) {
                throw new IllegalArgumentException(errorMessage, primaryException);
            }
        }
    }

    private String normalizeMetadataJson(String metadataJson) {
        String normalized = metadataJson.trim();
        if (normalized.length() >= 2 && normalized.startsWith("\"") && normalized.endsWith("\"")) {
            try {
                normalized = objectMapper.readValue(normalized, String.class);
            } catch (Exception ignored) {
                normalized = normalized.substring(1, normalized.length() - 1);
                normalized = normalized.replace("\\\"", "\"");
                normalized = normalized.replace("\\n", "\n");
                normalized = normalized.replace("\\r", "\r");
                normalized = normalized.replace("\\t", "\t");
                normalized = normalized.replace("\\\\", "\\");
            }
        }
        return normalized.trim();
    }

    private static final class LenientMapLikeParser {

        private final String text;
        private int index;

        private LenientMapLikeParser(String text) {
            this.text = text;
        }

        private Object parseValue() {
            skipWhitespace();
            if (index >= text.length()) {
                return null;
            }
            char ch = text.charAt(index);
            if (ch == '{') {
                return parseMap();
            }
            if (ch == '[') {
                return parseList();
            }
            if (ch == '"' || ch == '\'') {
                return parseQuotedString();
            }
            return parseBareToken();
        }

        private Map<String, Object> parseMap() {
            expect('{');
            Map<String, Object> result = new LinkedHashMap<>();
            skipWhitespace();
            if (peek('}')) {
                index++;
                return result;
            }
            while (index < text.length()) {
                String key = parseKey();
                skipWhitespace();
                if (peek('=') || peek(':')) {
                    index++;
                }
                Object value = parseValue();
                result.put(key, value);
                skipWhitespace();
                if (peek(',')) {
                    index++;
                    skipWhitespace();
                    continue;
                }
                if (peek('}')) {
                    index++;
                    break;
                }
            }
            return result;
        }

        private List<Object> parseList() {
            expect('[');
            List<Object> result = new ArrayList<>();
            skipWhitespace();
            if (peek(']')) {
                index++;
                return result;
            }
            while (index < text.length()) {
                result.add(parseValue());
                skipWhitespace();
                if (peek(',')) {
                    index++;
                    skipWhitespace();
                    continue;
                }
                if (peek(']')) {
                    index++;
                    break;
                }
            }
            return result;
        }

        private String parseKey() {
            skipWhitespace();
            if (peek('"') || peek('\'')) {
                return parseQuotedString();
            }
            int start = index;
            while (index < text.length()) {
                char ch = text.charAt(index);
                if (ch == '=' || ch == ':' || ch == ',' || ch == '}' || Character.isWhitespace(ch)) {
                    break;
                }
                index++;
            }
            return text.substring(start, index).trim();
        }

        private String parseQuotedString() {
            char quote = text.charAt(index++);
            StringBuilder builder = new StringBuilder();
            while (index < text.length()) {
                char ch = text.charAt(index++);
                if (ch == '\\' && index < text.length()) {
                    char escaped = text.charAt(index++);
                    switch (escaped) {
                        case 'n' -> builder.append('\n');
                        case 'r' -> builder.append('\r');
                        case 't' -> builder.append('\t');
                        case '\\' -> builder.append('\\');
                        case '"' -> builder.append('"');
                        case '\'' -> builder.append('\'');
                        default -> builder.append(escaped);
                    }
                    continue;
                }
                if (ch == quote) {
                    break;
                }
                builder.append(ch);
            }
            return builder.toString();
        }

        private Object parseBareToken() {
            int start = index;
            while (index < text.length()) {
                char ch = text.charAt(index);
                if (ch == ',' || ch == '}' || ch == ']') {
                    break;
                }
                index++;
            }
            String token = text.substring(start, index).trim();
            if (token.isEmpty()) {
                return "";
            }
            if ("null".equalsIgnoreCase(token)) {
                return null;
            }
            if ("true".equalsIgnoreCase(token)) {
                return Boolean.TRUE;
            }
            if ("false".equalsIgnoreCase(token)) {
                return Boolean.FALSE;
            }
            if (token.matches("-?\\d+")) {
                try {
                    return Integer.valueOf(token);
                } catch (NumberFormatException ignored) {
                    return Long.valueOf(token);
                }
            }
            if (token.matches("-?\\d+\\.\\d+")) {
                return Double.valueOf(token);
            }
            return token;
        }

        private void expect(char ch) {
            skipWhitespace();
            if (!peek(ch)) {
                throw new IllegalArgumentException("Expected '" + ch + "' at index " + index);
            }
            index++;
        }

        private boolean peek(char ch) {
            return index < text.length() && text.charAt(index) == ch;
        }

        private void skipWhitespace() {
            while (index < text.length() && Character.isWhitespace(text.charAt(index))) {
                index++;
            }
        }
    }

    /**
     * Экспортирует один DTT-архив из profile JSON.
     *
     * @param request запрос с profile JSON и идентификатором типа устройства
     * @return экспортированный DTT-архив в Base64
     */
    @Post(uri = "/export/profile/one", consumes = MediaType.APPLICATION_JSON, produces = MediaType.APPLICATION_JSON)
    @Operation(summary = "Экспортировать один DTT из profile JSON")
    @RequestBody(
            required = true,
            content = @Content(examples = {
                    @ExampleObject(name = "jsonObject", value = DttSwaggerExamples.PROFILE_EXPORT_SINGLE_OBJECT)
            })
    )
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
        if (request.profileJson() != null && !request.profileJson().isNull()) {
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
    @RequestBody(
            required = true,
            content = @Content(examples = {
                    @ExampleObject(name = "jsonObject", value = DttSwaggerExamples.PROFILE_EXPORT_SINGLE_OBJECT)
            })
    )
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
        if (request.profileJson() != null && !request.profileJson().isNull()) {
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
    @RequestBody(
            required = true,
            content = @Content(examples = {
                    @ExampleObject(name = "jsonObject", value = DttSwaggerExamples.PROFILE_EXPORT_SINGLE_OBJECT)
            })
    )
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
        } else if (request.profileJson() != null && !request.profileJson().isNull()) {
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
                                      "description": "Терминал (Киоск)",
                                      "imageBase64": "iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAA=="
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
                    @ExampleObject(name = "jsonObject", value = DttSwaggerExamples.PROFILE_EXPORT_ALL_OBJECT)
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
        if (request.profileJson() != null && !request.profileJson().isNull()) {
            return demoService.exportAllDttFromProfileJson(request.profileJson(), request.deviceTypeIds(), request.dttVersion());
        }
        throw new IllegalArgumentException("Either profile or profileJson must be provided");
    }

    /**
     * Экспортирует набор DTT из profile JSON в zip-архив (режим upload-download).
     */
    @Post(uri = "/export/profile/all/download", consumes = MediaType.APPLICATION_JSON, produces = MediaType.APPLICATION_OCTET_STREAM)
    @Operation(summary = "Экспортировать все DTT из profile JSON в zip (upload-download)")
    @RequestBody(
            required = true,
            content = @Content(examples = {
                    @ExampleObject(name = "objectModel", value = DttSwaggerExamples.PROFILE_EXPORT_ALL_OBJECT)
            })
    )
    public HttpResponse<byte[]> exportAllFromProfileDownload(@Body ExportAllDttFromProfileRequest request) {
        if (request == null || (request.profile() == null && (request.profileJson() == null || request.profileJson().isNull()))) {
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
    @RequestBody(
            required = true,
            content = @Content(examples = {
                    @ExampleObject(name = "branchEquipmentObject", value = DttSwaggerExamples.BRANCH_EXPORT_SINGLE_OBJECT)
            })
    )
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
        if (request.branchJson() != null && !request.branchJson().isNull()) {
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
    @RequestBody(
            required = true,
            content = @Content(examples = {
                    @ExampleObject(name = "branchEquipmentObject", value = DttSwaggerExamples.BRANCH_EXPORT_SINGLE_OBJECT)
            })
    )
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
        if (request.branchJson() != null && !request.branchJson().isNull()) {
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
    @RequestBody(
            required = true,
            content = @Content(examples = {
                    @ExampleObject(name = "branchEquipmentObject", value = DttSwaggerExamples.BRANCH_EXPORT_SINGLE_OBJECT)
            })
    )
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
        } else if (request.branchJson() != null && !request.branchJson().isNull()) {
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
        if (request.branchJson() != null && !request.branchJson().isNull()) {
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
        if (request == null || (request.branchEquipment() == null && (request.branchJson() == null || request.branchJson().isNull()))) {
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
        return HttpResponse.badRequest(new DemoErrorResponse(DemoErrorCode.INVALID_ARGUMENT.name(), exception.getMessage()));
    }


    /**
     * Возвращает структурированную ошибку для проблем формата входного JSON.
     *
     * @param exception исключение формата
     * @return JSON ошибки с кодом и сообщением
     */
    @Error(exception = DttFormatException.class)
    public HttpResponse<DemoErrorResponse> handleFormatError(DttFormatException exception) {
        return HttpResponse.badRequest(new DemoErrorResponse(DemoErrorCode.DTT_FORMAT_ERROR.name(), exception.getMessage()));
    }

    /**
     * Возвращает структурированную ошибку для проблем валидации шаблона.
     */
    @Error(exception = TemplateValidationException.class)
    public HttpResponse<DemoErrorResponse> handleValidationError(TemplateValidationException exception) {
        return HttpResponse.unprocessableEntity().body(
                new DemoErrorResponse(DemoErrorCode.TEMPLATE_VALIDATION_ERROR.name(), exception.getMessage())
        );
    }

    /**
     * Возвращает структурированную ошибку для проблем импорта шаблона.
     */
    @Error(exception = TemplateImportException.class)
    public HttpResponse<DemoErrorResponse> handleImportError(TemplateImportException exception) {
        return HttpResponse.status(HttpStatus.CONFLICT).body(
                new DemoErrorResponse(DemoErrorCode.TEMPLATE_IMPORT_ERROR.name(), exception.getMessage())
        );
    }

    /**
     * Возвращает структурированную ошибку для проблем экспорта шаблона.
     */
    @Error(exception = TemplateExportException.class)
    public HttpResponse<DemoErrorResponse> handleExportError(TemplateExportException exception) {
        return HttpResponse.status(HttpStatus.CONFLICT).body(
                new DemoErrorResponse(DemoErrorCode.TEMPLATE_EXPORT_ERROR.name(), exception.getMessage())
        );
    }

    /**
     * Возвращает структурированную ошибку для проблем merge/assembly сценариев.
     */
    @Error(exception = TemplateAssemblyException.class)
    public HttpResponse<DemoErrorResponse> handleAssemblyError(TemplateAssemblyException exception) {
        return HttpResponse.status(HttpStatus.CONFLICT).body(
                new DemoErrorResponse(DemoErrorCode.TEMPLATE_ASSEMBLY_ERROR.name(), exception.getMessage())
        );
    }

    /**
     * Возвращает структурированную ошибку по умолчанию для неожиданных исключений.
     */
    @Error(exception = Throwable.class)
    public HttpResponse<DemoErrorResponse> handleUnexpectedError(Throwable exception) {
        return HttpResponse.serverError(new DemoErrorResponse(DemoErrorCode.INTERNAL_ERROR.name(), exception.getMessage()));
    }

}
