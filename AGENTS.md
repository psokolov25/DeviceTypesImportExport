# AGENTS.md

## Назначение репозитория

Этот репозиторий содержит production-ready multi-module Maven проект на Java 17 + Micronaut 4.x для работы с шаблонами типов устройств в формате **`.dtt`**.

Проект обязан поддерживать **три модели данных**:

1. **`DTT`** — шаблон одного типа устройства.
2. **JSON профиля оборудования** — карта `deviceTypes`, то есть структура, эквивалентная содержимому `deviceTypes` внутри одного branch в `DeviceManager.json`.
3. **JSON оборудования отделений** — полный JSON уровня `DeviceManager.json`, содержащий набор отделений, внутри которых находятся `deviceTypes` и далее полная конфигурация типов устройств и устройств.

Ключевая идея: **`.dtt` — это канонический переносимый шаблон одного типа устройства**, а профиль оборудования и оборудование отделений собираются из **одного или нескольких `.dtt`**.

---

## Ключевые термины

### DTT

`.dtt` — ZIP-архив шаблона **одного** типа устройства.

Он содержит:

- метаданные типа устройства;
- схему параметров типа;
- схему параметров устройств;
- Groovy-скрипты;
- значения по умолчанию;
- примерные значения;
- binding hints;
- информацию, достаточную для восстановления:
  - в JSON профиля оборудования;
  - в JSON оборудования отделений.

### Профиль оборудования

Профиль оборудования — это **только карта `deviceTypes`**, без внешней branch-обёртки.

Иначе говоря, это отдельный JSON, форма которого соответствует тому, как `deviceTypes` хранится внутри одного отделения в `DeviceManager.json`.

### Оборудование отделений

Оборудование отделений — это полный JSON наподобие `DeviceManager.json`:

- карта отделений;
- внутри каждого отделения — `deviceTypes`;
- внутри типов устройств — параметры типа, скрипты, команды, устройства и их параметры.

---

## Главные архитектурные правила

1. Не делай упрощённые демонстрационные реализации там, где нужна расширяемая архитектура.
2. Не теряй Groovy-код ни при каких преобразованиях.
3. Явно разделяй:
   - шаблон типа устройства (`DTT`);
   - JSON профиля оборудования;
   - JSON оборудования отделений;
   - каноническую внутреннюю модель;
   - схему параметров;
   - значения по умолчанию;
   - явные override-значения;
   - archive DTO;
   - public API DTO;
   - raw source model.
4. Не смешивай схему параметров и конкретные значения в одну неразличимую массу.
5. Поддерживай сценарий `device type without child devices`.
6. Поддерживай сценарий `device type with one or many child devices`.
7. Поддерживай сборку:
   - одного `.dtt` в профиль оборудования;
   - нескольких `.dtt` в профиль оборудования;
   - одного `.dtt` в оборудование отделений;
   - нескольких `.dtt` в оборудование отделений.
8. Поддерживай обратный процесс:
   - profile JSON -> набор `.dtt`;
   - branch equipment JSON -> набор `.dtt`.
9. Сохраняй максимум метаданных параметров:
   - `name`
   - `displayName`
   - `type`
   - `description`
   - `exampleValue`
   - `paramatersMap`
   - иные вспомогательные поля.
10. Любая новая функциональность должна быть покрыта тестами.
11. Любая публичная API-сущность должна иметь подробный JavaDoc на русском языке.
12. Если README, JavaDoc, код и тесты расходятся — исправляй расхождение, а не маскируй его.

---

## Ключевое изменение формата

### Используется `.dtt`, а не `.dtts`

`.dtt` всегда описывает **один тип устройства**.

Если требуется собрать профиль оборудования или оборудование нескольких отделений, используются **один или более `.dtt`**.

### Внутри `.dtt` используется YAML, а не JSON

Все описательные файлы внутри шаблона должны быть в формате **YAML (`.yml`)**, а не JSON.

Это обязательное архитектурное правило.

Причины:

- YAML удобнее читать человеку;
- YAML удобнее редактировать вручную;
- YAML даёт более понятные diff в git;
- YAML лучше подходит для review и контроля изменений.

### Что именно должно быть в YAML

В YAML должны храниться:

- манифест;
- описание типа устройства;
- схемы параметров;
- binding hints;
- default values;
- example values;
- origin metadata;
- примеры запросов/сборки, если они кладутся в архив.

Groovy-код должен храниться отдельными `.groovy` файлами.

---

## Ожидаемая структура `.dtt`

`.dtt` — ZIP-архив с предсказуемой внутренней структурой.

Минимально ожидаются:

- `manifest.yml`
- `template/device-type.yml`
- `template/device-type-parameters.yml`
- `template/device-parameters-schema.yml`
- `template/template-origin.yml`
- `template/binding-hints.yml`
- `template/default-values.yml`
- `template/example-values.yml`
- `scripts/onStartEvent.groovy`
- `scripts/onStopEvent.groovy`
- `scripts/onPublicStartEvent.groovy`
- `scripts/onPublicFinishEvent.groovy`
- `scripts/deviceTypeFunctions.groovy`
- `scripts/event-handlers/<EVENT_NAME>.groovy`
- `scripts/commands/<COMMAND_NAME>.groovy`
- `examples/profile-values-example.yml`
- `examples/branch-values-example.yml`
- `README-IN-ARCHIVE.md`

---

## Минимальные требования к `manifest.yml`

`manifest.yml` должен содержать минимум:

- `formatName: DTT`
- `formatVersion`
- `createdAt`
- `createdBy`
- `libraryVersion`
- `deviceTypeId`
- `deviceTypeName`
- `deviceTypeDisplayName`
- `deviceTypeDescription`
- `deviceTypeKind`
- `supportsChildDevices`
- `containsLifecycleScripts`
- `containsEventHandlers`
- `containsCommands`
- `containsDeviceTypeFunctions`
- `parameterSchemaVersion`
- `defaultValuesIncluded`
- `exampleValuesIncluded`
- `supportsProfileImport`
- `supportsBranchImport`
- `sourceKind`
- `sourceSummary`

Если меняется структура шаблона, не ломай старые архивы без явного изменения `formatVersion` и логики обратной совместимости.

---

## Архитектурные инварианты

### 1. Каноническая внутренняя модель обязательна

Все преобразования должны идти через каноническую внутреннюю модель.

Нельзя строить core-логику на хаотичных прямых преобразованиях вида:

- `Map -> Map`
- `JSON tree -> JSON tree`
- `YAML tree -> branch JSON tree`

Минимально ожидаются сущности уровня:

- `CanonicalDeviceTypeTemplate`
- `CanonicalDeviceTypeMetadata`
- `CanonicalParameterSchema`
- `CanonicalParameterDefinition`
- `CanonicalDeviceInstanceTemplate`
- `CanonicalScriptSet`
- `CanonicalDefaultValues`
- `CanonicalProfileProjection`
- `CanonicalBranchProjection`

### 2. Нужно разделять source models

Должны существовать разные raw-модели и парсеры для:

- `.dtt`
- profile JSON
- branch equipment JSON

Не пытайся решить всё одним универсальным `Map<String, Object>`-подходом, если можно сделать типизированные модели.

### 3. `.dtt` не должен быть branch-only форматом

`.dtt` обязан быть пригоден:

- для сборки profile JSON;
- для сборки branch equipment JSON.

### 4. Default values должны быть отделены от override values

Нужно различать:

- значения по умолчанию из шаблона;
- примерные значения;
- значения, переданные пользователем явно при сборке профиля;
- значения, переданные пользователем явно при сборке оборудования отделений.

### 5. IDs не считаются абсолютной истиной

При переносе между средами и моделями исходные `id` нельзя слепо считать вечными идентификаторами.

Нужны стратегии разрешения конфликтов и генерации новых идентификаторов.

---

## Поддерживаемые направления преобразований

### A. Один `.dtt` -> profile JSON

На вход:

- один `.dtt`;
- явные значения параметров типа и устройств
  или
- указание использовать default values из шаблона.

На выход:

- profile JSON с одной записью типа устройства.

### B. Несколько `.dtt` -> profile JSON

На вход:

- один или более `.dtt`;
- для каждого шаблона:
  - значения параметров типа;
  - значения параметров устройств;
  - либо флаг использования default values.

На выход:

- единый profile JSON — карта `deviceTypes`.

### C. Один `.dtt` -> branch equipment JSON

На вход:

- один `.dtt`;
- список отделений;
- для каждого отделения:
  - значения параметров типа;
  - список устройств;
  - значения параметров устройств;
  - либо флаг использования default values.

На выход:

- полный JSON оборудования отделений.

### D. Несколько `.dtt` -> branch equipment JSON

На вход:

- один или более `.dtt`;
- список отделений;
- для каждого отделения:
  - какие шаблоны применять;
  - значения параметров типов;
  - значения параметров устройств;
  - режим использования default values;
  - merge strategy.

На выход:

- полный JSON оборудования отделений.

### E. Profile JSON -> набор `.dtt`

На вход:

- profile JSON.

На выход:

- набор `.dtt`, по одному на каждый тип устройства.

### F. Branch equipment JSON -> набор `.dtt`

На вход:

- полный JSON оборудования отделений.

На выход:

- набор `.dtt`, по одному на каждый тип устройства:
  - либо из выбранного отделения;
  - либо агрегированный по нескольким отделениям;
  - либо по явно заданному набору типов.

---

## Работа с параметрами

Поддерживаемые типы минимум:

- `String`
- `Number`
- `Boolean`
- `Select`
- `Object`
- `Array`
- `nullable`
- отсутствие значения как отдельный случай

Всегда старайся сохранять:

- `exampleValue`
- `defaultValue`
- display name
- description
- auxiliary metadata
- nested object schema
- array item schema

Для `Object` и вложенных структур, вроде `zones`, должна строиться полноценная вложенная схема.

Не упрощай вложенные параметры до строки JSON, если их можно хранить как структурированную схему.

---

## Работа со скриптами

Groovy-код хранить отдельно и без модификации.

Нужно поддерживать:

- `onStartEvent`
- `onStopEvent`
- `onPublicStartEvent`
- `onPublicFinishEvent`
- `eventHandlers`
- `commands`
- `deviceTypeFunctions`

Не нормализуй скрипты “для красоты”, не меняй форматирование и не переписывай содержимое.

Пустые и отсутствующие секции должны корректно читаться и восстанавливаться.

---

## Merge-стратегии

Должны поддерживаться и тестироваться:

- `FAIL_IF_EXISTS`
- `REPLACE`
- `MERGE_NON_NULLS`
- `MERGE_PRESERVE_EXISTING`
- `CREATE_COPY_WITH_SUFFIX`

Эти стратегии должны работать:

- при сборке profile JSON из нескольких `.dtt`;
- при сборке branch equipment JSON из нескольких `.dtt`;
- при patch/merge в существующие JSON.

Если добавляешь новую merge-логику:

1. сначала формализуй поведение на конфликтах;
2. затем обнови DTO;
3. затем обнови сервисы;
4. затем обнови README;
5. затем добавь тесты на конфликтные сценарии.

---

## Ожидаемая структура репозитория

Корневой проект должен быть multi-module:

- `device-template-library`
- `device-template-demo-service`
- при необходимости `device-template-library-test-support`

Типовые пакеты библиотеки:

- `ru.aritmos.dtt.api`
- `ru.aritmos.dtt.api.dto`
- `ru.aritmos.dtt.archive`
- `ru.aritmos.dtt.archive.model`
- `ru.aritmos.dtt.export`
- `ru.aritmos.dtt.importing`
- `ru.aritmos.dtt.json.profile`
- `ru.aritmos.dtt.json.branch`
- `ru.aritmos.dtt.model`
- `ru.aritmos.dtt.model.profile`
- `ru.aritmos.dtt.model.branch`
- `ru.aritmos.dtt.model.canonical`
- `ru.aritmos.dtt.assembly`
- `ru.aritmos.dtt.validation`
- `ru.aritmos.dtt.exception`

Типовые пакеты demo-service:

- `ru.aritmos.dtt.demo.controller`
- `ru.aritmos.dtt.demo.service`
- `ru.aritmos.dtt.demo.dto`
- `ru.aritmos.dtt.demo.config`

---

## Обязательные сервисы библиотеки

Минимально ожидаются:

- `DeviceTypeTemplateExportService`
- `DeviceTypeTemplateImportService`
- `DttArchiveReader`
- `DttArchiveWriter`
- `DeviceManagerBranchJsonParser`
- `DeviceManagerBranchJsonGenerator`
- `EquipmentProfileJsonParser`
- `EquipmentProfileJsonGenerator`
- `TemplateAssemblyService`
- `TemplateValidationService`

### Дополнительные пояснения

#### `DeviceTypeTemplateExportService`

Должен уметь:

- экспортировать один `.dtt` из profile JSON;
- экспортировать один `.dtt` из branch equipment JSON;
- экспортировать набор `.dtt`.

#### `DeviceTypeTemplateImportService`

Должен уметь:

- читать один `.dtt`;
- читать набор `.dtt`;
- валидировать шаблоны;
- строить profile JSON;
- строить branch equipment JSON.

#### `TemplateAssemblyService`

Должен уметь:

- собирать profile JSON из одного/нескольких `.dtt`;
- собирать branch equipment JSON из одного/нескольких `.dtt`;
- применять default values;
- применять explicit overrides;
- делать preview результата.

---

## DTO и публичное API

Нужны понятные public DTO.

Минимально ожидаются:

### Общие DTO

- `DttArchiveDescriptor`
- `DeviceTypeTemplate`
- `DeviceTypeMetadata`
- `TemplateParameterSchema`
- `TemplateParameterDefinition`
- `TemplateScriptSet`
- `TemplateDefaultValues`
- `ValidationResult`
- `ValidationIssue`
- `ExportResult`
- `ImportResult`

### Для сборки профиля оборудования

- `EquipmentProfileAssemblyRequest`
- `EquipmentProfileDeviceTypeRequest`
- `TemplateValueOverride`
- `DeviceInstanceValueOverride`

### Для сборки оборудования отделений

- `BranchEquipmentAssemblyRequest`
- `BranchImportRequest`
- `BranchDeviceTypeImportRequest`
- `DeviceInstanceImportRequest`

### Для обратного экспорта

- `ProfileExportRequest`
- `BranchEquipmentExportRequest`
- `BatchDttExportResult`

Не прячь важные сценарии в анонимные `Map<String, Object>`.

---

## Исключения

Используй собственные диагностичные исключения:

- `DttFormatException`
- `TemplateValidationException`
- `TemplateImportException`
- `TemplateExportException`
- `TemplateAssemblyException`

Не бросай сырые `RuntimeException` из core-логики формата, если можно дать предметное исключение.

---

## Правила для demo-service

Demo-service должен быть рабочим, а не декоративным.

Он обязан поддерживать минимум:

- валидацию `.dtt`;
- инспекцию `.dtt`;
- export from profile JSON;
- export all `.dtt` from profile JSON;
- export from branch equipment JSON;
- export all `.dtt` from branch equipment JSON;
- import one/many `.dtt` to profile JSON;
- import one/many `.dtt` to branch equipment JSON;
- preview для сборки профиля;
- preview для сборки оборудования отделений.

### Для контроллеров demo-service

- Всегда добавляй OpenAPI annotations.
- Для DTO добавляй примеры.
- Для multipart endpoints описывай содержимое явно.
- Для ошибок валидации возвращай структурированный результат.
- Не делай Swagger UI формальным — он должен реально показывать сценарии работы с одним и несколькими `.dtt`.

---

## README и документация

README обязан быть синхронизирован с кодом.

При любых архитектурных изменениях проверь и обнови:

1. формат `.dtt`;
2. YAML-структуру архива;
3. реальные имена классов;
4. список эндпоинтов;
5. сценарии сборки profile JSON;
6. сценарии сборки branch equipment JSON;
7. сценарии обратного экспорта в набор `.dtt`;
8. примеры default values и explicit overrides;
9. merge-стратегии.

Если README описывает несуществующий класс, endpoint или структуру архива — это дефект.

---

## JavaDoc

JavaDoc обязателен для:

- public interfaces;
- public classes;
- DTO;
- enum;
- exception classes;
- public service classes;
- controller classes;
- `package-info.java` ключевых пакетов.

JavaDoc должен быть на русском языке и объяснять смысл, а не просто повторять имя поля.

Хороший JavaDoc должен объяснять:

- что делает сущность;
- для какой модели она предназначена (`DTT`, profile JSON, branch equipment JSON, canonical model);
- какие есть ограничения;
- как трактуются значения по умолчанию;
- как ведёт себя код при конфликтах и ошибках.

---

## Технологические ограничения

- Java 17
- Micronaut 4.x
- Maven
- JUnit 5
- AssertJ
- Mockito только если действительно нужен
- Jackson или Micronaut Serialization
- ZIP через `java.util.zip` или Apache Commons Compress
- OpenAPI / Swagger UI через Micronaut OpenAPI
- Без Lombok
- Не использовать `@SuppressWarnings`, кроме реально оправданных случаев
- Не оставлять бессодержательные `TODO`
- Не вводить магические строки там, где они влияют на формат `.dtt`

---

## Стиль изменений

### Делай

- небольшие логически цельные изменения;
- типизированные DTO;
- явные границы между archive/API/canonical/raw model;
- обновление тестов вместе с кодом;
- обновление README и JavaDoc вместе с кодом.

### Не делай

- огромные несвязанные рефакторинги “заодно”;
- скрытую смену формата `.dtt`;
- нарушение обратной совместимости без изменения `formatVersion`;
- замену типизированной модели на `Map<String, Object>` без серьёзной причины.

---

## Правила для тестов

Минимальный обязательный набор сценариев при изменениях в core-логике:

- один `.dtt` -> profile JSON;
- несколько `.dtt` -> profile JSON;
- один `.dtt` -> branch equipment JSON;
- несколько `.dtt` -> branch equipment JSON;
- profile JSON -> набор `.dtt`;
- branch equipment JSON -> набор `.dtt`;
- round-trip profile -> dtt set -> profile;
- round-trip branches -> dtt set -> branches;
- YAML parsing;
- broken YAML;
- missing required YAML files;
- deterministic ZIP output;
- type without child devices;
- multiple device instances of same type;
- nested object parameters;
- null/empty scripts;
- multiple event handlers;
- multiple commands;
- merge strategy conflicts;
- demo-service OpenAPI generation.

---

## Проверка перед завершением задачи

Перед тем как считать работу завершённой, проверь:

1. проект компилируется;
2. тесты проходят;
3. новый код покрыт тестами;
4. README не расходится с кодом;
5. JavaDoc не расходится с кодом;
6. OpenAPI не сломан;
7. сценарии profile JSON поддержаны;
8. сценарии branch equipment JSON поддержаны;
9. обратный экспорт в набор `.dtt` поддержан;
10. Groovy-код не теряется;
11. YAML внутри `.dtt` остался читаемым и предсказуемым;
12. deterministic archive output не сломан, если затронут архиватор.

---

## Команды

Если в задаче не сказано иное, используй Maven wrapper.

### Базовые команды

```bash
./mvnw -Dmaven.repo.local=.m2/repository clean test
./mvnw -Dmaven.repo.local=.m2/repository clean verify
./mvnw -Dmaven.repo.local=.m2/repository -pl device-template-library test
./mvnw -Dmaven.repo.local=.m2/repository -pl device-template-demo-service test
```

Если меняется:

- формат `.dtt`;
- YAML serialization/deserialization;
- archive reader/writer;
- profile assembly;
- branch assembly;
- merge-логика;
- demo-service OpenAPI,

предпочитай полный `clean verify`, а не частичный прогон.

---

## Приоритеты при неоднозначности

Если есть неоднозначность, приоритет такой:

1. сохранность данных и Groovy-кода;
2. корректность модели и обратимость преобразования;
3. предсказуемость формата `.dtt`;
4. диагностичность ошибок;
5. читаемость YAML;
6. читаемость кода;
7. удобство API и demo-service.

---

## Что считать завершённой работой

Работа считается завершённой только если:

- код реализован;
- тесты добавлены и проходят;
- README обновлён;
- JavaDoc обновлён;
- форматные и архитектурные инварианты не нарушены.

Если что-то не выполнено — укажи это явно в отчёте.

---

## Формат отчёта о проделанной работе

Когда завершаешь задачу, в ответе кратко укажи:

1. что изменено;
2. какие модули затронуты;
3. какие тесты добавлены/обновлены;
4. какие ограничения или риски остались;
5. какие команды запускались для проверки.

Не пиши общие фразы вроде “улучшил архитектуру”; перечисляй конкретные изменения.

---

## Использование библиотеки как зависимости (обязательный сценарий)

При добавлении/изменении публичного API учитывай, что библиотека должна быть удобна для подключения как Maven dependency.

Минимальные требования:

1. Должен существовать единый фасадный вход (`DeviceTemplateLibrary` / `DeviceTemplateLibraryFacade`) для типовых сценариев без ручной сборки графа сервисов.
2. Должен существовать builder/конфигурируемый способ подмены реализаций (validator/assembler/parser/generator) для интеграций.
3. Изменения в фасаде и builder обязательно покрывать unit-тестами.
4. README обязан содержать раздел с примером методного использования библиотеки как зависимости.

### Режимы передачи данных в фасаде библиотеки (обязательно)

Для сценариев интеграции библиотека должна поддерживать оба режима:

1. `JSON как объект` и `JSON как строка` для экспортных операций profile/branch.
2. `DTT как Base64` и `DTT как upload-download (zip)` для batch-импорта/экспорта.

Если добавляется новый публичный сценарий импорта/экспорта — проверяй, что он доступен в обоих режимах
или явно документировано, почему конкретный режим не поддержан.


## Уточнение по demo-service (добавлено по итогам ревью)

- Demo-service обязан использовать библиотеку **как внешнюю зависимость через публичный фасад API** (`DeviceTemplateLibraryFacade` / `DeviceTemplateLibrary`), а не собирать core-логику из внутренних `Default*`-реализаций библиотеки напрямую.
- Для сценариев валидации, инспекции, импорта и сборки в demo-service предпочтительно вызывать методы фасада, чтобы демонстрировать интеграционный способ использования библиотеки потребителем.
- Для demo-controller ошибок входной валидации нужно возвращать **структурированный JSON-ответ** (не только текст исключения), чтобы в Swagger UI были явные форматы ошибок.
- В demo-service и demo-controller использовать DI и фасад библиотеки как основной способ интеграции; не создавать вручную внутренние core-сервисы библиотеки в контроллерах.
