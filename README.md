# DeviceTypesImportExport

Репозиторий содержит стартовую реализацию multi-module проекта для работы с `.dtt` (один тип устройства в одном архиве).

## Модули

- `device-template-library` — библиотека импорта/экспорта и сборки profile JSON.
- `device-template-demo-service` — demo-модуль на Micronaut 4.x.

## Что реализовано

### 1) Сборка profile JSON из нескольких шаблонов

В библиотеке реализованы:

- API `TemplateAssemblyService`;
- DTO сборки профиля (`EquipmentProfileAssemblyRequest`, `EquipmentProfileDeviceTypeRequest`, `TemplateValueOverride`);
- merge-стратегии:
  - `FAIL_IF_EXISTS`
  - `REPLACE`
  - `MERGE_NON_NULLS`
  - `MERGE_PRESERVE_EXISTING`
  - `CREATE_COPY_WITH_SUFFIX`
- `DefaultTemplateAssemblyService`.

### 2) Базовый DTT archive I/O (YAML внутри ZIP)

Добавлены:

- `DttArchiveWriter` / `DefaultDttArchiveWriter`;
- `DttArchiveReader` / `DefaultDttArchiveReader`;
- archive DTO:
  - `DttArchiveDescriptor`
  - `DttArchiveTemplate`
- сервисы верхнего уровня:
  - `DeviceTypeTemplateExportService`
  - `DeviceTypeTemplateImportService`
- диагностичное исключение `DttFormatException`.
- валидация Groovy-скриптов через `TemplateValidationService` (`DefaultTemplateValidationService`) с диагностикой по пути скрипта.

Внутри архива пишутся YAML-файлы (`manifest.yml`, `template/*.yml`) и Groovy-скрипты (`scripts/*.groovy`, `scripts/event-handlers/*.groovy`, `scripts/commands/*.groovy`).

### 3) Deterministic archive output

`DefaultDttArchiveWriter` формирует детерминированный ZIP:

- фиксированный порядок записи;
- фиксированный timestamp `ZipEntry`.

Это упрощает git-diff и автоматические проверки байтовой стабильности.


### 5) Branch equipment JSON и JSON parser/generator

Добавлены модели и сервисы для полного JSON уровня отделений:

- `BranchEquipment`, `BranchNode`, `BranchDeviceType`, `DeviceInstanceTemplate`;
- сборка через `TemplateAssemblyService#assembleBranchEquipment(...)`;
- `DeviceManagerBranchJsonParser` / `DeviceManagerBranchJsonGenerator`;
- `EquipmentProfileJsonParser` / `EquipmentProfileJsonGenerator`.

Поддержаны сценарии:

- тип устройства без дочерних устройств;
- тип устройства с одним или несколькими устройствами;
- merge-конфликты типов устройств в отделении.


### 6) Canonical internal model (первая итерация)

Добавлены:

- `CanonicalDeviceTypeTemplate`;
- `CanonicalDeviceTypeMetadata`;
- `CanonicalScriptSet`;
- `CanonicalTemplateMapper` / `DefaultCanonicalTemplateMapper` для преобразований `DTT archive DTO <-> canonical`.

На текущем этапе маппер сохраняет Groovy-код и ключевые metadata/schema/default/example поля без потерь.


### 7) Использование библиотеки как зависимости (facade API)

Для удобного программного использования добавлены:

- `DeviceTemplateLibraryFacade`;
- `DefaultDeviceTemplateLibraryFacade`;
- `DeviceTemplateLibrary#createDefaultFacade()`;
- `DeviceTemplateLibrary#createFacadeBuilder()` для подмены реализаций.

Фасад поддерживает методные сценарии:

- read/write DTT bytes;
- validate DTT bytes/template;
- assemble profile/branch модели;
- parse/generate profile/branch JSON.
- batch export/import DTT set из profile (`exportDttSetFromProfile` / `importDttSetToProfile`).

Пример методного использования:

```java
DeviceTemplateLibraryFacade facade = DeviceTemplateLibrary.createDefaultFacade();

DttArchiveTemplate template = facade.readDtt(bytes);
ValidationResult validation = facade.validate(template);
String profileJson = facade.toProfileJson(
        facade.assembleProfile(profileRequest)
);
```

### 4) Demo-service

Добавлен технический endpoint:

- `GET /api/system/health`.
- `POST /api/dtt/validate` (application/octet-stream).
- `POST /api/dtt/inspect` (application/octet-stream).
- `POST /api/dtt/import/profile` (application/json, Base64 DTT set -> profile JSON).
- `POST /api/dtt/export/profile/all` (application/json, profile JSON -> Base64 DTT set).

## Тесты

Запуск полного набора тестов:

```bash
./mvnw -Dmaven.repo.local=.m2/repository clean test
```

Покрыты сценарии:

- merge-стратегии сборки profile JSON;
- применение override-значений;
- round-trip DTT writer -> reader;
- broken YAML;
- missing required YAML (`manifest.yml`);
- deterministic ZIP output;
- import/export сервисы поверх archive reader/writer.
- валидация синтаксиса Groovy-скриптов (lifecycle, event-handlers, commands).
- branch assembly (включая type without child devices и multiple device instances).
- генерация/парсинг profile JSON и branch equipment JSON.

## Ближайшие шаги

1. Расширить canonical-модель до полного покрытия profile/branch и parameter schema уровней (включая nested структуры).
2. Добавить экспорт набора `.dtt` из profile JSON и branch equipment JSON.
3. Расширить demo-service endpoint-ами импорта/экспорта и preview-сценариями.
4. Расширить валидацию Groovy с проверкой доменных контекстов выполнения (не только синтаксис).
