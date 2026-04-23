# Руководство разработчика по интеграции `device-template-library`

Документ ориентирован на разработчика, который пишет **свою** службу-потребитель с нуля и использует `device-template-library` как обычную Java-библиотеку. Ниже акцент сделан не на demo-service и не на Swagger UI, а на встраивании библиотеки в собственный код, доменную модель, файловое хранилище, REST API и фоновые процессы.

Demo-service в этом репозитории полезен как:

- интеграционный пример того, как оборачивать библиотеку в HTTP API;
- ручной стенд для проверки сценариев через Swagger UI;
- источник контрактных OpenAPI-примеров.

Но для прикладного разработчика основной контракт — это `DeviceTemplateLibraryFacade`.

## 1. Что библиотека даёт прикладной службе

С точки зрения внешнего интегратора библиотека решает четыре класса задач:

1. **Работа с одним DTT**: прочитать архив, проверить, модифицировать и снова записать.
2. **Сборка прикладной конфигурации**: построить `EquipmentProfile` или `BranchEquipment` из шаблонов и override-значений.
3. **Batch-операции**: импортировать/экспортировать набор `.dtt` из/в profile, branch equipment, Base64 и ZIP.
4. **Преобразование между типизированной моделью и JSON**: `parseProfileJson`, `parseBranchJson`, `toProfileJson`, `toBranchJson`.

Из этого следует простое правило:

- если ваша служба работает с **одним шаблоном**, используйте `readDtt/writeDtt/validate`;
- если ваша служба собирает **профиль или отделения**, используйте `assembleProfile/assembleBranch`;
- если ваша служба принимает или отдает **набор архивов**, используйте `import...Set...`, `export...Set...`, `importDttZip...`, `export...ToDttZip...`.

## 2. Архитектура интеграции

Исходники PlantUML расположены в каталоге [`docs/plantuml`](plantuml), а визуализированные SVG-иллюстрации — в [`docs/plantuml/svg`](plantuml/svg).

### 2.1. Архитектура библиотеки

![Архитектура библиотеки](plantuml/svg/01-library-architecture.svg)

PlantUML: [`docs/plantuml/01-library-architecture.puml`](plantuml/01-library-architecture.puml)

### 2.2. Алгоритм интегратора

![Алгоритм интегратора](plantuml/svg/02-import-export-algorithm.svg)

PlantUML: [`docs/plantuml/02-import-export-algorithm.puml`](plantuml/02-import-export-algorithm.puml)

### 2.3. Один DTT → несколько производных типов / наборов устройств

![Один DTT -> несколько производных шаблонов](plantuml/svg/03-derived-device-types.svg)

PlantUML: [`docs/plantuml/03-derived-device-types.puml`](plantuml/03-derived-device-types.puml)

### 2.4. Выбор merge-стратегии

![Выбор merge-стратегии](plantuml/svg/04-merge-strategy-choice.svg)

PlantUML: [`docs/plantuml/04-merge-strategy-choice.puml`](plantuml/04-merge-strategy-choice.puml)

## 3. Подключение к своей службе

### 3.1. Maven dependency

```xml
<dependency>
    <groupId>ru.aritmos.dtt</groupId>
    <artifactId>device-template-library</artifactId>
    <version>0.1.0-SNAPSHOT</version>
</dependency>
```

Если ваш прикладной сервис собирается в том же multi-module проекте, достаточно зависеть от модуля `device-template-library`. Если сервис живёт отдельно, публикуйте библиотеку во внутренний Maven-репозиторий и подключайте её как обычный артефакт.

### 3.2. Базовая инициализация фасада

```java
import ru.aritmos.dtt.api.DeviceTemplateLibrary;
import ru.aritmos.dtt.api.DeviceTemplateLibraryFacade;

DeviceTemplateLibraryFacade facade = DeviceTemplateLibrary.createDefaultFacade();
```

`createDefaultFacade()` подходит для большинства сценариев.

`createFacadeBuilder()` нужен, если вы хотите:

- подменить сериализаторы/парсеры;
- подменить реализацию валидации;
- встроить свои low-level сервисы импорта/экспорта;
- тестировать службу с частично подменённым поведением фасада.

## 4. Ментальная модель API библиотеки

### 4.1. Ключевые типы

- `DeviceTemplateLibraryFacade` — основной API для прикладной службы.
- `DttArchiveTemplate` — полная модель одного `.dtt` архива.
- `DeviceTypeTemplate` — прикладное представление шаблона типа устройства, удобное для сборки profile/branch.
- `EquipmentProfile` — профиль оборудования.
- `BranchEquipment` — полная branch-конфигурация уровня `DeviceManager.json`.
- `ProfileExportRequest` / `BranchEquipmentExportRequest` — high-level DTO для batch-экспорта.
- `EquipmentProfileAssemblyRequest` / `BranchEquipmentAssemblyRequest` — DTO для явной сборки прикладной модели.

### 4.2. Три уровня API

#### Термины интеграции

- **Merge-стратегия** — правило разрешения конфликтов `deviceTypeId` при сборке/импорте.
- **Preview** — диагностический dry-run: можно проверить успех и получить причины ошибок, не выполняя фактическую выгрузку клиенту.
- **Single-export** — экспорт одного типа устройства (`deviceTypeId`) в один `.dtt`.
- **String JSON flow** — сценарий, где сервис не строит object-модель, а передаёт raw JSON строку напрямую в фасад.
- **High-level import-plan** — прикладной запрос, который описывает, какие DTT нужно взять, в какие branch/profile их собрать, какие metadata и значения параметров переопределить, и из какого источника читать архив: Base64 или zip entry.
- **Metadata override** — частичное или полное переопределение `id`, `name`, `displayName`, `description`, `version`, `iconBase64` для конкретного производного типа устройства.
- **Branch-specific topology** — branch-ориентированный снимок типа устройства внутри `templateOrigin`. Если он присутствует, библиотека может восстановить не только параметры типа устройства, но и связанные устройства, скрипты и branch-поля именно для заданного отделения.

#### Уровень 1. Архивный

Используйте, если хотите управлять содержимым одного шаблона:

- `readDtt(byte[])`
- `writeDtt(DttArchiveTemplate)`
- `validate(byte[])`
- `validate(DttArchiveTemplate)`

#### Уровень 2. Модельный

Используйте, если ваша служба оперирует типизированными объектами:

- `assembleProfile(...)`
- `assembleBranch(...)`
- `mergeBranchEquipment(...)`
- `parseProfileJson(...)`
- `parseBranchJson(...)`
- `toProfileJson(...)`
- `toBranchJson(...)`
- `compareDttVersion(...)`

#### Уровень 3. Batch-интеграционный

Используйте, если у вас потоки архивов, Base64 или zip:

- `importDttSetToProfile(...)`
- `importDttSetToBranch(...)`
- `importDttZipToProfile(...)`
- `importDttZipToBranch(...)`
- `readDttFilesFromZipByEntryName(...)`
- `resolveDttArchiveEntry(...)`
- `exportDttSetFromProfile(...)`
- `exportDttSetFromBranch(...)`
- `exportProfileToDttZip(...)`
- `exportBranchToDttZip(...)`

#### Уровень 3a. High-level import-plan подготовка

Используйте, если прикладная служба принимает не готовые `EquipmentProfileAssemblyRequest` / `BranchEquipmentAssemblyRequest`, а более прикладной запрос: Base64, zip, branchId, список типов устройств, override metadata, override параметров и override экземпляров устройств.

Ключевые методы:

- `prepareProfileAssemblyRequest(...)`
- `prepareProfileAssemblyRequestFromZip(...)`
- `prepareBranchAssemblyRequest(...)`
- `prepareBranchAssemblyRequestFromZip(...)`
- `computeProfileImportPreview(...)`
- `computeProfileImportPreview(byte[] zipPayload, ...)`
- `computeBranchImportPreview(...)`
- `computeBranchImportPreview(byte[] zipPayload, ...)`
- `previewProfileImportDetailed(...)`
- `previewProfileImportDetailed(byte[] zipPayload, ...)`
- `previewBranchImportDetailed(...)`
- `previewBranchImportDetailed(byte[] zipPayload, ...)`

Что именно делает этот слой:

1. Декодирует Base64-архивы и/или разрешает конкретные `.dtt` entry внутри zip-пакета.
2. Читает DTT и восстанавливает `DeviceTypeTemplate` / `BranchDeviceTypeImportRequest`.
3. Применяет metadata override.
4. Применяет override-значения параметров типа устройства.
5. Для branch-сценариев применяет override устройств и override поля `kind`.
6. Если в `templateOrigin` сохранена branch-specific topology, использует именно её, а не generic fallback-проекцию.
7. Возвращает готовый assembly-request, который затем передаётся в `assembleProfile(...)` или `assembleBranch(...)`.
8. Для preview-диагностики умеет считать default/override как в Base64-сценариях, так и в zip-сценариях с `archiveEntryName` и legacy-режиме `все .dtt -> все branchId`.
9. Для детального preview может одним вызовом вернуть и собранную preview-модель, и диагностические счётчики defaults/overrides без ручной склейки нескольких вызовов фасада.

Это позволяет держать HTTP-слой тонким: контроллер и application-service только валидируют входной payload и делегируют подготовку импорта в библиотеку.


##### Когда нужен именно детальный preview

Используйте `previewProfileImportDetailed(...)` или `previewBranchImportDetailed(...)`, когда вашей службе нужно одновременно:

- получить уже собранную preview-модель;
- показать пользователю диагностику по defaults/overrides;
- не дублировать orchestration `assemble + computePreview` в собственном application-service.

Пример:

```java
ProfileImportPreviewResult preview = facade.previewProfileImportDetailed(plan);
EquipmentProfile previewProfile = preview.profile();
Map<String, ImportPreviewComputationEntry> counters = preview.computationsByDeviceType();
```

### 4.1. Карта сценариев с быстрым выбором метода

1. **Нужно провалидировать один архив**  
   Используйте `readDtt(...)` + `validate(...)`.

2. **Нужно быстро извлечь карточки типов устройств из `.dtt`/zip**  
   Используйте `extractDeviceTypeMetadataFromDttOrZip(...)`.

3. **Нужно вычислить имя файла для download/export**  
   Используйте `resolveDeviceTypeArchiveBaseName(...)`.

4. **Нужно сравнить входную версию и версию в шаблоне**  
   Используйте `compareDttVersion(...)`.

5. **Нужно импортировать DTT-set в profile**  
   Используйте `importDttSetToProfile(...)` / `importDttBase64SetToProfile(...)` / `importDttZipToProfile(...)`.

6. **Нужно импортировать DTT-set в branch equipment**  
   Используйте `importDttSetToBranch(...)` / `importDttBase64SetToBranch(...)` / `importDttZipToBranch(...)`.

7. **Нужно merge в существующий branch equipment JSON**  
   Используйте `importDtt*ToExistingBranch(...)` или прямой `mergeBranchEquipment(...)`.

8. **Нужен безопасный preview перед применением**  
   Используйте `previewDttSetToProfile(...)` / `previewDttSetToBranch(...)` и Base64/zip аналоги.

9. **Нужно работать с zip на уровне конкретных entry**  
   Используйте `readDttFilesFromZipByEntryName(...)` + `resolveDttArchiveEntry(...)`.

10. **Нужно выгрузить DTT из profile/branch**  
    Используйте `exportDttSetFromProfile*` / `exportDttSetFromBranch*` или zip-режимы `exportProfileToDttZip(...)`, `exportBranchToDttZip(...)`.

11. **Нужно проверить single-export без фактической выгрузки файла**  
    Используйте `previewSingleDttExportFromProfile(...)` / `previewSingleDttExportFromBranch(...)`.

12. **Нужно получить один `.dtt` в бинарном виде (download endpoint)**  
    Используйте `exportSingleDttFromProfile(...)` / `exportSingleDttFromBranch(...)`.

13. **Нужно выполнять single-export/preview прямо из string JSON**  
    Используйте `exportSingleDttFromProfileJson(...)`, `exportSingleDttFromBranchJson(...)`,
    `previewSingleDttExportFromProfileJson(...)`, `previewSingleDttExportFromBranchJson(...)`.

14. **Нужно импортировать DTT-set в существующий branch, где текущее состояние хранится строкой JSON**  
    Используйте `importDttBase64SetToExistingBranchJson(...)` / `importDttZipToExistingBranchJson(...)`.

15. **Нужно сформировать zip DTT-set напрямую из string JSON**  
    Используйте `exportProfileToDttZip(String, ...)` / `exportBranchToDttZip(String, ...)`.

## 5. Базовые алгоритмы интеграции

## 5.1. Алгоритм A: принять один DTT от внешней системы

Подходит для REST endpoint вашей службы, файлового обмена, очереди сообщений, MinIO/S3 ingress.

### Рекомендуемая последовательность

1. Прочитать бинарное содержимое архива.
2. Вызвать `readDtt(...)`.
3. Вызвать `validate(...)`.
4. Если нужно — нормализовать metadata/default values/example values.
5. Либо сохранить DTT как артефакт, либо импортировать дальше в profile/branch.

### Пример

```java
import ru.aritmos.dtt.api.DeviceTemplateLibrary;
import ru.aritmos.dtt.api.DeviceTemplateLibraryFacade;
import ru.aritmos.dtt.api.dto.ValidationResult;
import ru.aritmos.dtt.archive.model.DttArchiveTemplate;

import java.nio.file.Files;
import java.nio.file.Path;

DeviceTemplateLibraryFacade facade = DeviceTemplateLibrary.createDefaultFacade();

byte[] bytes = Files.readAllBytes(Path.of("Terminal.dtt"));
DttArchiveTemplate template = facade.readDtt(bytes);
ValidationResult validation = facade.validate(template);

if (!validation.valid()) {
    throw new IllegalStateException("DTT невалиден: " + validation.errors());
}
```

### Что обычно делает прикладная служба после этого

- кладёт исходный DTT в файловое хранилище;
- извлекает `metadata.id`, `metadata.name`, `defaultValues` в свою БД каталога шаблонов;
- показывает пользователю результат `validate(...)`;
- строит из DTT производные шаблоны.

## 5.2. Алгоритм B: собрать профиль оборудования

Подходит, когда ваша служба хранит каталог шаблонов отдельно и должна получить профиль как объект или JSON.

### Пример

```java
import ru.aritmos.dtt.api.DeviceTemplateLibrary;
import ru.aritmos.dtt.api.DeviceTemplateLibraryFacade;
import ru.aritmos.dtt.api.dto.DeviceTypeMetadata;
import ru.aritmos.dtt.api.dto.DeviceTypeTemplate;
import ru.aritmos.dtt.api.dto.EquipmentProfileAssemblyRequest;
import ru.aritmos.dtt.api.dto.EquipmentProfileDeviceTypeRequest;
import ru.aritmos.dtt.api.dto.MergeStrategy;
import ru.aritmos.dtt.api.dto.TemplateValueOverride;
import ru.aritmos.dtt.json.profile.EquipmentProfile;

import java.util.List;
import java.util.Map;

DeviceTemplateLibraryFacade facade = DeviceTemplateLibrary.createDefaultFacade();

DeviceTypeTemplate terminal = new DeviceTypeTemplate(
        new DeviceTypeMetadata(
                "terminal",
                "Terminal",
                "Terminal",
                "Киоск самообслуживания"
        ),
        Map.of(
                "prefix", "DEF",
                "printerServiceURL", "http://127.0.0.1:8084"
        )
);

EquipmentProfile profile = facade.assembleProfile(new EquipmentProfileAssemblyRequest(
        List.of(new EquipmentProfileDeviceTypeRequest(terminal, true)),
        List.of(new TemplateValueOverride(
                "terminal",
                Map.of(
                        "prefix", "MSK",
                        "printerServiceURL", "http://10.10.10.10:8084"
                )
        )),
        MergeStrategy.FAIL_IF_EXISTS
));

String profileJson = facade.toProfileJson(profile);
```

### Когда этот путь лучше batch-импорта

Используйте `assembleProfile(...)`, если:

- шаблоны уже лежат у вас в БД/кеше/каталоге;
- вы сами управляете составом типов устройства;
- вы хотите явно задавать override-значения;
- вы хотите строить профиль по собственной доменной модели, а не только принимать готовые `.dtt` файлы от пользователя.

## 5.3. Алгоритм C: собрать `DeviceManager.json` / branch equipment

Это основной сценарий для служб, которые публикуют итоговую конфигурацию отделений.

### Пример

```java
import ru.aritmos.dtt.api.DeviceTemplateLibrary;
import ru.aritmos.dtt.api.DeviceTemplateLibraryFacade;
import ru.aritmos.dtt.api.dto.DeviceTypeMetadata;
import ru.aritmos.dtt.api.dto.DeviceTypeTemplate;
import ru.aritmos.dtt.api.dto.EquipmentProfileDeviceTypeRequest;
import ru.aritmos.dtt.api.dto.MergeStrategy;
import ru.aritmos.dtt.api.dto.branch.BranchDeviceTypeImportRequest;
import ru.aritmos.dtt.api.dto.branch.BranchEquipmentAssemblyRequest;
import ru.aritmos.dtt.api.dto.branch.BranchImportRequest;
import ru.aritmos.dtt.api.dto.branch.DeviceInstanceImportRequest;
import ru.aritmos.dtt.json.branch.BranchEquipment;

import java.util.List;
import java.util.Map;

DeviceTemplateLibraryFacade facade = DeviceTemplateLibrary.createDefaultFacade();

DeviceTypeTemplate display = new DeviceTypeTemplate(
        new DeviceTypeMetadata(
                "display-red-window",
                "Display WD3264 Красное окно",
                "Display WD3264 Красное окно",
                "Дисплей для красной зоны"
        ),
        Map.of(
                "FirstZoneColor", "red",
                "TicketZone", "5"
        )
);

BranchEquipment result = facade.assembleBranch(new BranchEquipmentAssemblyRequest(
        List.of(new BranchImportRequest(
                "branch-01",
                "Отделение 01",
                List.of(new BranchDeviceTypeImportRequest(
                        new EquipmentProfileDeviceTypeRequest(display, true),
                        List.of(
                                new DeviceInstanceImportRequest(
                                        "display-1",
                                        "display-1",
                                        "Display 1",
                                        "Первый дисплей",
                                        Map.of(
                                                "IP", "10.10.10.11",
                                                "Port", 22224,
                                                "ServicePointId", "sp-101",
                                                "ServicePointDisplayName", "ОКНО 101",
                                                "showOnStart", true
                                        )
                                )
                        ),
                        "Display",
                        null,
                        null,
                        null,
                        null,
                        null,
                        Map.of(),
                        Map.of()
                ))
        )),
        MergeStrategy.FAIL_IF_EXISTS
));

String branchJson = facade.toBranchJson(result);
```

### Практическое правило

Если ваш итог — это `DeviceManager.json`, то полезно проектировать интеграцию так, чтобы:

- входные данные вашей службы были вашей собственной доменной моделью;
- в самом конце эта модель преобразовывалась в DTO библиотеки;
- финальная сериализация делалась через `toBranchJson(...)`.

Так библиотека остаётся «движком трансформации», а не вытесняет вашу прикладную модель.

## 5.4. Алгоритм D: импортировать или экспортировать набор DTT

### Импорт ZIP в profile

```java
byte[] zipPayload = Files.readAllBytes(Path.of("device-types.zip"));
EquipmentProfile profile = facade.importDttZipToProfile(
        zipPayload,
        MergeStrategy.FAIL_IF_EXISTS
);
```

### Импорт ZIP в отделения

```java
byte[] zipPayload = Files.readAllBytes(Path.of("device-types.zip"));
BranchEquipment branchEquipment = facade.importDttZipToBranch(
        zipPayload,
        List.of("branch-01", "branch-02"),
        MergeStrategy.MERGE_NON_NULLS
);
```

### Экспорт набора DTT из существующего `DeviceManager.json`

```java
import ru.aritmos.dtt.api.dto.MergeStrategy;
import ru.aritmos.dtt.api.dto.branch.BranchEquipmentExportRequest;
import ru.aritmos.dtt.json.branch.BranchEquipment;

BranchEquipment branchEquipment = facade.parseBranchJson(
        Files.readString(Path.of("DeviceManager.json"))
);

byte[] zipBytes = facade.exportBranchToDttZip(new BranchEquipmentExportRequest(
        branchEquipment,
        List.of("branch-01"),
        List.of(),
        MergeStrategy.MERGE_NON_NULLS,
        "2.1.0"
));
```

### Когда лучше использовать именно batch API

- когда ваша служба принимает файл от пользователя;
- когда ваша служба скачивает zip из другого сервиса;
- когда вам нужен транспортный формат «набор DTT как одно вложение»;
- когда вам не нужно вручную трогать каждый шаблон перед импортом.

## 6. Extension points и практические шаблоны интеграции

### 6.1. Паттерн «каталог шаблонов + сборщик конфигурации»

Служба хранит DTT в каталоге/БД и по запросу собирает profile/branch.

Рекомендуемый pipeline:

1. Загрузить архивы из каталога.
2. Для каждого шаблона выполнить `readDtt(...)` и `validate(...)` при загрузке.
3. При запросе клиента выбрать нужные шаблоны.
4. Преобразовать их в `DeviceTypeTemplate` или напрямую собрать `DttArchiveTemplate` → производные.
5. Вызвать `assembleProfile(...)` или `assembleBranch(...)`.

### 6.2. Паттерн «предпросмотр перед импортом»

Если служба должна сначала показать пользователю ожидаемый результат, используйте `preview...` методы.

Пример:

```java
BranchEquipment preview = facade.previewDttZipToBranch(
        zipPayload,
        List.of("branch-01"),
        MergeStrategy.FAIL_IF_EXISTS
);
```

Это удобно, если вы:

- показываете diff пользователю;
- валидируете конфигурацию в UI;
- строите approval workflow;
- хотите избежать немедленной фиксации импортируемого набора.

### 6.3. Паттерн «строковый JSON на границе, типизированная модель внутри»

Если ваша служба общается по REST строковым JSON, а внутри хочет типы, используйте:

- `parseProfileJson(...)`
- `parseBranchJson(...)`
- `toProfileJson(...)`
- `toBranchJson(...)`

Это лучший компромисс между внешним контрактом и безопасной внутренней моделью.

## 7. Сценарий “один DTT → несколько производных типов устройств / наборов устройств”

Это ключевой прикладной кейс, который вы отдельно просили: есть **один** DTT, например `Display WD3264.dtt`, но из него нужно получить несколько производных типов устройств:

- `Display WD3264 Красное окно`
- `Display WD3264 Синее окно`

Они отличаются:

- metadata типа устройства;
- значениями параметров типа устройства;
- наборами устройств и значениями параметров устройств.

## 7.1. Почему здесь не стоит ограничиваться одним импортом “как есть”

Если импортировать один и тот же DTT несколько раз без отдельного клонирования metadata, вы всё равно будете получать один и тот же `deviceTypeId`, а значит упрётесь в merge-стратегии и конфликты идентичности.

Поэтому правильный путь:

1. клонировать базовый DTT;
2. сменить `metadata.id/name/displayName/description`;
3. при необходимости изменить default/example values;
4. записать каждый производный шаблон как отдельный DTT;
5. уже затем импортировать их как разные типы.

## 7.2. Пошаговый library-only пример

### Шаг 1. Прочитать базовый DTT и сделать производный “Красное окно”

```java
import ru.aritmos.dtt.api.DeviceTemplateLibrary;
import ru.aritmos.dtt.api.DeviceTemplateLibraryFacade;
import ru.aritmos.dtt.api.dto.DeviceTypeMetadata;
import ru.aritmos.dtt.archive.model.DttArchiveTemplate;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;

DeviceTemplateLibraryFacade facade = DeviceTemplateLibrary.createDefaultFacade();
byte[] baseBytes = Files.readAllBytes(Path.of("Display WD3264.dtt"));
DttArchiveTemplate base = facade.readDtt(baseBytes);

Map<String, Object> redDefaults = new LinkedHashMap<>(base.defaultValues());
redDefaults.put("FirstZoneColor", "red");
redDefaults.put("TicketZone", "5");

Map<String, Object> redExamples = new LinkedHashMap<>(base.exampleValues());
redExamples.put("FirstZoneColor", "red");

DttArchiveTemplate red = new DttArchiveTemplate(
        base.descriptor(),
        new DeviceTypeMetadata(
                "display-wd3264-red-window",
                "Display WD3264 Красное окно",
                "Display WD3264 Красное окно",
                "Производный шаблон для красного окна"
        ),
        base.deviceTypeParametersSchema(),
        base.deviceParametersSchema(),
        base.bindingHints(),
        redDefaults,
        redExamples,
        base.templateOrigin(),
        base.onStartEvent(),
        base.onStopEvent(),
        base.onPublicStartEvent(),
        base.onPublicFinishEvent(),
        base.deviceTypeFunctions(),
        base.eventHandlers(),
        base.commands()
);

byte[] redBytes = facade.writeDtt(red);
```

### Шаг 2. Аналогично сделать “Синее окно”

```java
Map<String, Object> blueDefaults = new LinkedHashMap<>(base.defaultValues());
blueDefaults.put("FirstZoneColor", "blue");
blueDefaults.put("TicketZone", "6");

Map<String, Object> blueExamples = new LinkedHashMap<>(base.exampleValues());
blueExamples.put("FirstZoneColor", "blue");

DttArchiveTemplate blue = new DttArchiveTemplate(
        base.descriptor(),
        new DeviceTypeMetadata(
                "display-wd3264-blue-window",
                "Display WD3264 Синее окно",
                "Display WD3264 Синее окно",
                "Производный шаблон для синего окна"
        ),
        base.deviceTypeParametersSchema(),
        base.deviceParametersSchema(),
        base.bindingHints(),
        blueDefaults,
        blueExamples,
        base.templateOrigin(),
        base.onStartEvent(),
        base.onStopEvent(),
        base.onPublicStartEvent(),
        base.onPublicFinishEvent(),
        base.deviceTypeFunctions(),
        base.eventHandlers(),
        base.commands()
);

byte[] blueBytes = facade.writeDtt(blue);
```

### Шаг 3. Собрать branch equipment из производных типов напрямую в библиотеке

```java
import ru.aritmos.dtt.api.dto.DeviceTypeTemplate;
import ru.aritmos.dtt.api.dto.EquipmentProfileDeviceTypeRequest;
import ru.aritmos.dtt.api.dto.MergeStrategy;
import ru.aritmos.dtt.api.dto.branch.BranchDeviceTypeImportRequest;
import ru.aritmos.dtt.api.dto.branch.BranchEquipmentAssemblyRequest;
import ru.aritmos.dtt.api.dto.branch.BranchImportRequest;
import ru.aritmos.dtt.api.dto.branch.DeviceInstanceImportRequest;
import ru.aritmos.dtt.json.branch.BranchEquipment;

import java.util.List;
import java.util.Map;

DeviceTypeTemplate redTemplate = new DeviceTypeTemplate(
        red.metadata(),
        Map.of(
                "FirstZoneColor", "red",
                "TicketZone", "5"
        )
);
DeviceTypeTemplate blueTemplate = new DeviceTypeTemplate(
        blue.metadata(),
        Map.of(
                "FirstZoneColor", "blue",
                "TicketZone", "6"
        )
);

BranchEquipment branchEquipment = facade.assembleBranch(new BranchEquipmentAssemblyRequest(
        List.of(new BranchImportRequest(
                "branch-custom",
                "Отделение custom",
                List.of(
                        new BranchDeviceTypeImportRequest(
                                new EquipmentProfileDeviceTypeRequest(redTemplate, true),
                                List.of(
                                        new DeviceInstanceImportRequest(
                                                "red-1",
                                                "red-1",
                                                "Red 1",
                                                "Красный дисплей 1",
                                                Map.of("IP", "10.10.10.11", "Port", 22224)
                                        ),
                                        new DeviceInstanceImportRequest(
                                                "red-2",
                                                "red-2",
                                                "Red 2",
                                                "Красный дисплей 2",
                                                Map.of("IP", "10.10.10.12", "Port", 22224)
                                        )
                                ),
                                "Display",
                                null,
                                null,
                                null,
                                null,
                                null,
                                Map.of(),
                                Map.of()
                        ),
                        new BranchDeviceTypeImportRequest(
                                new EquipmentProfileDeviceTypeRequest(blueTemplate, true),
                                List.of(
                                        new DeviceInstanceImportRequest(
                                                "blue-1",
                                                "blue-1",
                                                "Blue 1",
                                                "Синий дисплей 1",
                                                Map.of("IP", "10.10.10.21", "Port", 22224)
                                        ),
                                        new DeviceInstanceImportRequest(
                                                "blue-2",
                                                "blue-2",
                                                "Blue 2",
                                                "Синий дисплей 2",
                                                Map.of("IP", "10.10.10.22", "Port", 22224)
                                        ),
                                        new DeviceInstanceImportRequest(
                                                "blue-3",
                                                "blue-3",
                                                "Blue 3",
                                                "Синий дисплей 3",
                                                Map.of("IP", "10.10.10.23", "Port", 22224)
                                        )
                                ),
                                "Display",
                                null,
                                null,
                                null,
                                null,
                                null,
                                Map.of(),
                                Map.of()
                        )
                )
        )),
        MergeStrategy.FAIL_IF_EXISTS
));
```

### Шаг 4. Собрать профиль оборудования из тех же производных типов

```java
import ru.aritmos.dtt.api.dto.EquipmentProfileAssemblyRequest;
import ru.aritmos.dtt.json.profile.EquipmentProfile;

EquipmentProfile profile = facade.assembleProfile(new EquipmentProfileAssemblyRequest(
        List.of(
                new EquipmentProfileDeviceTypeRequest(redTemplate, true),
                new EquipmentProfileDeviceTypeRequest(blueTemplate, true)
        ),
        List.of(),
        MergeStrategy.FAIL_IF_EXISTS
));
```

## 7.3. JSON-описание того же прикладного кейса

Ниже — пример того, как ваша служба **может** описывать производные типы во внутреннем JSON-конфиге перед преобразованием в DTO библиотеки:

```json
{
  "derivedDeviceTypes": [
    {
      "templateFile": "Display WD3264.dtt",
      "deviceType": {
        "id": "display-wd3264-red-window",
        "name": "Display WD3264 Красное окно",
        "displayName": "Display WD3264 Красное окно",
        "description": "Производный шаблон для красного окна"
      },
      "deviceTypeParamValues": {
        "FirstZoneColor": "red",
        "TicketZone": "5"
      },
      "devices": [
        {
          "id": "red-1",
          "name": "red-1",
          "displayName": "Red 1",
          "deviceParamValues": {
            "IP": "10.10.10.11",
            "Port": 22224
          }
        },
        {
          "id": "red-2",
          "name": "red-2",
          "displayName": "Red 2",
          "deviceParamValues": {
            "IP": "10.10.10.12",
            "Port": 22224
          }
        }
      ]
    },
    {
      "templateFile": "Display WD3264.dtt",
      "deviceType": {
        "id": "display-wd3264-blue-window",
        "name": "Display WD3264 Синее окно",
        "displayName": "Display WD3264 Синее окно",
        "description": "Производный шаблон для синего окна"
      },
      "deviceTypeParamValues": {
        "FirstZoneColor": "blue",
        "TicketZone": "6"
      },
      "devices": [
        {
          "id": "blue-1",
          "name": "blue-1",
          "displayName": "Blue 1",
          "deviceParamValues": {
            "IP": "10.10.10.21",
            "Port": 22224
          }
        },
        {
          "id": "blue-2",
          "name": "blue-2",
          "displayName": "Blue 2",
          "deviceParamValues": {
            "IP": "10.10.10.22",
            "Port": 22224
          }
        },
        {
          "id": "blue-3",
          "name": "blue-3",
          "displayName": "Blue 3",
          "deviceParamValues": {
            "IP": "10.10.10.23",
            "Port": 22224
          }
        }
      ]
    }
  ]
}
```

## 7.4. Пошаговый REST-вариант того же сценария

Хотя основной акцент документа — библиотечная интеграция, ниже приведён и REST-вариант, если ваша служба всё же хочет поднимать HTTP-обёртку над библиотекой.

### Вариант для branch equipment

`metadataJson` для multipart-импорта может выглядеть так:

```json
{
  "mergeStrategy": "FAIL_IF_EXISTS",
  "branches": [
    {
      "branchId": "branch-custom",
      "displayName": "Отделение custom",
      "deviceTypes": [
        {
          "archiveEntryName": "Display WD3264 Красное окно.dtt",
          "deviceTypeParamValues": {
            "FirstZoneColor": "red",
            "TicketZone": "5"
          },
          "metadataOverride": {
            "id": "display-wd3264-red-window",
            "name": "Display WD3264 Красное окно",
            "displayName": "Display WD3264 Красное окно",
            "description": "Производный тип для красного окна"
          },
          "devices": [
            {
              "id": "red-1",
              "name": "red-1",
              "displayName": "Red 1",
              "deviceParamValues": {
                "IP": "10.10.10.11",
                "Port": 22224
              }
            },
            {
              "id": "red-2",
              "name": "red-2",
              "displayName": "Red 2",
              "deviceParamValues": {
                "IP": "10.10.10.12",
                "Port": 22224
              }
            }
          ]
        },
        {
          "archiveEntryName": "Display WD3264 Синее окно.dtt",
          "deviceTypeParamValues": {
            "FirstZoneColor": "blue",
            "TicketZone": "6"
          },
          "metadataOverride": {
            "id": "display-wd3264-blue-window",
            "name": "Display WD3264 Синее окно",
            "displayName": "Display WD3264 Синее окно",
            "description": "Производный тип для синего окна"
          },
          "devices": [
            {
              "id": "blue-1",
              "name": "blue-1",
              "displayName": "Blue 1",
              "deviceParamValues": {
                "IP": "10.10.10.21",
                "Port": 22224
              }
            },
            {
              "id": "blue-2",
              "name": "blue-2",
              "displayName": "Blue 2",
              "deviceParamValues": {
                "IP": "10.10.10.22",
                "Port": 22224
              }
            },
            {
              "id": "blue-3",
              "name": "blue-3",
              "displayName": "Blue 3",
              "deviceParamValues": {
                "IP": "10.10.10.23",
                "Port": 22224
              }
            }
          ]
        }
      ]
    }
  ]
}
```

Пример запроса:

```bash
curl -X POST "http://localhost:8080/api/dtt/import/branch/upload/multipart"   -H "accept: application/json"   -H "Content-Type: multipart/form-data"   -F "zipPayload=@display-derived-set.zip;type=application/zip"   -F "metadataJson={...валидный JSON как выше...}"
```

### Вариант для profile

```json
{
  "mergeStrategy": "FAIL_IF_EXISTS",
  "deviceTypes": [
    {
      "archiveEntryName": "Display WD3264 Красное окно.dtt",
      "deviceTypeParamValues": {
        "FirstZoneColor": "red",
        "TicketZone": "5"
      },
      "metadataOverride": {
        "id": "display-wd3264-red-window",
        "name": "Display WD3264 Красное окно",
        "displayName": "Display WD3264 Красное окно",
        "description": "Профильный производный тип для красного окна"
      }
    },
    {
      "archiveEntryName": "Display WD3264 Синее окно.dtt",
      "deviceTypeParamValues": {
        "FirstZoneColor": "blue",
        "TicketZone": "6"
      },
      "metadataOverride": {
        "id": "display-wd3264-blue-window",
        "name": "Display WD3264 Синее окно",
        "displayName": "Display WD3264 Синее окно",
        "description": "Профильный производный тип для синего окна"
      }
    }
  ]
}
```

Для export-сценариев передавайте во входном `profileJson`/`branchJson` корневую секцию `metadata` (список или один объект в списке) вместе с целевыми `deviceTypes`: это сохраняет согласованность между реальным JSON-парсером, OpenAPI-примерами и round-trip `JSON -> DTT -> JSON`.

Пример запроса:

```bash
curl -X POST "http://localhost:8080/api/dtt/import/profile/upload/multipart"   -H "accept: application/json"   -H "Content-Type: multipart/form-data"   -F "zipPayload=@display-derived-set.zip;type=application/zip"   -F "metadataJson={...валидный JSON как выше...}"
```

### 7.5. Чек-лист качества OpenAPI примеров для вашей службы

При сопровождении demo-service/OpenAPI проверяйте следующие инварианты:

1. Во всех export-входах (`profileJson`, `branchJson`, `branchEquipment`) есть root-level `metadata`.
2. У каждого metadata-объекта заполнены:
   - `id`
   - `name`
   - `displayName`
   - `description`
   - `imageBase64` (PNG Base64)
3. Upload `metadataJson` примеры содержат не только `mergeStrategy`, но и:
   - `metadataOverride` для типов;
   - override значений `deviceTypeParamValues`;
   - override значений `devices[].deviceParamValues` для branch сценариев.
4. Примеры проходят автопроверку тестами, а не только «выглядят корректно».

Рекомендуемые команды проверки (Maven Wrapper):

```bash
./mvnw -Dmaven.repo.local=.m2/repository clean verify
```

или таргетированно для OpenAPI/контрактов demo-service:

```bash
./mvnw -Dmaven.repo.local=.m2/repository \
  -pl device-template-demo-service -am test \
  -Dtest=OpenApiSpecContractTest,DttControllerExamplesContractTest,DttSwaggerExamplesTest \
  -Dsurefire.failIfNoSpecifiedTests=false
```


### 7.6. Настройка default-иконки типа устройства (`defaultDTTIcon`)

В demo-service можно централизованно задать PNG-иконку по умолчанию через `application.yml`:

```yaml
dtt:
  defaultDTTIcon: "<BASE64_PNG>"
```

Где используется это значение:

- `import .dtt -> profile JSON`: если в архиве нет `icon.png`, в metadata будет подставлена default-иконка.
- `import .dtt -> branch equipment JSON`: аналогично, `imageBase64` заполняется fallback-иконкой.
- `export profile/branch -> .dtt`: если в metadata не передан `imageBase64`, в архив всё равно будет записан `icon.png` из `defaultDTTIcon`.

Практические рекомендации:

1. Сохраняйте значение **только** как Base64 PNG (без data URI префикса).
2. Валидируйте строку на старте сервиса: невалидный Base64 должен считаться ошибкой конфигурации.
3. Для корпоративных поставок храните canonical default-иконку в отдельном конфигурационном репозитории и подключайте через переменные окружения/секреты.

Пример JSON metadata с явной иконкой (перекрывает fallback):

```json
{
  "id": "display-wd3264",
  "name": "Display WD3264",
  "displayName": "Display WD3264",
  "description": "Базовый дисплей",
  "imageBase64": "iVBORw0KGgoAAAANSUhEUgAA..."
}
```

Если `imageBase64` не передан, fallback порядок остаётся таким: входной JSON -> `icon.png` в DTT -> `dtt.defaultDTTIcon` -> встроенная библиотечная иконка.

## 8. Примечания по проектированию своей службы

### 8.1. Где лучше держать собственную доменную модель

Лучше не строить архитектуру так, чтобы ваши контроллеры/обработчики работали напрямую с DTO библиотеки. На практике надёжнее держать:

- свою доменную модель запроса/конфигурации;
- адаптер, преобразующий её в DTO библиотеки;
- библиотеку как отдельный трансформационный слой.

Так проще:

- менять внешний REST контракт;
- валидировать прикладные бизнес-правила отдельно от DTT;
- тестировать преобразование независимо от самой библиотеки.

### 8.2. Где библиотека особенно полезна

Она особенно хорошо подходит для служб, которые:

- импортируют или экспортируют DeviceManager-конфигурации;
- управляют каталогом шаблонов оборудования;
- хранят DTT как переносимый артефакт между стендами/контурами;
- строят производные типы устройств из эталонных шаблонов.

### 8.3. Anti-patterns

- Не копируйте код из demo-service вместо использования фасада.
- Не редактируйте YAML/Groovy внутри zip вручную без `validate(...)`.
- Не делайте `REPLACE` default-стратегией для массового импорта.
- Не превращайте DTT в единственный runtime-источник истины для branch-конфигурации.
- Не смешивайте ответственность “каталог шаблонов” и “runtime-конфиг отделений” в одной сущности без явного слоя адаптации.

## 9. Что имеет смысл покрыть тестами в своей службе

Минимальный прикладной набор:

1. Импорт одного DTT с позитивным сценарием.
2. Импорт невалидного DTT с проверкой диагностики.
3. Сборка profile из нескольких шаблонов.
4. Сборка branch equipment с несколькими branch.
5. Экспорт `DeviceManager.json -> DTT zip`.
6. Round-trip `DTT -> branch -> DTT`.
7. Сценарий одного базового DTT и нескольких производных типов.
8. Поведение merge-стратегий при повторном импорте.

Отдельно в библиотеке уже есть эталонный unit-тест canonical mapping на nullable + nested array/object metadata:

- `device-template-library/src/test/java/ru/aritmos/dtt/model/mapping/DefaultCanonicalTemplateMapperTest`
  (`shouldPreserveNullableAndNestedArrayObjectMetadataOnRoundTrip`).

Дополнительно проверяется проекционный кейс: даже если у `Array`-параметра нет default value, в profile projection сохраняется `items`-схема (включая nested `parametersMap` и nullable metadata) — см. `DefaultCanonicalProjectionMapperTest#shouldKeepArrayItemsSchemaWhenArrayValueIsMissing`.

## 10. Навигация по связанным материалам

- Корневое описание проекта: [`../README.md`](../README.md)
- План развития и технический TODO: [`project-todo.md`](project-todo.md)
- PlantUML-исходники: [`plantuml`](plantuml)
- SVG-диаграммы: [`plantuml/svg`](plantuml/svg)
- Референсный demo-service: `../device-template-demo-service`

## 11. Расширенный cookbook интеграции (production-практика)

Ниже собраны типовые «боевые» сценарии, которые обычно появляются после первого успешного прототипа.

### 11.1. Сценарий «предпросмотр + подтверждение + применение»

Рекомендуемый pipeline для UI и approval-процессов:

1. Пользователь загружает zip с `.dtt`.
2. Сервис вызывает `previewDttZipToProfile(...)` или `previewDttZipToBranch(...)`.
3. UI показывает diff/результат и предупреждения.
4. После подтверждения вызывается соответствующий `import...` метод.
5. Результат сохраняется в БД/конфигурационное хранилище.

Практическая польза:

- конфликт виден заранее;
- можно внедрить роль «ревьюер» перед применением;
- меньше rollback-сценариев.

### 11.2. Сценарий «merge в существующий branch JSON»

Если ваша система хранит «живой» `DeviceManager.json`, используйте:

- `importDttSetToExistingBranch(...)`;
- `importDttBase64SetToExistingBranch(...)`;
- `importDttZipToExistingBranch(...)`.

Рекомендация: для nightly/batch jobs обычно безопаснее `MERGE_PRESERVE_EXISTING`, а `REPLACE` оставлять для ручного административного режима.

### 11.3. Сценарий «JSON на входе, типы внутри»

Удобный подход для REST-сервисов:

1. На входе принимаете JSON как строку.
2. Парсите через `parseProfileJson(...)` / `parseBranchJson(...)`.
3. Выполняете бизнес-валидацию на типизированной модели.
4. После модификаций сериализуете через `toProfileJson(...)` / `toBranchJson(...)`.

Так вы избегаете ручного `Map<String, Object>` и получаете предсказуемое поведение сериализации.

## 12. Обработка ошибок и наблюдаемость

### 12.1. Какие исключения использовать в API-службе

В адаптере/контроллере полезно разделять ошибки:

- `DttFormatException` → 400 Bad Request (некорректный архив/структура);
- `TemplateValidationException` → 422 Unprocessable Entity (формат валиден, но нарушены правила);
- `TemplateImportException` / `TemplateExportException` → 409/500 в зависимости от причины;
- `TemplateAssemblyException` → 409 Conflict (чаще merge-конфликт) или 500 (ошибка сборки).

Для машинной обработки ошибок в demo API введены стабильные `code` значения:

- `INVALID_ARGUMENT`;
- `DTT_FORMAT_ERROR`;
- `TEMPLATE_VALIDATION_ERROR`;
- `TEMPLATE_IMPORT_ERROR`;
- `TEMPLATE_EXPORT_ERROR`;
- `TEMPLATE_ASSEMBLY_ERROR`;
- `INTERNAL_ERROR`.

Текущее поведение demo-service по HTTP статусам:

- `INVALID_ARGUMENT`, `DTT_FORMAT_ERROR` → `400 Bad Request`;
- `TEMPLATE_VALIDATION_ERROR` → `422 Unprocessable Entity`;
- `TEMPLATE_IMPORT_ERROR`, `TEMPLATE_EXPORT_ERROR`, `TEMPLATE_ASSEMBLY_ERROR` → `409 Conflict`;
- `INTERNAL_ERROR` → `500 Internal Server Error`.

Также эти статусы и `DemoErrorResponse` описаны в OpenAPI как стандартные error-response для операций `DttController`, чтобы клиенты могли опираться на единый контракт.

### 12.2. Что логировать обязательно

Минимальные поля для диагностики:

- `operation` (`import_profile`, `export_branch_all`, `preview_branch` и т.д.);
- `mergeStrategy`;
- `branchIds`;
- `deviceTypeIds` (если применимы фильтры);
- `archiveCount` / `archiveSizeBytes`;
- `requestId` / `correlationId`.

Не логируйте полный Groovy-код и полные Base64 payload в INFO/ERROR логах.

### 12.3. Метрики, которые реально полезны

- количество импортов/экспортов по типам операций;
- доля preview, завершившихся apply-операцией;
- число merge-конфликтов по стратегиям;
- среднее время `import zip -> branch` и `export branch -> zip`;
- количество невалидных DTT.

## 13. Чеклист релиза изменений для разработчика

Используйте этот список как «Definition of Done» для задач, затрагивающих формат или API:

1. Пройдены модульные тесты библиотеки и demo-service.
2. Для новых сценариев добавлены контрактные примеры в OpenAPI.
3. README и `docs/developer-guide.md` синхронизированы с кодом.
4. В `manifest.yml` не добавлены обязательные поля без версии/совместимости.
5. Проверено, что Groovy-скрипты не модифицируются при round-trip.
6. Проверены минимум два merge-сценария: конфликтный и бесконфликтный.
7. Для batch-сценариев проверен импорт/экспорт zip и Base64.

## 14. Минимальный onboarding-план для нового разработчика команды

Если в команду пришёл новый инженер, обычно достаточно такого маршрута:

1. Прочитать `README.md` и разделы 1–6 этого гайда.
2. Запустить `./mvnw -Dmaven.repo.local=.m2/repository clean verify`.
3. Поднять demo-service и вручную прогнать 2–3 endpoint-а через Swagger.
4. Выполнить локальный сценарий round-trip:
   - взять один `.dtt`;
   - импортировать в profile;
   - экспортировать обратно;
   - сравнить ключевые YAML + scripts.
5. Реализовать небольшое изменение только в одном направлении (например, export profile -> dtt) и покрыть тестом.

Такой onboarding обычно даёт достаточное понимание архитектуры, формата и точек расширения за 1 рабочий день.

## 15. Гигиена исходников и контроль артефактов

В проекте добавлены автоматические проверки `SourceTreeHygieneTest` (для demo-service и для device-template-library), которые контролируют, что в `src/main/java` не попадают:

- `.class` файлы;
- временные артефакты (`.tmp`, `.bak`);
- служебные файлы вроде `tmpfile.txt`.

Практический смысл проверки:

1. предотвращает случайные коммиты бинарных файлов в исходники;
2. снижает «шум» в pull request и риск конфликтов в репозитории;
3. повышает воспроизводимость сборки и прозрачность ревью.

## 5.3. Алгоритм B1: один DTT -> несколько производных типов устройств

Этот сценарий нужен, когда один исходный шаблон должен превратиться в несколько разных типов устройств, отличающихся metadata, значениями параметров типа устройства и набором экземпляров устройств.

Пример задачи: из одного DTT `Display WD3264` сформировать два отдельных производных типа — `Display WD3264 Красное окно` и `Display WD3264 Синее окно`, у которых различаются `deviceTypeId`, название, описание, значения зон и список дисплеев.

### Что важно

1. Исходный DTT остаётся одним и тем же.
2. Производные типы становятся разными типами устройств уже на уровне итогового profile/branch JSON.
3. Для этого используются `metadataOverride` и override значений параметров.
4. Для branch-сценария можно ещё и задать разные списки устройств через override экземпляров устройств.

### Пример для profile

```java
ProfileImportPlanRequest plan = new ProfileImportPlanRequest(
        List.of(),
        MergeStrategy.FAIL_IF_EXISTS,
        List.of(
                new ProfileDeviceTypeImportSourceRequest(
                        redArchiveBase64,
                        null,
                        Map.of("TicketZone", "3", "ServicePointNameZone", "1"),
                        new DeviceTypeMetadata(
                                "display-red-window",
                                "Display WD3264 Красное окно",
                                "Display WD3264 Красное окно",
                                "Профиль красного окна",
                                "2.1.0",
                                null
                        )
                ),
                new ProfileDeviceTypeImportSourceRequest(
                        blueArchiveBase64,
                        null,
                        Map.of("TicketZone", "4", "ServicePointNameZone", "1"),
                        new DeviceTypeMetadata(
                                "display-blue-window",
                                "Display WD3264 Синее окно",
                                "Display WD3264 Синее окно",
                                "Профиль синего окна",
                                "2.1.0",
                                null
                        )
                )
        )
);

EquipmentProfile profile = facade.assembleProfile(facade.prepareProfileAssemblyRequest(plan));
```

### Пример для branch

```java
BranchImportPlanRequest plan = new BranchImportPlanRequest(
        null,
        null,
        MergeStrategy.FAIL_IF_EXISTS,
        List.of(new BranchImportSourceRequest(
                "branch-1",
                "Отделение 1",
                List.of(
                        new BranchDeviceTypeImportSourceRequest(
                                redArchiveBase64,
                                null,
                                Map.of("TicketZone", "3"),
                                new DeviceTypeMetadata("display-red-window", "Display WD3264 Красное окно", "Display WD3264 Красное окно", "Красный профиль", "2.1.0", null),
                                List.of(
                                        new DeviceInstanceImportRequest("red-1", "display red 1", "display red 1", "Красный дисплей 1", Map.of("IP", "10.0.0.11", "Port", 22224)),
                                        new DeviceInstanceImportRequest("red-2", "display red 2", "display red 2", "Красный дисплей 2", Map.of("IP", "10.0.0.12", "Port", 22224))
                                ),
                                "display"
                        ),
                        new BranchDeviceTypeImportSourceRequest(
                                blueArchiveBase64,
                                null,
                                Map.of("TicketZone", "4"),
                                new DeviceTypeMetadata("display-blue-window", "Display WD3264 Синее окно", "Display WD3264 Синее окно", "Синий профиль", "2.1.0", null),
                                List.of(
                                        new DeviceInstanceImportRequest("blue-1", "display blue 1", "display blue 1", "Синий дисплей 1", Map.of("IP", "10.0.0.21", "Port", 22224)),
                                        new DeviceInstanceImportRequest("blue-2", "display blue 2", "display blue 2", "Синий дисплей 2", Map.of("IP", "10.0.0.22", "Port", 22224)),
                                        new DeviceInstanceImportRequest("blue-3", "display blue 3", "display blue 3", "Синий дисплей 3", Map.of("IP", "10.0.0.23", "Port", 22224))
                                ),
                                "display"
                        )
                )
        ))
);

BranchEquipment branchEquipment = facade.assembleBranch(facade.prepareBranchAssemblyRequest(plan));
```

В этом сценарии merge-стратегия не создаёт производные типы сама. Производные типы создаются за счёт того, что вы явно задаёте разные metadata override и тем самым получаете разные `deviceTypeId`. Merge-стратегия вступает в дело только тогда, когда итоговые `deviceTypeId` всё-таки конфликтуют.


## 11. Частые ошибки интеграции и ожидаемое поведение библиотеки

### 11.1. Ошибка: в structured import-plan указан `archiveEntryName`, но запрос пришёл не через zip

Сценарий:
- сервис формирует `ProfileImportPlanRequest` или `BranchImportPlanRequest`;
- в элементе типа устройства заполнен `archiveEntryName`;
- вызван `prepare*AssemblyRequest(...)`, а не `prepare*AssemblyRequestFromZip(...)`.

Ожидаемое поведение:
- библиотека завершит подготовку импорта с `IllegalArgumentException`;
- причина: `archiveEntryName` допустим только в zip-контексте.

### 11.2. Ошибка: и `archivesBase64`, и structured-список пусты

Сценарий:
- сервис передал пустой план импорта без архивов и без списка типов устройств.

Ожидаемое поведение:
- библиотека завершит вызов `prepareProfileAssemblyRequest(...)` или `prepareBranchAssemblyRequest(...)` с `IllegalArgumentException`;
- assembly-request не будет создан.

### 11.3. Ошибка: в branch-plan не указан `branchId`

Сценарий:
- в одном из `BranchImportSourceRequest` отсутствует `branchId` или передана пустая строка.

Ожидаемое поведение:
- библиотека завершит подготовку branch-импорта с `IllegalArgumentException`;
- сообщение будет указывать на обязательность `branchId`.

### 11.4. Ошибка: в zip нет ни одного `.dtt`

Сценарий:
- multipart upload пришёл, но внутри zip отсутствуют DTT-файлы;
- вызван `prepareProfileAssemblyRequestFromZip(...)` или `prepareBranchAssemblyRequestFromZip(...)`.

Ожидаемое поведение:
- библиотека завершит вызов с `IllegalArgumentException`;
- итоговая profile/branch модель не строится.

### 11.5. Ошибка: конфликт итоговых `deviceTypeId`

Сценарий:
- два производных типа после metadata override получили одинаковый `deviceTypeId`;
- merge-стратегия не допускает такое объединение.

Ожидаемое поведение:
- на этапе `assembleProfile(...)`, `assembleBranch(...)` или merge-операции библиотека применит выбранную merge-стратегию;
- при `FAIL_IF_EXISTS` будет выброшена ошибка конфликта;
- при `CREATE_COPY_WITH_SUFFIX` библиотека создаст копию конфликтующего типа с суффиксом.

### 11.6. Ошибка: разработчик ожидает, что merge-стратегия создаст производный тип автоматически

Сценарий:
- один и тот же DTT импортируется несколько раз;
- metadata override не меняет `id`;
- ожидается, что библиотека сама сделает «красный» и «синий» типы.

Ожидаемое поведение:
- merge-стратегия не создаёт производный доменный тип сама по себе;
- для появления разных типов устройств нужно явно задать разные metadata override, прежде всего разные `deviceTypeId`.

## 9. Новый orchestration-слой high-level facade API

На текущем этапе библиотека поддерживает уже не только подготовку import-plan, но и его прямое исполнение на уровне фасада.

### 9.1 Методы

- `assembleProfile(ProfileImportPlanRequest)`
- `assembleProfile(byte[] zipPayload, ProfileImportPlanRequest)`
- `assembleBranch(BranchImportPlanRequest)`
- `assembleBranch(byte[] zipPayload, BranchImportPlanRequest)`
- `mergeIntoExistingBranch(BranchEquipment, BranchImportPlanRequest)`
- `mergeIntoExistingBranch(byte[] zipPayload, BranchEquipment, BranchImportPlanRequest)`
- `mergeIntoExistingBranchJson(String, BranchImportPlanRequest)`
- `mergeIntoExistingBranchJson(byte[] zipPayload, String, BranchImportPlanRequest)`

### 9.2 Зачем этот слой нужен

Этот слой убирает из прикладной службы повторяющийся orchestration-код:

- decode Base64;
- resolve zip-entry;
- `prepare...AssemblyRequest(...)`;
- `assemble...(...)`;
- parse existing JSON;
- merge incoming model в existing model.

В результате demo-service и любая интеграционная служба могут использовать библиотеку как готовый движок прикладных операций, а не только как набор низкоуровневых примитивов.

### 9.3 Рекомендуемая граница ответственности

**Библиотека** должна отвечать за:

- доменную интерпретацию `.dtt`;
- import/export/mapping/merge;
- branch-specific topology;
- metadata override;
- device override;
- preview-диагностику.

**Прикладная служба** должна отвечать за:

- HTTP-контракт;
- авторизацию;
- ограничение размера payload;
- аудит вызовов;
- сохранение итогового JSON или файлов в инфраструктурные хранилища.

## 10. Полный каталог фич и кейсов

Подробное описание всех фич, терминов, merge-стратегий, подкатегорий кейсов и примеров использования вынесено в отдельный документ:

- `docs/feature-catalog.md`

Его стоит считать основным навигационным документом по сценариям библиотеки. `README.md` остаётся обзорным входом, а этот guide — документом о внутренней архитектуре и слоях API.


## Совместная сборка profile и branch с наследованием metadata

Для сценария, где требуется одним вызовом собрать `EquipmentProfile` и `BranchEquipment`, библиотека теперь предоставляет `ProfileBranchMetadataImportPlanRequest` и метод фасада `assembleProfileAndBranchWithMetadata(...)`.

Этот слой нужен для того, чтобы прикладная служба не реализовывала вручную:

- чтение DTT для определения исходного `deviceTypeId`;
- построение карты profile-level metadata override;
- построение карты branch-level metadata override;
- вызов низкоуровневого API с несколькими техническими map-структурами.

Теперь demo-service выполняет только преобразование входных DTO в DTO библиотеки.
