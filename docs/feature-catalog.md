# Полный каталог фич и сценариев использования библиотеки DeviceTypesImportExport

Документ описывает библиотеку как прикладной инструмент для реальных задач работы с настройками оборудования: импорт шаблонов типов устройств, сборка профилей оборудования, сборка конфигурации отделений, merge в существующую конфигурацию, preview-диагностика, экспорт обратно в `.dtt`, работа в Base64- и zip-режимах.

---

## 1. Термины

### 1.1 `.dtt`
`.dtt` — zip-архив одного типа устройства. Внутри лежат YAML-манифест, описание структуры параметров, значения по умолчанию, скрипты Groovy и служебные метаданные.

### 1.2 DTT-set
DTT-set — набор нескольких `.dtt`, который импортируется совместно. На практике это либо список byte[], либо список Base64-строк, либо zip-пакет, содержащий несколько `.dtt` entry.

### 1.3 Profile JSON
Profile JSON — карта типов устройств без branch-обёртки. Это то представление, которое соответствует профилю оборудования как набору типов устройств и их параметров.

### 1.4 Branch equipment JSON
Branch equipment JSON — полная конфигурация отделений. В ней есть branch-ы, внутри branch — `deviceTypes`, а внутри типов устройств — `devices` и их параметры.

### 1.5 Legacy-import
Legacy-import — сценарий, в котором один и тот же набор `.dtt` просто применяется ко всем указанным branchId без детальной branch-specific настройки каждого типа устройства.

### 1.6 Structured-import
Structured-import — сценарий, в котором вызывающая сторона явно перечисляет branch-ы, типы устройств, metadata override, override-значения параметров типа устройства, устройства и их параметры.

### 1.7 Metadata override
Metadata override — замещение `id`, `name`, `displayName`, `description`, `version`, `iconBase64` у типа устройства на этапе импорта.

### 1.8 Device instance override
Device instance override — замещение или добавление конкретных устройств внутри branch-специфичного типа устройства: `id`, `name`, `displayName`, `description`, `deviceParamValues`.

### 1.9 Merge-стратегия
Merge-стратегия — правило разрешения конфликта, когда библиотека сталкивается с одинаковым `deviceTypeId` или merge в существующую модель должен совместить старое и новое состояние.

Поддерживаемые стратегии:

- `FAIL_IF_EXISTS` — прекращает операцию при конфликте.
- `REPLACE` — входящее значение полностью замещает существующее.
- `MERGE_NON_NULLS` — непустые поля из incoming накладываются поверх existing.
- `MERGE_PRESERVE_EXISTING` — existing считается приоритетным, incoming заполняет только пробелы.
- `CREATE_COPY_WITH_SUFFIX` — при конфликте создаётся копия типа устройства с вычисленным суффиксом.

### 1.10 Preview
Preview — сухой прогон без отдельного режима хранения. В библиотеке это либо отдельные preview-методы, либо вычисление диагностических счётчиков до сборки (`compute...Preview(...)`), либо single-export preview.

### 1.11 Branch-specific topology
Branch-specific topology — branch-специфичное состояние типа устройства, сохранённое в `templateOrigin`. Если такое состояние присутствует, библиотека при structured branch-импорте может взять не generic template, а специальный снимок именно для конкретного branch.

### 1.12 High-level import-plan API
High-level import-plan API — уровень фасада, который принимает прикладной план импорта и сам:

- декодирует Base64,
- читает `.dtt`,
- разрешает нужные zip-entry,
- применяет metadata override,
- применяет override параметров,
- применяет override устройств,
- собирает типизированный assembly-request,
- а затем по новому orchestration API может сразу вернуть готовую модель.

### 1.13 ExportResult
`ExportResult` — типизированный результат single-export из фасада библиотеки, который сразу содержит:

- `deviceTypeId` — какой тип устройства был экспортирован;
- `archiveBytes` — бинарное содержимое `.dtt` для download/файловой выдачи;
- `archiveBase64` — строковое представление того же архива для JSON API и message-based интеграций.

### 1.14 DttInspectionResult
`DttInspectionResult` — типизированный результат быстрой инспекции `.dtt` из фасада библиотеки.

Он нужен, когда прикладному сервису требуется получить карточку шаблона без ручного разбора
`DttArchiveTemplate` и без дублирования полей в controller/service слое.

### 1.15 ProfileAssemblyView и BranchAssemblyView
`ProfileAssemblyView` и `BranchAssemblyView` — прикладные DTO фасада для случаев,
когда после сборки нужно сразу получить:

- сериализованный JSON;
- агрегированные счётчики (`deviceTypesCount` / `branchesCount`);
- metadata типов устройств (для branch-сценариев).

### 1.16 Нормализация metadata типов устройств
Нормализация metadata — это приведение `DeviceTypeMetadata` к отображаемому виду:

- заполнение fallback для `name`, `displayName`, `description`;
- нормализация `iconBase64` (дефолтная иконка, если исходная не задана);
- сохранение `id`/`version` без потери бизнес-смысла.

### 1.17 Контракт `formatVersion`
`formatVersion` — версия структуры DTT-архива.

Текущий валидируемый контракт библиотеки:

- поддерживаются версии формата `1.x`;
- версия `2.x` и выше считается неподдерживаемой и возвращает валидационную issue
  `UNSUPPORTED_FORMAT_VERSION` по пути `manifest.yml:formatVersion`.

---

## 2. Слои API библиотеки

### 2.1 Archive layer
Методы этого слоя работают с одним `.dtt` как с архивом:

- `readDtt(...)`
- `writeDtt(...)`
- `validate(...)`
- `inspectDtt(...)`
- `compareDttVersion(...)`
- `extractDeviceTypeMetadataFromDttOrZip(...)`
- `normalizeDeviceTypeMetadata(...)`
- `resolveDeviceTypeArchiveBaseName(...)`

Когда использовать:

- входящий файл нужно только прочитать и проверить;
- требуется построить карточку типа устройства для UI;
- нужно сравнить входную версию и версию из архива;
- нужен только один шаблон, а не сборка profile/branch.

### 2.2 Typed assembly layer
Методы этого слоя работают с уже подготовленными типизированными request DTO:

- `assembleProfile(EquipmentProfileAssemblyRequest)`
- `assembleBranch(BranchEquipmentAssemblyRequest)`
- `mergeBranchEquipment(...)`
- `parseProfileJson(...)` / `toProfileJson(...)`
- `parseBranchJson(...)` / `toBranchJson(...)`
- `toProfileAssemblyView(...)`
- `toBranchAssemblyView(...)`

Когда использовать:

- прикладная служба сама хранит шаблоны и строит request DTO вручную;
- нужно полное управление последовательностью сборки;
- нужна собственная доменная логика до вызова библиотеки.

### 2.3 High-level import-plan preparation layer
Методы подготавливают typed request из прикладного плана:

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
- `previewProfileImport(...)`
- `previewProfileImport(byte[] zipPayload, ...)`
- `previewBranchImport(...)`
- `previewBranchImport(byte[] zipPayload, ...)`

Когда использовать:

- прикладная служба хочет сначала посмотреть подготовленный request;
- нужно отладить import-plan и увидеть промежуточную структуру;
- требуется preview default/override counts до сборки;
- нужно одним вызовом получить и preview-модель, и диагностические счётчики;
- import-plan приходит в виде zip-пакета, а библиотека должна сама разрешить `archiveEntryName` и посчитать диагностику без ручной распаковки в прикладной службе.

### 2.4 High-level orchestration layer
Это новый уровень прямого исполнения import-plan, который убирает orchestration-код из demo-service:

- `assembleProfile(ProfileImportPlanRequest)`
- `assembleProfile(byte[] zipPayload, ProfileImportPlanRequest)`
- `assembleBranch(BranchImportPlanRequest)`
- `assembleBranch(byte[] zipPayload, BranchImportPlanRequest)`
- `previewProfileImport(ProfileImportPlanRequest)`
- `previewProfileImport(byte[] zipPayload, ProfileImportPlanRequest)`
- `previewBranchImport(BranchImportPlanRequest)`
- `previewBranchImport(byte[] zipPayload, BranchImportPlanRequest)`
- `mergeIntoExistingBranch(BranchEquipment, BranchImportPlanRequest)`
- `mergeIntoExistingBranch(byte[] zipPayload, BranchEquipment, BranchImportPlanRequest)`
- `mergeIntoExistingBranchJson(String, BranchImportPlanRequest)`
- `mergeIntoExistingBranchJson(byte[] zipPayload, String, BranchImportPlanRequest)`

Когда использовать:

- не нужен промежуточный `prepare...Request(...)`;
- нужен короткий и прямой API для прикладной службы;
- требуется импорт в существующий branch JSON без ручного parse + assemble + merge.

### 2.4.1 Что такое детальный preview импорта
Детальный preview импорта — это режим, в котором библиотека за один вызов возвращает:

1. уже собранную preview-модель (`EquipmentProfile` или `BranchEquipment`);
2. диагностическую карту, показывающую, сколько значений было рассчитано из defaults и сколько пришло как override.

Этот режим нужен, когда прикладная служба строит экран предварительного просмотра, формирует объяснимую диагностику для пользователя или принципиально хочет оставаться thin-adapter слоем без ручной склейки `assemble + computePreview`.

### 2.5 Export / preview layer
- `exportDttSetFromProfile*`
- `exportDttSetFromBranch*`
- `exportSingleDttFromProfile*`
- `exportSingleDttFromBranch*`
- `exportSingleDttResultFromProfile*`
- `exportSingleDttResultFromBranch*`
- `previewSingleDttExportFromProfile*`
- `previewSingleDttExportFromBranch*`
- `exportProfileToDttZip*`
- `exportBranchToDttZip*`

Когда использовать:

- нужно вернуть `.dtt` пользователю;
- нужно вернуть zip-набор `.dtt`;
- нужно проверить, экспортируется ли конкретный тип устройства, до фактической отдачи файла.

---

## 3. Карта фич по категориям

## 3.1 Чтение, запись и валидация одного шаблона

### Что покрыто
- чтение `.dtt` в `DttArchiveTemplate`;
- запись `DttArchiveTemplate` в bytes;
- валидация структуры, YAML и Groovy;
- получение версии шаблона;
- получение имени типа устройства для имени выходного файла.

### Пример

```java
DeviceTemplateLibraryFacade facade = DeviceTemplateLibrary.createDefaultFacade();

byte[] bytes = Files.readAllBytes(Path.of("Display WD3264.dtt"));
DttArchiveTemplate template = facade.readDtt(bytes);
ValidationResult validation = facade.validate(template);
String archiveBaseName = facade.resolveDeviceTypeArchiveBaseName(bytes, "device-type");
```

### Подкатегории кейсов
1. Проверка файла при upload.
2. Нормализация и повторная запись архива.
3. Сверка версии шаблона с тем, что передал клиент.
4. Получение карточки типа устройства для списка файлов в zip-пакете.

---

## 3.2 Извлечение metadata из одного `.dtt` или из zip-набора

### Что покрыто
- извлечение списка `DeviceTypeMetadata` из одного `.dtt`;
- извлечение списка `DeviceTypeMetadata` из zip, содержащего несколько `.dtt`;
- работа с nested entry (`nested/Display.dtt`).

### Пример

```java
List<DeviceTypeMetadata> cards = facade.extractDeviceTypeMetadataFromDttOrZip(zipPayload);
```

### Подкатегории кейсов
1. Предпросмотр содержимого zip до импорта.
2. Отображение списка шаблонов в UI.
3. Автоматический выбор `archiveEntryName` пользователем по карточке.

---

## 3.3 Импорт в profile JSON

### Режимы
1. bytes-режим — `importDttSetToProfile(...)`
2. Base64-режим — `importDttBase64SetToProfile(...)`
3. zip-режим — `importDttZipToProfile(...)`
4. high-level plan preparation — `prepareProfileAssemblyRequest(...)`
5. high-level plan orchestration — `assembleProfile(ProfileImportPlanRequest)`
6. high-level zip orchestration — `assembleProfile(byte[] zipPayload, ProfileImportPlanRequest)`
7. high-level preview orchestration — `previewProfileImport(...)`, `previewBranchImport(...)`

### Что покрыто
- импорт набора `.dtt`;
- override metadata;
- override значений параметров типа устройства;
- импорт всех `.dtt` из zip;
- импорт только выбранных zip-entry по `archiveEntryName`.

### Пример: один шаблон -> производный тип устройства

```java
ProfileImportPlanRequest plan = new ProfileImportPlanRequest(
        List.of(),
        MergeStrategy.FAIL_IF_EXISTS,
        List.of(new ProfileDeviceTypeImportSourceRequest(
                archiveBase64,
                null,
                Map.of(
                        "ServicePointNameZone", "1",
                        "TicketZone", "3"
                ),
                new DeviceTypeMetadata(
                        "display-wd3264-red-window",
                        "Display WD3264 Красное окно",
                        "Display WD3264 Красное окно",
                        "Профиль для красного окна",
                        "2.1.0",
                        null
                )
        ))
);

EquipmentProfile profile = facade.assembleProfile(plan);
```

### Пример: zip + выбор entry

```java
ProfileImportPlanRequest plan = new ProfileImportPlanRequest(
        null,
        MergeStrategy.FAIL_IF_EXISTS,
        List.of(new ProfileDeviceTypeImportSourceRequest(
                null,
                "nested/Display WD3264.dtt",
                Map.of("TicketZone", "4"),
                new DeviceTypeMetadata(
                        "display-wd3264-blue-window",
                        "Display WD3264 Синее окно",
                        "Display WD3264 Синее окно",
                        "Профиль для синего окна",
                        "2.2.0",
                        null
                )
        ))
);

EquipmentProfile profile = facade.assembleProfile(zipPayload, plan);
```

### Подкатегории кейсов
1. Один `.dtt` становится одним типом устройства.
2. Один `.dtt` становится несколькими производными типами устройства с разными `id/name/displayName`.
3. Один `.dtt` становится несколькими вариантами одного и того же класса оборудования с разными параметрами зон, адресами и версиями.
4. zip-пакет содержит несколько `.dtt`, и библиотека импортирует все entry автоматически.
5. zip-пакет содержит несколько `.dtt`, и прикладная служба выбирает только часть entry по имени.

---

## 3.4 Импорт в branch equipment JSON

### Режимы
1. bytes-режим — `importDttSetToBranch(...)`
2. Base64-режим — `importDttBase64SetToBranch(...)`
3. zip-режим — `importDttZipToBranch(...)`
4. high-level plan preparation — `prepareBranchAssemblyRequest(...)`
5. high-level plan orchestration — `assembleBranch(BranchImportPlanRequest)`
6. high-level zip orchestration — `assembleBranch(byte[] zipPayload, BranchImportPlanRequest)`

### Что покрыто
- legacy branch import по списку `branchIds`;
- structured branch import с явным перечислением типов устройств;
- metadata override;
- override значений параметров типа устройства;
- override устройств;
- override `kind`;
- branch-specific topology из `templateOrigin`, если она присутствует в `.dtt`.

### Пример: один шаблон -> один branch-specific тип с несколькими устройствами

```java
BranchImportPlanRequest plan = new BranchImportPlanRequest(
        null,
        null,
        MergeStrategy.FAIL_IF_EXISTS,
        List.of(new BranchImportSourceRequest(
                "branch-msk-01",
                "Москва · окно 1",
                List.of(new BranchDeviceTypeImportSourceRequest(
                        archiveBase64,
                        null,
                        Map.of("TicketZone", "3"),
                        new DeviceTypeMetadata(
                                "display-wd3264-red-window",
                                "Display WD3264 Красное окно",
                                "Display WD3264 Красное окно",
                                "Конфигурация дисплея для красного окна",
                                "2.1.0",
                                null
                        ),
                        List.of(
                                new DeviceInstanceImportRequest(
                                        "display-1",
                                        "Display 1",
                                        "Display 1",
                                        "Основной дисплей",
                                        Map.of("IP", "192.168.1.101", "Port", 22224)
                                ),
                                new DeviceInstanceImportRequest(
                                        "display-2",
                                        "Display 2",
                                        "Display 2",
                                        "Дополнительный дисплей",
                                        Map.of("IP", "192.168.1.102", "Port", 22224)
                                )
                        ),
                        "display"
                ))
        ))
);

BranchEquipment equipment = facade.assembleBranch(plan);
```

### Подкатегории кейсов
1. Один тип устройства без вложенных устройств.
2. Один тип устройства с одним устройством.
3. Один тип устройства с несколькими устройствами.
4. Один `.dtt` используется в нескольких branch-ах с разными override.
5. Один `.dtt` используется несколько раз в одном branch, если merge-стратегия допускает копирование с суффиксом.
6. Импорт branch-specific снимка из `templateOrigin`.

---

## 3.5 Импорт в уже существующий branch equipment JSON

### Режимы
1. legacy bytes/Base64/zip-методы:
   - `importDttSetToExistingBranch(...)`
   - `importDttBase64SetToExistingBranch(...)`
   - `importDttZipToExistingBranch(...)`
2. JSON-ориентированные методы:
   - `importDttBase64SetToExistingBranchJson(...)`
   - `importDttZipToExistingBranchJson(...)`
3. новый orchestration layer:
   - `mergeIntoExistingBranch(...)`
   - `mergeIntoExistingBranchJson(...)`
   - `mergeIntoExistingBranch(byte[] zipPayload, ...)`
   - `mergeIntoExistingBranchJson(byte[] zipPayload, ...)`

### Что покрыто
- merge incoming branch equipment с existing branch equipment;
- merge по выбранной стратегии;
- structured-import в existing JSON без ручного parse + assemble + merge в прикладной службе.

### Пример

```java
BranchImportPlanRequest plan = ...;
String existingBranchJson = Files.readString(Path.of("DeviceManager.json"));

BranchEquipment merged = facade.mergeIntoExistingBranchJson(existingBranchJson, plan);
```

### Подкатегории кейсов
1. Добавить новый тип устройства в существующее отделение.
2. Заменить существующий тип устройства более новой редакцией.
3. Добавить устройства к уже существующему типу устройства.
4. Импортировать набор `.dtt` в существующий JSON из zip-пакета.

---

## 3.6 Preview-диагностика импорта

### Что покрыто
- подсчёт количества значений по умолчанию;
- подсчёт количества override-значений;
- диагностика на уровне `deviceTypeId` или `branchId:deviceTypeId`.

### Методы
- `computeProfileImportPreview(...)`
- `computeProfileImportPreview(byte[] zipPayload, ...)`
- `computeBranchImportPreview(...)`
- `computeBranchImportPreview(byte[] zipPayload, ...)`
- `previewProfileImportDetailed(...)`
- `previewProfileImportDetailed(byte[] zipPayload, ...)`
- `previewBranchImportDetailed(...)`
- `previewBranchImportDetailed(byte[] zipPayload, ...)`

### Что означает результат

`ImportPreviewComputationEntry` содержит:

- `defaultValuesCount` — сколько default значений было прочитано из `.dtt`;
- `overrideValuesCount` — сколько override-значений было передано поверх шаблона.

Это не итоговое количество параметров в конечной модели. Это именно диагностический счётчик, который помогает понять, насколько сильно входящий plan отклоняется от исходного шаблона.

---

## 3.7 Экспорт `.dtt` из profile JSON

### Что покрыто
- batch-export всего набора типов устройств;
- batch-export выбранных `deviceTypeId`;
- single-export одного типа устройства;
- export в zip;
- export в Base64;
- export из object-model и из string JSON;
- preview single-export.

### Подкатегории кейсов
1. Скачать все шаблоны профиля как zip.
2. Скачать один тип устройства как `.dtt`.
3. Проверить, сможет ли система экспортировать указанный `deviceTypeId`, не отдавая файл.
4. Сгенерировать zip на лету в upload-download endpoint.

---

## 3.8 Экспорт `.dtt` из branch equipment JSON

### Что покрыто
- batch-export из branch equipment;
- выбор branch-фильтра;
- merge конфликтов между branch при повторении одного `deviceTypeId`;
- single-export конкретного типа устройства;
- export в zip/Base64;
- export из object-model и из string JSON;
- preview single-export.

### Подкатегории кейсов
1. Экспортировать тип устройства из одного branch.
2. Экспортировать общий тип из нескольких branch по merge-стратегии.
3. Получить zip-набор шаблонов для переноса между окружениями.

---

## 3.9 Metadata inheritance и парный импорт profile + branch

### Что покрыто
- одновременная сборка profile и branch equipment из одного набора `.dtt`;
- отдельные metadata override для profile и отдельно для каждого branch.

### Когда использовать
- когда каталог типов устройств и branch-конфигурация должны собираться из одного входного набора шаблонов, но иметь различающиеся displayName/description/version.

---

## 4. Как выбирать merge-стратегию

### `FAIL_IF_EXISTS`
Используйте, когда конфликт должен считаться ошибкой интеграции. Это режим строгого контроля.

Подходит для:
- production-импорта с гарантией отсутствия скрытого перезаписывания;
- CI-проверки шаблонов;
- административных операций, где конфликт должен быть явно разрешён человеком.

### `REPLACE`
Используйте, когда входящий шаблон — новый источник истины.

Подходит для:
- controlled rollout новой редакции типа устройства;
- пакетного обновления конфигурации отделений;
- повторного импорта из эталонного каталога.

### `MERGE_NON_NULLS`
Используйте, когда incoming должен точечно обновить значения, но не стирать отсутствующие поля.

Подходит для:
- частичных patch-операций;
- override нескольких параметров поверх эталонного шаблона.

### `MERGE_PRESERVE_EXISTING`
Используйте, когда существующее состояние важнее входящего, а incoming выступает как источник только для пустых значений.

Подходит для:
- дозаполнения конфигурации;
- мягкой миграции старых конфигураций.

### `CREATE_COPY_WITH_SUFFIX`
Используйте, когда один шаблон должен породить несколько производных конфигураций без перетирания базового типа устройства.

Подходит для:
- сценариев вида `Display WD3264 Красное окно` / `Display WD3264 Синее окно`;
- миграции, где нужно временно держать рядом старую и новую версию;
- генерации нескольких вариаций оборудования из одного шаблона.

---

## 5. Примеры реальных прикладных задач

## 5.1 Один шаблон дисплея -> несколько производных типов устройства

Задача: есть один исходный `Display WD3264.dtt`, но нужно получить:
- `Display WD3264 Красное окно`
- `Display WD3264 Синее окно`
- `Display WD3264 VIP окно`

Решение:
- использовать три элемента `ProfileDeviceTypeImportSourceRequest` или `BranchDeviceTypeImportSourceRequest`;
- для каждого задать свой `id/name/displayName`;
- для каждого задать свой набор override-значений зон, цветов, адресов, сервисных точек;
- при конфликте использовать `CREATE_COPY_WITH_SUFFIX`, если нужен автоматический режим создания копий.

## 5.2 Один шаблон branch-specific дисплея -> разные branch topology

Задача: один `.dtt` уже содержит `templateOrigin.branchTopologyByBranchId`, и для разных branch должны подтягиваться разные наборы устройств и branch-специфичные скрипты.

Решение:
- использовать structured branch import;
- передать `branchId`, соответствующий снимку в `templateOrigin`;
- библиотека возьмёт branch-specific topology, а не generic fallback.

## 5.3 Импорт zip-набора шаблонов, из которого пользователь выбрал только часть

Задача: UI дал пользователю список всех `.dtt` внутри zip, а затем пользователь выбрал только нужные.

Решение:
1. вызвать `extractDeviceTypeMetadataFromDttOrZip(zipPayload)`;
2. показать пользователю список карточек;
3. собрать `ProfileImportPlanRequest` или `BranchImportPlanRequest` c `archiveEntryName`;
4. вызвать `assembleProfile(zipPayload, plan)` или `assembleBranch(zipPayload, plan)`.

## 5.4 Merge нового шаблона в уже существующий `DeviceManager.json`

Задача: branch уже работает в production, и в него нужно добавить новый производный тип устройства, не разрушив текущую конфигурацию.

Решение:
- считать существующий JSON строкой или моделью;
- собрать `BranchImportPlanRequest`;
- вызвать `mergeIntoExistingBranchJson(...)` или `mergeIntoExistingBranch(...)`.

---

## 6. Что теперь не обязана делать прикладная служба вручную

После переноса orchestration-логики в библиотеку прикладной слой больше не обязан вручную:

- парсить Base64 `.dtt` и самостоятельно читать каждый архив;
- распаковывать zip и сопоставлять `archiveEntryName`;
- вручную строить `EquipmentProfileAssemblyRequest` или `BranchEquipmentAssemblyRequest` для high-level сценариев;
- самостоятельно выполнять цепочку `parse existing JSON -> assemble incoming -> merge` при structured merge в существующий branch JSON;
- дублировать логику overlay metadata/deviceTypeParamValues/device overrides между несколькими REST endpoint.
- вручную кодировать результат single-export в Base64 (`Base64.getEncoder().encodeToString(...)`) для JSON-ответов.

---

## 7. Рекомендуемый выбор API по задаче

| Задача | Рекомендуемый метод |
|---|---|
| Проверить один `.dtt` | `readDtt(...)` + `validate(...)` |
| Получить карточки шаблонов из zip | `extractDeviceTypeMetadataFromDttOrZip(...)` |
| Нормализовать metadata для отображения | `normalizeDeviceTypeMetadata(...)` |
| Собрать profile из Base64-import-plan | `assembleProfile(ProfileImportPlanRequest)` |
| Собрать profile из zip-import-plan | `assembleProfile(byte[] zipPayload, ProfileImportPlanRequest)` |
| Собрать branch из Base64-import-plan | `assembleBranch(BranchImportPlanRequest)` |
| Собрать branch из zip-import-plan | `assembleBranch(byte[] zipPayload, BranchImportPlanRequest)` |
| Выполнить preview profile из import-plan | `previewProfileImport(ProfileImportPlanRequest)`, `previewProfileImport(byte[] zipPayload, ...)` |
| Выполнить preview branch из import-plan | `previewBranchImport(BranchImportPlanRequest)`, `previewBranchImport(byte[] zipPayload, ...)` |
| Merge в существующий branch JSON | `mergeIntoExistingBranchJson(...)` |
| Merge в существующий branch JSON из zip | `mergeIntoExistingBranchJson(byte[] zipPayload, ...)` |
| Получить счётчики default/override | `computeProfileImportPreview(...)`, `computeProfileImportPreview(byte[] zipPayload, ...)`, `computeBranchImportPreview(...)`, `computeBranchImportPreview(byte[] zipPayload, ...)` |
| Получить JSON + агрегированные счётчики после сборки | `toProfileAssemblyView(...)`, `toBranchAssemblyView(...)` |
| Экспорт одного `.dtt` из profile | `exportSingleDttFromProfile(...)` |
| Экспорт одного `.dtt` из branch | `exportSingleDttFromBranch(...)` |
| Экспорт одного `.dtt` в unified DTO | `exportSingleDttResultFromProfile(...)`, `exportSingleDttResultFromProfileJson(...)`, `exportSingleDttResultFromBranch(...)`, `exportSingleDttResultFromBranchJson(...)` |
| Экспорт одного `.dtt` в Base64 | `exportSingleDttFromProfileBase64(...)`, `exportSingleDttFromBranchBase64(...)`, `exportSingleDttFromProfileJsonBase64(...)`, `exportSingleDttFromBranchJsonBase64(...)` |
| Preview single-export | `previewSingleDttExportFromProfile(...)`, `previewSingleDttExportFromBranch(...)` |
| Вернуть zip-набор `.dtt` | `exportProfileToDttZip(...)`, `exportBranchToDttZip(...)` |

---

## 8. Итог

Библиотека покрывает три класса задач:

1. **Работа с архивом шаблона** — read/write/validate/inspect/version.
2. **Сборка и merge конфигураций** — profile, branch, existing branch, Base64, zip, structured import, legacy import.
3. **Экспорт назад в `.dtt`** — batch, single, zip, preview, object-model и string JSON.

Новый orchestration layer делает библиотеку пригодной не только как низкоуровневый toolkit, но и как прямой прикладной движок для служб, которые принимают upload/Base64/zip и формируют конфигурации оборудования без дублирования доменной логики в HTTP-слое.


## Совместная сборка профиля и отделений с наследованием metadata

### Что решает эта фича

Фича предназначена для случая, когда один и тот же набор DTT должен одновременно участвовать в двух результатах:

- в `EquipmentProfile`;
- в `BranchEquipment`.

При этом metadata типа устройства могут отличаться:

- на уровне профиля оборудования;
- на уровне конкретного отделения.

### API

Используется высокоуровневый метод фасада `assembleProfileAndBranchWithMetadata(ProfileBranchMetadataImportPlanRequest)`.

### Что такое наследование metadata

Наследование metadata означает, что branch-уровень берёт metadata, уже сформированные на уровне profile, и при необходимости дополняет или переопределяет их для конкретного отделения.

### Подкатегории кейсов

#### 1. Один шаблон, одинаковые metadata везде
Когда профиль и отделения должны ссылаться на один и тот же тип устройства без дополнительных различий.

#### 2. Один шаблон, общие profile override
Когда на уровне профиля нужно изменить имя, отображаемое имя, описание, версию или иконку типа устройства для всех последующих branch.

#### 3. Один шаблон, разные metadata по отделениям
Когда в отделении A и отделении B один и тот же тип устройства должен выглядеть по-разному в metadata.

#### 4. Комбинированный сценарий
Когда сначала формируются общие metadata уровня profile, а затем отдельные branch дополняют их собственными изменениями.

### Пример

Пример: один шаблон `Display WD3264` импортируется как общий тип витринного дисплея в профиль оборудования, но в отделении `branch-red` отображается как `Display WD3264 Красное окно`, а в отделении `branch-blue` — как `Display WD3264 Синее окно`.

### Термины

#### Merge-стратегия
Merge-стратегия — это правило, по которому библиотека разрешает конфликт, если при сборке уже существует тип устройства, branch или устройство с тем же ключом. В зависимости от выбранной стратегии библиотека либо запрещает конфликт, либо заменяет существующее значение, либо дополняет существующую модель.
