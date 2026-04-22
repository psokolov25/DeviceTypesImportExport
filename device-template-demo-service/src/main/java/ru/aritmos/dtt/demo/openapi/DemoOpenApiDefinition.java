package ru.aritmos.dtt.demo.openapi;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.tags.Tag;

/**
 * Глобальная OpenAPI-конфигурация demo-service с детализированными тегами Swagger UI.
 */
@OpenAPIDefinition(
        info = @Info(
                title = "Device Template Demo Service API",
                version = "1.0",
                description = "Демо REST API для диагностики, импорта, preview-сборки и экспорта DTT-архивов, а также для сборки profile JSON и branch equipment JSON с агрегированными metadata в корне ответа."
        ),
        tags = {
                @Tag(name = "System", description = "Системные точки demo-service: health-check и проверка доступности служебной инфраструктуры Swagger/OpenAPI."),
                @Tag(name = "DTT · Диагностика", description = "Диагностика входных DTT-архивов: валидация структуры, Groovy-скриптов, инспекция содержимого, извлечение базовых metadata и сравнение версий."),
                @Tag(name = "DTT · Импорт в profile JSON", description = "Операции импорта и preview-сборки profile JSON из одного или нескольких DTT. Используются для подготовки канонического профиля типов устройств. В profile JSON в корне присутствует агрегированная секция metadata."),
                @Tag(name = "DTT · Импорт в branch JSON", description = "Операции импорта, merge-импорта и preview-сборки branch equipment JSON из набора DTT. Предназначены для подготовки конфигурации отделений и оборудования. В branch JSON в корне присутствует агрегированная секция metadata."),
                @Tag(name = "DTT · Комплексные сценарии", description = "Сценарии, в которых из одного запроса одновременно собираются несколько производных представлений, например profile JSON и branch equipment JSON с наследованием или переопределением metadata."),
                @Tag(name = "DTT · Экспорт из profile JSON", description = "Операции экспорта одного или нескольких типов устройств из profile JSON обратно в DTT, включая preview single-export и download-режимы."),
                @Tag(name = "DTT · Экспорт из branch JSON", description = "Операции экспорта одного или нескольких типов устройств из branch equipment JSON обратно в DTT, включая merge-логику между отделениями, preview и download-режимы.")
        }
)
public final class DemoOpenApiDefinition {

    private DemoOpenApiDefinition() {
    }
}
