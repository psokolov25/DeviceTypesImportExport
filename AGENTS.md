# AGENTS.md

## 1. Назначение проекта

Проект реализует production-ready решение на **Java 17 + Micronaut + Maven** для экспорта и импорта шаблонов типов оборудования в/из архива **`.dtts`**, а также для двусторонней работы с двумя видами JSON-моделей:

1. **JSON отделений** — реальная конфигурация оборудования по отделениям.
2. **JSON профиля оборудования** — шаблонный профиль набора оборудования отделения.

Предметная модель должна соответствовать реальной структуре `DeviceManager.json`: у отделений есть `deviceTypes`, у типов устройств есть `deviceTypeParamValues`, `eventHandlers`, `commands`, lifecycle-секции (`onStartEvent`, `onStopEvent`, `onPublicStartEvent`, `onPublicFinishEvent`), `deviceTypeFunctions`, а также `devices[*].deviceParamValues`; встречаются вложенные объектные параметры вроде `zones`, типы без дочерних устройств и типы с несколькими экземплярами устройств. 

---

## 2. Главная цель

Нужно построить библиотеку и demo-service, которые поддерживают следующие направления преобразования:

* `branch-json -> dtts`
* `dtts -> branch-json`
* `equipment-profile-json -> dtts`
* `dtts -> equipment-profile-json`
* `branch-json -> equipment-profile-json`
* `equipment-profile-json -> branch-json`

При этом `.dtts` является **каноническим переносимым форматом шаблона типа оборудования**, не привязанным жестко только к branch JSON.

---

## 3. Что обязательно должно быть реализовано

### 3.1. Библиотека

Модуль библиотеки должен:

* экспортировать шаблон типа оборудования из JSON отделений;
* экспортировать шаблон типа оборудования из JSON профиля оборудования;
* импортировать `.dtts` в модель отделений;
* импортировать `.dtts` в модель профиля оборудования;
* уметь генерировать:

  * новый JSON отделений,
  * patch existing JSON отделений,
  * merged JSON отделений,
  * новый JSON профиля оборудования,
  * patch existing JSON профиля оборудования,
  * merged JSON профиля оборудования;
* уметь делать preview без записи;
* уметь читать и валидировать `.dtts`;
* уметь извлекать и сравнивать версии шаблонов;
* уметь возвращать версии шаблонов в итоговом JSON;
* уметь автоматически подставлять **значения по умолчанию из шаблона**, если при генерации итогового JSON значение явно не указано.

### 3.2. Demo-service

Micronaut-служба должна предоставлять REST API + Swagger UI для:

* загрузки branch JSON;
* загрузки equipment-profile JSON;
* загрузки/скачивания `.dtts`;
* экспорта шаблона;
* импорта шаблона;
* inspect архива;
* validate архива;
* preview diff;
* генерации итогового branch JSON;
* генерации итогового equipment-profile JSON;
* извлечения версий шаблонов;
* сравнения версий шаблонов.

### 3.3. Документация

Обязательно:

* очень подробный `README.md`;
* полный JavaDoc на русском языке;
* настоящий `AGENTS.md`, согласованный с кодом, README, OpenAPI и тестами.

---

## 4. Ключевые инварианты предметной области

### 4.1. Тип оборудования

Тип оборудования включает:

* метаданные типа;
* параметры типа (`deviceTypeParamValues`);
* Groovy-скрипты:

  * lifecycle,
  * event handlers,
  * commands,
  * functions;
* список дочерних устройств или отсутствие таковых;
* схему параметров дочерних устройств;
* версию шаблона;
* значения по умолчанию для параметров.

### 4.2. Экземпляры устройств

В одном отделении или профиле может быть:

* несколько разных типов устройств;
* несколько экземпляров одного и того же типа;
* экземпляры с разными именами;
* экземпляры с числовыми постфиксами;
* экземпляры с разными значениями `deviceParamValues`.

### 4.3. Тип без дочерних устройств

Если у типа оборудования нет устройств, он всё равно должен импортироваться и экспортироваться как полноценный шаблон.

### 4.4. Вложенные параметры

Нужно поддерживать вложенные параметры типа `Object`, `Array`, составные структуры и nullable-поля.

---

## 5. Новый обязательный блок: версии шаблонов

### 5.1. Общие правила

У каждого шаблона типа оборудования должна быть **отдельная версия шаблона**.

Нельзя смешивать:

* `formatVersion` — версия формата `.dtts`;
* `templateVersion` — версия шаблона типа оборудования;
* `parameterSchemaVersion` — версия схемы параметров.

### 5.2. Для шаблона обязательно хранить

Минимум следующие поля:

* `templateId`
* `templateCode`
* `templateVersion`
* `templateVersionLabel`
* `templateRevision`
* `templateStatus`
* `templateCreatedAt`
* `templateUpdatedAt`

### 5.3. Где версия обязана присутствовать

Версия шаблона должна быть отражена:

* в `manifest.json`;
* в `template/device-type.json`;
* во внутренней canonical model;
* в DTO библиотеки;
* в DTO demo-service;
* в итоговом branch JSON;
* в итоговом equipment-profile JSON;
* в inspect/validate/import/export results.

### 5.4. Получение версии шаблона

Нужно иметь возможность получить версию:

* из `.dtts`;
* из branch JSON;
* из equipment-profile JSON;
* список версий всех шаблонов из branch JSON;
* список версий всех шаблонов из equipment-profile JSON;
* сравнение двух версий.

### 5.5. Конфликты версий

Нужно поддержать version conflict policy:

* `ALLOW_SAME_VERSION`
* `REJECT_OLDER_VERSION`
* `ALLOW_UPGRADE`
* `ALLOW_DOWNGRADE`
* `REPLACE_REGARDLESS_OF_VERSION`
* `KEEP_EXISTING_VERSION`
* `CREATE_PARALLEL_VERSION`
* `FAIL_ON_VERSION_CONFLICT`

---

## 6. Новый обязательный блок: значения по умолчанию параметров шаблона

### 6.1. Общая идея

У шаблона у параметров должны быть **значения по умолчанию оборудования**.

Это обязательная часть модели.

Нужно различать:

* `exampleValue` — пример для документации и диагностики;
* `defaultValue` — техническое значение по умолчанию;
* `defaultEquipmentValue` — дефолтное значение, которое должно подставляться при генерации equipment JSON / branch JSON;
* явно заданное значение в request;
* уже существующее значение в target JSON.

### 6.2. Обязательные правила подстановки default values

Если при генерации branch JSON или equipment-profile JSON **какое-либо значение не указано явно**, библиотека должна использовать:

1. значение из входного request;
2. если его нет — `defaultEquipmentValue` из шаблона;
3. если его нет — `defaultValue` из шаблона;
4. если его нет — существующее значение из target JSON при merge-режиме;
5. если его нет — `null`, только если параметр nullable;
6. иначе — validation issue / import error.

### 6.3. Где нужны default values

Default values должны поддерживаться для:

* параметров типа оборудования;
* параметров дочерних устройств;
* вложенных объектных параметров;
* элементов массивов, если это допустимо по схеме.

### 6.4. Для параметра шаблона обязательно хранить

Минимум:

* `name`
* `displayName`
* `type`
* `description`
* `exampleValue`
* `defaultValue`
* `defaultEquipmentValue`
* `required`
* `nullable`
* `sourceHints`
* `paramatersMap` / служебные маппинги, если есть
* nested schema / array item schema — если применимо

### 6.5. Default values в документации и API

Swagger, README и JavaDoc должны явно описывать:

* что такое `defaultEquipmentValue`;
* чем он отличается от `exampleValue`;
* в каком порядке применяется подстановка;
* как это влияет на branch JSON и equipment-profile JSON.

---

## 7. Архитектурный подход

### 7.1. Тип проекта

Проект должен быть **multi-module Maven project**.

### 7.2. Модули

Обязательные модули:

1. `device-template-library`
2. `device-template-demo-service`

Опционально:

3. `device-template-library-test-support`

### 7.3. Внутренняя архитектура библиотеки

Обязательно разделить:

* raw source model для branch JSON;
* raw source model для equipment-profile JSON;
* canonical internal model;
* archive model;
* public API DTO;
* import/export requests and results.

---

## 8. Рекомендуемая структура пакетов

### 8.1. Библиотека

* `ru.aritmos.dtts.api`
* `ru.aritmos.dtts.api.dto`
* `ru.aritmos.dtts.archive`
* `ru.aritmos.dtts.archive.model`
* `ru.aritmos.dtts.export`
* `ru.aritmos.dtts.importing`
* `ru.aritmos.dtts.json.branch`
* `ru.aritmos.dtts.json.profile`
* `ru.aritmos.dtts.model`
* `ru.aritmos.dtts.model.branch`
* `ru.aritmos.dtts.model.profile`
* `ru.aritmos.dtts.model.canonical`
* `ru.aritmos.dtts.model.version`
* `ru.aritmos.dtts.transformation`
* `ru.aritmos.dtts.validation`
* `ru.aritmos.dtts.exception`

### 8.2. Demo-service

* `ru.aritmos.dtts.demo.controller`
* `ru.aritmos.dtts.demo.service`
* `ru.aritmos.dtts.demo.dto`
* `ru.aritmos.dtts.demo.config`

---

## 9. Формат `.dtts`

`.dtts` — ZIP-архив со строго определенной структурой.

### 9.1. Обязательная структура

* `manifest.json`
* `template/device-type.json`
* `template/device-type-parameters.json`
* `template/device-parameters-schema.json`
* `template/template-origin.json`
* `template/profile-binding-hints.json`
* `scripts/onStartEvent.groovy`
* `scripts/onStopEvent.groovy`
* `scripts/onPublicStartEvent.groovy`
* `scripts/onPublicFinishEvent.groovy`
* `scripts/deviceTypeFunctions.groovy`
* `scripts/event-handlers/<EVENT_NAME>.groovy`
* `scripts/commands/<COMMAND_NAME>.groovy`
* `examples/branch-bindings-example.json`
* `examples/equipment-profile-example.json`
* `README-IN-ARCHIVE.md`

### 9.2. Что должно быть в `manifest.json`

Минимум:

* `formatName`
* `formatVersion`
* `createdAt`
* `createdBy`
* `libraryVersion`
* `templateSourceKind`
* `templateId`
* `templateCode`
* `templateVersion`
* `templateVersionLabel`
* `templateRevision`
* `templateStatus`
* `templateCreatedAt`
* `templateUpdatedAt`
* `deviceTypeCode`
* `deviceTypeName`
* `deviceTypeDisplayName`
* `deviceTypeDescription`
* `deviceTypeKind`
* `supportsDevices`
* `containsScripts`
* `containsEventHandlers`
* `containsCommands`
* `containsDeviceTypeFunctions`
* `parameterSchemaVersion`
* `supportsBranchJsonImport`
* `supportsEquipmentProfileImport`

---

## 10. Источники и целевые модели

### 10.1. Branch JSON

Модель реальных отделений и оборудования.

### 10.2. Equipment Profile JSON

Модель шаблонного профиля оборудования отделения.

### 10.3. Canonical model

Единая внутренняя модель, через которую выполняются:

* экспорт;
* импорт;
* diff;
* merge;
* cross-model conversion;
* version handling;
* default-value resolution.

---

## 11. Обязательные публичные интерфейсы

### 11.1. Экспорт/импорт

* `DeviceTypeTemplateExportService`
* `DeviceTypeTemplateImportService`

### 11.2. Архив

* `DttsArchiveReader`
* `DttsArchiveWriter`

### 11.3. Схемы и парсинг

* `DeviceTypeSchemaBuilder`
* `DeviceManagerJsonParser`
* `DeviceManagerJsonGenerator`
* `EquipmentProfileJsonParser`
* `EquipmentProfileJsonGenerator`

### 11.4. Трансформации

* `BranchProfileTransformationService`

### 11.5. Валидация

* `TemplateValidationService`

### 11.6. Версии

* `TemplateVersionService`
* `TemplateMetadataResolver`

### 11.7. Подстановка default values

Нужно добавить отдельный сервис, например:

* `TemplateDefaultValueResolver`

Он обязан уметь:

* вычислять итоговое значение параметра;
* применять `defaultEquipmentValue`;
* работать с nested object schema;
* работать с массивами;
* формировать trace/diagnostics о том, откуда было взято итоговое значение.

---

## 12. Обязательные DTO

### 12.1. Базовые DTO

* `DttsArchiveDescriptor`
* `DeviceTypeTemplate`
* `DeviceTypeMetadata`
* `TemplateParameterSchema`
* `TemplateParameterDefinition`
* `TemplateScriptSet`
* `ImportResult`
* `ExportResult`
* `ValidationResult`
* `ValidationIssue`

### 12.2. DTO для branch JSON

* `BranchImportRequest`
* `BranchDeviceTypeImportRequest`
* `DeviceInstanceImportRequest`

### 12.3. DTO для equipment profile JSON

* `EquipmentProfileImportRequest`
* `EquipmentProfileDeviceTypeImportRequest`
* `ProfileDeviceInstanceTemplateRequest`

### 12.4. DTO для версий

* `TemplateVersionInfo`
* `TemplateVersionDescriptor`
* `TemplateVersionComparisonResult`
* `TemplateVersionConflict`
* `TemplateVersionSummary`

### 12.5. DTO для default resolution

Нужно добавить, например:

* `ResolvedParameterValue`
* `DefaultValueResolutionTrace`
* `DefaultValueSource`
* `ResolvedTemplateDefaultsSummary`

---

## 13. Модели версий и default values — обязательные поля

### 13.1. `DeviceTypeMetadata`

Должна содержать:

* `templateId`
* `templateCode`
* `templateVersion`
* `templateVersionLabel`
* `templateRevision`
* `templateStatus`

### 13.2. `TemplateParameterDefinition`

Должна содержать:

* `name`
* `displayName`
* `type`
* `description`
* `exampleValue`
* `defaultValue`
* `defaultEquipmentValue`
* `required`
* `nullable`
* `nestedSchema`
* `arrayItemSchema`

### 13.3. `ImportResult`

Должен содержать:

* `appliedTemplateVersion`
* `existingTemplateVersion`
* `versionConflicts`
* `templateVersionSummaries`
* `resolvedDefaultsSummary`

### 13.4. `ExportResult`

Должен содержать:

* `exportedTemplateVersion`
* `templateVersionInfo`

### 13.5. `ValidationResult`

Должен содержать:

* `templateVersionInfo`
* `versionValidationIssues`
* `defaultValueValidationIssues`

---

## 14. Merge-стратегии

Нужно поддерживать базовые merge strategies:

* `FAIL_IF_EXISTS`
* `REPLACE`
* `MERGE_NON_NULLS`
* `MERGE_PRESERVE_EXISTING`
* `CREATE_COPY_WITH_SUFFIX`

Отдельно поддерживать version conflict strategy:

* `ALLOW_SAME_VERSION`
* `REJECT_OLDER_VERSION`
* `ALLOW_UPGRADE`
* `ALLOW_DOWNGRADE`
* `REPLACE_REGARDLESS_OF_VERSION`
* `KEEP_EXISTING_VERSION`
* `CREATE_PARALLEL_VERSION`
* `FAIL_ON_VERSION_CONFLICT`

---

## 15. Правила генерации итогового JSON

### 15.1. Итоговый branch JSON обязан возвращать

* метаданные типа устройства;
* версии шаблонов;
* итоговые значения параметров;
* явно заполненные значения;
* значения, подставленные из default values;
* при необходимости — служебный блок `templateMetadata`.

### 15.2. Итоговый equipment-profile JSON обязан возвращать

* метаданные типа устройства;
* версии шаблонов;
* профильные значения;
* значения, подставленные из default values;
* шаблонные устройства;
* при необходимости — `templateMetadata`.

### 15.3. Единый способ хранения метаданных шаблона

Выбрать один согласованный вариант и использовать везде одинаково, предпочтительно:

* `templateMetadata`

  * `templateId`
  * `templateCode`
  * `templateVersion`
  * `templateVersionLabel`
  * `templateRevision`
  * `templateStatus`

---

## 16. REST API demo-service

### 16.1. Экспорт

* `POST /api/templates/export/from-branch-json`
* `POST /api/templates/export/from-equipment-profile-json`

### 16.2. Архив

* `POST /api/templates/validate`
* `POST /api/templates/inspect`

### 16.3. Импорт

* `POST /api/templates/import/to-branch-json`
* `POST /api/templates/import/to-branch-json/generate`
* `POST /api/templates/import/to-equipment-profile-json`
* `POST /api/templates/import/to-equipment-profile-json/generate`

### 16.4. Cross-model преобразования

* `POST /api/templates/convert/branch-json-to-equipment-profile`
* `POST /api/templates/convert/equipment-profile-to-branch-json`

### 16.5. Preview

* `POST /api/templates/import/preview-diff`

### 16.6. Версии

* `POST /api/templates/version/read`
* `POST /api/templates/version/extract-from-branch-json`
* `POST /api/templates/version/extract-from-equipment-profile-json`
* `POST /api/templates/version/compare`

---

## 17. Swagger / OpenAPI

Swagger UI обязан:

* подробно документировать все DTO;
* иметь примеры branch JSON;
* иметь примеры equipment-profile JSON;
* иметь примеры `.dtts`;
* показывать version-related поля;
* показывать default-related поля;
* объяснять порядок применения default values;
* объяснять разницу между:

  * `formatVersion`
  * `templateVersion`
  * `parameterSchemaVersion`

---

## 18. README — обязательные разделы

README должен содержать как минимум:

1. назначение библиотеки;
2. архитектуру проекта;
3. структуру `.dtts`;
4. экспорт из branch JSON;
5. экспорт из equipment-profile JSON;
6. импорт в branch JSON;
7. импорт в equipment-profile JSON;
8. branch JSON `<->` DTTS;
9. equipment-profile JSON `<->` DTTS;
10. branch JSON `<->` equipment-profile JSON;
11. версии шаблонов;
12. отличие `templateVersion` от `formatVersion`;
13. как получить номер версии шаблона;
14. как версии возвращаются в итоговом JSON;
15. default values параметров шаблона;
16. порядок применения default values;
17. как формируются итоговые значения параметров;
18. merge strategies;
19. version conflict strategies;
20. validation;
21. ограничения;
22. расширение формата в будущем;
23. типовые ошибки и диагностика.

---

## 19. JavaDoc

JavaDoc обязателен для:

* всех public interfaces;
* всех public classes;
* всех DTO;
* всех enum;
* всех исключений;
* всех контроллеров;
* ключевых внутренних сервисов;
* package-info.java.

JavaDoc должен быть:

* на русском языке;
* объясняющим, а не формальным;
* согласованным с кодом;
* согласованным с README;
* согласованным со Swagger.

---

## 20. Обязательные исключения

Нужно использовать собственные исключения:

* `DttsFormatException`
* `TemplateValidationException`
* `TemplateImportException`
* `TemplateExportException`
* `EquipmentProfileTransformationException`
* `TemplateVersionConflictException`
* `TemplateDefaultValueResolutionException`

---

## 21. Обязательные тесты

### 21.1. Архив и round-trip

* экспорт `.dtts` из branch JSON;
* экспорт `.dtts` из equipment-profile JSON;
* импорт `.dtts` в branch JSON;
* импорт `.dtts` в equipment-profile JSON;
* round-trip для branch JSON;
* round-trip для equipment-profile JSON;
* cross-round-trip между моделями;
* deterministic zip.

### 21.2. Предметная модель

* тип устройства без устройств;
* тип устройства с несколькими экземплярами;
* разные схемы параметров устройств;
* вложенные объектные параметры;
* пустые/null скрипты;
* несколько event handlers;
* несколько commands.

### 21.3. Версии

* чтение версии из `.dtts`;
* экспорт с версией;
* импорт с версией;
* возврат версии в branch JSON;
* возврат версии в equipment-profile JSON;
* inspect/validate/import/export result с версиями;
* equal version scenario;
* upgrade scenario;
* downgrade conflict;
* compareVersions.

### 21.4. Default values

Обязательные тесты:

* параметр без явного значения использует `defaultEquipmentValue`;
* если нет `defaultEquipmentValue`, используется `defaultValue`;
* если request задает значение — оно имеет приоритет над default;
* nested object parameter uses default values;
* device parameter uses template default;
* branch JSON generation fills defaults;
* equipment-profile JSON generation fills defaults;
* preview result показывает, что значение взято из default;
* validation detects invalid default type;
* null/default interaction for nullable and non-nullable fields.

### 21.5. REST и OpenAPI

* контроллеры demo-service;
* генерация Swagger/OpenAPI;
* version endpoints;
* default resolution endpoints/flows.

---

## 22. Качество кода

Обязательные правила:

* Java 17;
* Micronaut 4.x;
* Maven;
* без Lombok;
* без бессмысленных TODO;
* без временных заглушек;
* без `@SuppressWarnings`, если нет реальной причины;
* чистый, читаемый, расширяемый код;
* иммутабельные DTO там, где возможно;
* builder/factory там, где это оправдано;
* без магических строк для формата и имен архивных entry.

---

## 23. Что агент не должен делать

Запрещено:

* терять Groovy-код;
* преобразовывать скрипты “для красоты”;
* смешивать схему и конкретные значения в одну неразличимую структуру;
* смешивать `exampleValue` и `defaultEquipmentValue`;
* игнорировать версии шаблонов;
* генерировать итоговый JSON без возврата версий шаблонов;
* оставлять неявное поведение default values без документации и тестов;
* делать декоративный demo-service без реальных endpoint flows;
* документировать несуществующие классы или endpoint’ы.

---

## 24. Приоритеты реализации

Если реализация ведется поэтапно, порядок приоритета такой:

1. canonical model;
2. version model;
3. parameter schema + default values;
4. archive reader/writer;
5. export/import services;
6. branch JSON parser/generator;
7. equipment-profile JSON parser/generator;
8. cross-model transformation;
9. validation;
10. demo-service;
11. Swagger;
12. README/JavaDoc;
13. exhaustive tests.

---

## 25. Правила согласованности результата

Перед завершением работы обязательно проверить:

* что код компилируется;
* что тесты соответствуют архитектуре;
* что README описывает реально существующие классы;
* что JavaDoc не расходится с кодом;
* что OpenAPI не расходится с контроллерами;
* что версии шаблонов реально возвращаются в JSON;
* что default values реально подставляются;
* что preview показывает происхождение значений;
* что один и тот же кейс поддержан:

  * в library,
  * в demo-service,
  * в README,
  * в JavaDoc,
  * в тестах.

---

## 26. Definition of Done

Задача считается завершенной только если одновременно выполнены все условия:

1. `.dtts` поддерживает шаблон как для branch JSON, так и для equipment-profile JSON.
2. У каждого шаблона есть версия.
3. Версию шаблона можно получить отдельно.
4. Версии шаблонов возвращаются в итоговых JSON.
5. У параметров шаблона есть `defaultValue` и `defaultEquipmentValue`.
6. При отсутствии явного значения генерация использует значение по умолчанию из шаблона.
7. Есть полные тесты на version handling и default value resolution.
8. Demo-service предоставляет реальные endpoint’ы.
9. Swagger UI показывает версии и default values.
10. README и JavaDoc подробно описывают всё перечисленное.

---

## 27. Ожидаемый результат от агента

Агент должен выдать:

1. полный multi-module Maven project;
2. полный исходный код;
3. полный `README.md`;
4. полный JavaDoc в исходниках;
5. полный набор тестов;
6. рабочий demo-service;
7. примеры `.dtts`;
8. примеры branch JSON;
9. примеры equipment-profile JSON;
10. примеры JSON с версиями шаблонов;
11. примеры JSON с подставленными default values;
12. примеры version conflict handling.

---

## 28. Итоговый смысл проекта в одной фразе

Нужно реализовать расширяемую библиотеку и demo-service, которые умеют переносить, версионировать, валидировать и применять шаблоны типов оборудования через формат `.dtts` между branch JSON и equipment-profile JSON, при этом сохраняя Groovy-код, версии шаблонов и корректно подставляя значения параметров по умолчанию.
