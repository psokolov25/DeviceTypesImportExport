# DeviceTypesImportExport

Репозиторий содержит стартовую реализацию multi-module проекта для работы с `.dtt` (один тип устройства в одном архиве).

## Модули

- `device-template-library` — библиотека импорта/экспорта и сборки profile JSON.
- `device-template-demo-service` — demo-модуль на Micronaut 4.x.

## Что реализовано

### 1) Сборка profile JSON из нескольких шаблонов

В библиотеке реализованы:

- API `TemplateAssemblyService`;
- preview API `TemplateAssemblyService` (`previewEquipmentProfile`, `previewBranchEquipment`);
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
- batch export/import DTT set из profile (`exportDttSetFromProfile` / `importDttSetToProfile`);
- batch export/import DTT set из branch (`exportDttSetFromBranch` / `importDttSetToBranch`).
- preview-сценарии на уровне библиотеки:
  - profile (`previewDttSetToProfile`, `previewDttBase64SetToProfile`, `previewDttZipToProfile`);
  - branch equipment (`previewDttSetToBranch`, `previewDttBase64SetToBranch`, `previewDttZipToBranch`).
- dual-mode передачи DTT на уровне фасада библиотеки:
  - Base64 (`importDttBase64SetToProfile`, `importDttBase64SetToBranch`);
  - upload-download zip (`importDttZipToProfile`, `importDttZipToBranch`, `exportProfileToDttZip`, `exportBranchToDttZip`);
  - zip+Base64 (`exportProfileToDttZipBase64`, `exportBranchToDttZipBase64`).
- dual-mode JSON на уровне фасада библиотеки:
  - object-model запросы (`ProfileExportRequest`, `BranchEquipmentExportRequest`);
  - string JSON (`exportDttSetFromProfileJson`, `exportDttSetFromBranchJson`) с поддержкой передачи `dttVersion`.

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
- `POST /api/dtt/preview/profile` (application/json, Base64 DTT set -> preview profile JSON).
- `POST /api/dtt/import/profile/upload` (application/octet-stream zip с `.dtt` -> profile JSON).
- `POST /api/dtt/preview/profile/upload` (application/octet-stream zip с `.dtt` -> preview profile JSON).
- `POST /api/dtt/export/profile/all` (application/json, profile JSON -> Base64 DTT set).
- `POST /api/dtt/export/profile/all/download` (application/json, profile JSON -> zip с `.dtt`).
- `POST /api/dtt/import/branch` (application/json, Base64 DTT set + branchIds -> branch equipment JSON).
- `POST /api/dtt/preview/branch` (application/json, Base64 DTT set + branchIds -> preview branch equipment JSON).
- `POST /api/dtt/import/branch/upload` (application/octet-stream zip с `.dtt` + query branchIds -> branch equipment JSON).
- `POST /api/dtt/preview/branch/upload` (application/octet-stream zip с `.dtt` + query branchIds -> preview branch equipment JSON).
- `POST /api/dtt/export/branch/all` (application/json, branch equipment JSON -> Base64 DTT set, поддерживает фильтры `branchIds` и `deviceTypeIds`).
- `POST /api/dtt/export/branch/all/download` (application/json, branch equipment JSON -> zip с `.dtt`).
- export endpoint-ы поддерживают **оба варианта входа**: типизированные объектные модели (`profile`, `branchEquipment`) и строковые JSON-поля (`profileJson`, `branchJson`).
- для export endpoint-ов можно передать `dttVersion`:
  - версия фиксируется в `manifest.yml` (`deviceTypeVersion`) без изменения версии формата (`formatVersion`);
  - версия добавляется в конец `description` типа устройства;
  - `defaultValues` в DTT берутся из фактических значений параметров типа устройства, переданных в profile/branch JSON.
- `GET /swagger-ui/index.html` (Swagger UI для ручного прогона сценариев).
- `GET /swagger/device-template-demo.yml` (OpenAPI-спецификация demo-service).

## Maven-окружение в репозитории

В репозитории размещено полноценное Maven-окружение для воспроизводимой сборки:

- Maven Wrapper (`mvnw`, `mvnw.cmd`, `.mvn/wrapper/maven-wrapper.properties`) для запуска без локальной установки Maven;
- `.mvn/maven.config` с настройками batch-режима, отключением progress-шума и фиксированным локальным репозиторием `.m2/repository`;
- `.mvn/jvm.config` с единым `UTF-8` и фиксированной локалью JVM;
- `maven-enforcer-plugin` в root `pom.xml` для обязательных версий Java 17 и Maven 3.9+;
- единая фиксация версий базовых Maven plugins в `pluginManagement` для стабильной multi-module сборки.

Это позволяет одинаково запускать команды локально, в CI и в контейнерных окружениях.


## Рекомендуемые настройки IntelliJ IDEA 2025 (оптимально для этого проекта)

Ниже — практичный baseline для Windows/Linux/macOS, чтобы избежать проблем с `mvnw` и обеспечить повторяемую multi-module сборку.

### 1) Project SDK и язык

- `File -> Project Structure -> Project`:
  - `Project SDK`: **Java 17**;
  - `Project language level`: **17 (SDK default)**.
- `Modules`: убедитесь, что оба модуля (`device-template-library`, `device-template-demo-service`) используют тот же SDK 17.

### 2) Maven в IDEA (ключевой пункт)

- `Settings -> Build, Execution, Deployment -> Build Tools -> Maven`:
  - `Maven home`: **Bundled (Maven 3)** или **Wrapper** (предпочтительно Wrapper);
  - `User settings file`: default;
  - `Local repository`: оставить default;
  - для CLI в этом репозитории отдельный `-Dmaven.repo.local` не требуется (он уже задан в `.mvn/maven.config`).
  - включить `Always update snapshots` только при необходимости.
- `Runner`:
  - `JRE`: **Project SDK (17)**;
  - `VM Options`: `-Dfile.encoding=UTF-8 -Duser.language=ru -Duser.country=RU`;
  - `Environment variables` (Windows): при необходимости явно задать `JAVA_HOME=<путь к JDK 17>`.

> Если в IDEA на Windows появляется ошибка вида `fail to move MAVEN_HOME`/`Cannot start Maven from wrapper`, обычно помогает переключение `Maven home` на **Bundled Maven** и явный выбор `JRE = JDK 17`.

### 3) Делегирование сборки

- `Settings -> Build Tools -> Maven -> Runner`:
  - включить `Delegate IDE build/run actions to Maven` для максимальной близости к CI;
  - либо оставить выключенным для более быстрых локальных инкрементальных сборок (рекомендуется только при понимании отличий от CI).

Для этого репозитория предпочтителен режим с делегированием в Maven.

### 4) Annotation Processing и Micronaut

- `Settings -> Build, Execution, Deployment -> Compiler -> Annotation Processors`:
  - `Enable annotation processing` = **ON**.

Это важно для стабильной генерации Micronaut метаданных в demo-service.

### 5) Код-стиль и импорты

- `Settings -> Editor -> Code Style -> Java`:
  - `Tab size = 4`, `Indent = 4`;
  - `Continuation indent = 8`;
  - line separator `LF` (если команда не требует иного).
- `Settings -> Editor -> Code Style -> Java -> Imports`:
  - использовать `Class count to use import with '*' = 999`;
  - `Names count to use static import with '*' = 999`.

Это снижает шум в diff и делает ревью предсказуемее.

### 6) Рекомендуемые Run Configuration

Создайте Maven-конфигурации из корня репозитория:

1. `clean test (root)`
   - Command line: `clean test`
2. `verify (root)`
   - Command line: `clean verify`
3. `demo test with dependencies`
   - Command line: `-pl device-template-demo-service -am test`

### 7) Что проверить после настройки IDEA

1. Успешный `Reload All Maven Projects`;
2. Успешный запуск `clean test (root)` из IDEA;
3. Отсутствие ошибки резолва `device-template-library:0.1.0-SNAPSHOT` в demo-модуле;
4. Корректная генерация OpenAPI/Micronaut артефактов при сборке.

## Тесты

Запуск полного набора тестов:

```bash
./mvnw clean test
```

## Как запустить demo-service

В репозитории уже есть Maven Wrapper:

- `./mvnw` для Linux/macOS;
- `mvnw.cmd` для Windows.

> Важно: `device-template-demo-service` зависит от локального модуля `device-template-library`,  
> поэтому запускать demo-модуль нужно с флагом `-am` (also-make), чтобы Maven сначала собрал зависимый модуль.

Запуск из корня репозитория:

```bash
./mvnw -pl device-template-demo-service -am test
./mvnw -pl device-template-demo-service -am install -DskipTests
./mvnw -f device-template-demo-service/pom.xml exec:java
```

> Примечание: в текущей конфигурации репозитория запуск через `mn:run` может не резолвиться по plugin-prefix
> в чистом окружении Maven. Для воспроизводимого старта используйте `exec:java` или запуск shaded-jar.

Сборка fat jar demo-service (shaded jar):

```bash
./mvnw -pl device-template-demo-service -am clean package
java -jar device-template-demo-service/target/device-template-demo-service-0.1.0-SNAPSHOT-all.jar
```

После запуска demo-service:

- health-check: `http://localhost:8080/api/system/health`
- Swagger UI: `http://localhost:8080/swagger-ui/index.html`
- OpenAPI YAML: `http://localhost:8080/swagger/device-template-demo.yml`


> Примечание для PowerShell: если вручную добавлять `-D...` параметры, лучше брать их в кавычки,
> например `"-Dmaven.repo.local=.m2/repository"`, иначе в некоторых окружениях PowerShell аргумент
> может быть разбит некорректно и Maven покажет ошибку `Unknown lifecycle phase ".repo.local=..."`.
> В этом репозитории отдельный `-Dmaven.repo.local` обычно не нужен, так как он уже задан в `.mvn/maven.config`.

Типовая причина ошибки `Could not find artifact ru.aritmos.dtt:device-template-library:0.1.0-SNAPSHOT` — запуск demo-модуля без `-am` или без предварительной сборки root-проекта.

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
2. Добавить preview endpoint-ы сборки profile/branch с отображением рассчитанных defaults/overrides без сохранения.
3. Расширить валидацию Groovy с проверкой доменных контекстов выполнения (не только синтаксис).
