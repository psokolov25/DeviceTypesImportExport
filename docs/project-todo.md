# Project TODO / План развития

Документ фиксирует технический анализ текущего состояния и ближайшие шаги развития.

## 1) Ключевые зоны развития

### 1.1 Архитектурное усиление canonical-layer
- [x] Расширить типизированные canonical-проекции для более полного покрытия nested array/object схем в branch/profile сценариях (добавлены branch/profile projection инварианты).
- [x] Формализовать контракт совместимости `formatVersion` + миграции для будущих breaking-изменений формата (валидационный код `UNSUPPORTED_FORMAT_VERSION` для неподдерживаемых `formatVersion`).
- [x] Добавить отдельные тесты инвариантов round-trip для mixed nullable/object/array параметров.

### 1.2 Качество API и диагностика
- [x] Уточнить mapping исключений библиотеки в demo-service по единым HTTP-контрактам (400/409/422/500).
- [x] Добавить систематизированные error-codes для импорт/экспорт/assembly ошибок.
- [x] Расширить описание troubleshooting-кейсов в OpenAPI примерах.

### 1.3 Надёжность CI и гигиена репозитория
- [x] Удалить случайно закоммиченные артефакты (`.class`, временные файлы) из `src/main/java`.
- [x] Добавить автоматическую проверку, блокирующую бинарные/временные файлы в дереве исходников demo-service.
- [x] Добавить аналогичную проверку для других модулей (library/test-support при добавлении).

### 1.4 Документация и onboarding
- [x] Добавить публичный TODO/roadmap документ с приоритизацией.
- [x] Добавить таблицу "сценарий -> рекомендуемый API метода фасада" для быстрого выбора интеграционного пути.
- [x] Добавить отдельный раздел "частые ошибки интеграции" с примерами payload и expected behavior.

## 2) Что начато в рамках текущей итерации

В этой итерации начата реализация блока **«Надёжность CI и гигиена репозитория»**:

1. Удалены нецелевые файлы из исходников demo-service.
2. Добавлен тест `SourceTreeHygieneTest`, который не позволит повторно добавить `.class` и временные артефакты в `src/main/java`.
3. В demo-service введены системные error-codes (`INVALID_ARGUMENT`, `DTT_FORMAT_ERROR`, `TEMPLATE_*`, `INTERNAL_ERROR`) для стабильной машинной обработки ошибок клиентами API.
4. Добавлен аналогичный `SourceTreeHygieneTest` для `device-template-library`.
5. Добавлен canonical round-trip тест для nullable + nested array/object metadata (`DefaultCanonicalTemplateMapperTest`).
6. Добавлен тест проекционного инварианта `Array` без default value: сохраняется `items`-схема в profile projection (`DefaultCanonicalProjectionMapperTest`).

## 3) Приоритеты следующей итерации

1. [x] Добавить расширенные round-trip тесты nested nullable/object/array схем.
2. [x] Продолжить детализацию JavaDoc и developer-guide для high-level import-plan API и branch-specific topology.


7. В библиотеку перенесён orchestration-слой direct import-plan execution (`assembleProfile/assembleBranch` по high-level plan, а также merge в existing branch JSON/model).
8. Demo-service упрощён: structured import и merge в existing branch больше не собираются вручную через `prepare -> assemble -> merge`, а делегируются в facade API библиотеки.
9. Добавлен отдельный документ `docs/feature-catalog.md` с полным каталогом фич, терминов, merge-стратегий и прикладных кейсов.

## 4) Следующая итерация

1. [x] Добавлены отдельные facade-методы preview-диагностики для zip-based import-plan; библиотека сама разрешает `archiveEntryName` и считает preview для structured и legacy zip-сценариев.
2. [x] Детальный preview import-plan вынесен в library facade как отдельный контракт (`previewProfileImportDetailed`, `previewBranchImportDetailed`), чтобы demo-service не склеивал вручную `assemble + computePreview`.
3. [x] Продолжить вынос вспомогательной экспортной orchestration-логики из demo-service в library facade там, где она ещё не даёт существенного библиотечного контракта (single-export Base64 вынесен в facade).
4. [x] Добавить офлайн-сценарий сборки/прогона тестов в контейнере без скачивания Maven Wrapper дистрибутива.

- [x] Совместная сборка profile и branch с наследованием metadata вынесена в библиотеку.
- [x] Детальный preview profile/branch import теперь сводится к прямому thin-adapter поверх фасада библиотеки.
- [x] Проверить, какие ещё сценарии demo-service можно свести к прямому thin-adapter поверх фасада библиотеки без промежуточной ручной оркестрации (single-export response-path переведён на прямые методы фасада).
- [x] Preview-диагностика high-level import-plan теперь покрывает legacy Base64, legacy zip, structured Base64 и structured zip сценарии.
- [x] Preview high-level import-plan вынесен в отдельные facade-методы (`previewProfileImport`, `previewBranchImport`, включая zip-перегрузки), чтобы demo-service не вызывал `assemble...` в preview endpoint-ах.
- [x] Single-export response orchestration (совместная отдача `bytes + Base64`) вынесена в facade через `ExportResult`, demo-service переведён на thin-adapter делегирование.
- [x] DTT inspect orchestration вынесен в facade (`inspectDtt` + `DttInspectionResult`), demo-service больше не собирает inspect-результат вручную из archive DTO.
- [x] Пост-обработка assemble-результатов (JSON + агрегированные счётчики + metadata) вынесена в facade (`toProfileAssemblyView`, `toBranchAssemblyView`), demo-service больше не дублирует расчёты вручную.
- [x] Детальный preview import-plan получил прикладные view-контракты в библиотеке (`previewProfileImportView`, `previewBranchImportView`, включая zip-перегрузки): demo-service больше не дублирует сериализацию JSON и агрегированные счётчики для detailed preview endpoint-ов.
- [x] Legacy preview API получил прикладные view-контракты (`previewDttSetToProfileView`, `previewDttBase64SetToProfileView`, `previewDttZipToProfileView`, `previewDttSetToBranchView`, `previewDttBase64SetToBranchView`, `previewDttZipToBranchView`), что убрало дублирование JSON/counts orchestration в demo-service.
- [x] Legacy import API получил прикладные view-контракты (`importDttSetToProfileView`, `importDttBase64SetToProfileView`, `importDttZipToProfileView`, `importDttSetToBranchView`, `importDttBase64SetToBranchView`, `importDttZipToBranchView`, включая existing-branch варианты), что убрало дублирование JSON/counts orchestration в demo-service.
- [x] Нормализация metadata в branch response-path вынесена в библиотеку (`toNormalizedBranchAssemblyView` + использование в import/preview view), demo-service больше не вызывает `normalizeDeviceTypeMetadata(...)` для branch-ответов.
- [x] Batch export response-path вынесен в библиотеку через `BatchDttExportView` (`exportDttSetFromProfileView`, `exportDttSetFromBranchView`), demo-service больше не считает `encoded.size()` вручную.
- [x] Transport request DTO demo-service переведены на переиспользование library import-plan типов (`ProfileDeviceTypeImportSourceRequest`, `BranchImportSourceRequest`), что убрало основную часть ручного nested mapping в demo-service.
- [x] Нормализация metadata типов устройств (fallback name/displayName/description + default icon) вынесена в facade (`normalizeDeviceTypeMetadata`), demo-service больше не дублирует эту логику.
- [x] Добавлен скрипт `scripts/offline-test.sh` для офлайн прогона тестов (`-o`) через Maven Wrapper или system Maven.

## 5) Задачи следующей итерации (после текущего переноса)

1. [x] Вынести из demo-service в библиотеку typed adapter для import-plan request mapping (конвертация transport DTO -> library plan DTO), чтобы demo-service оставался полностью transport-only.
2. [x] Добавить в facade ready-to-use preview-view контракты для legacy preview API (`previewDttSetToProfile`, `previewDttSetToBranch`, zip/base64 режимы), чтобы унифицировать response orchestration.
3. [x] Расширить каталог фич sequence-диаграммами по сценариям `import-plan -> preview-view -> apply`, включая merge-конфликты по каждой стратегии.
4. [x] Добавить import-view методы для high-level import-plan orchestration (`assembleProfile/assembleBranch/mergeIntoExistingBranch`) с единым контрактом JSON/counts на уровне apply/merge (`assembleProfileView`, `assembleBranchView`, `mergeIntoExistingBranchJsonView`).
5. [x] Убрать ручной mapping preview-computation DTO в demo-service: detailed preview endpoint-ы используют `ImportPreviewComputationEntry` из библиотеки напрямую.

## 6) Задачи следующей итерации (новый цикл)

1. [x] Вынести детальную apply-диагностику в отдельные facade DTO для high-level apply/merge (по аналогии с detailed preview), чтобы в одном ответе были JSON/counts и карты defaults/overrides после фактического применения.
2. [x] Свести оставшийся top-level mapping `toLibraryProfileImportPlan(...)`/`toLibraryBranchImportPlan(...)` к отдельному reusable adapter-компоненту demo-service с минимальным сервисным кодом (`ImportPlanRequestMapper`).
3. [x] Добавить sequence-диаграммы для всех merge-стратегий (`FAIL_IF_EXISTS`, `REPLACE`, `MERGE_NON_NULLS`, `MERGE_PRESERVE_EXISTING`, `CREATE_COPY_WITH_SUFFIX`) в apply/preview сценариях.
4. [x] Дополнить feature-catalog матрицей «входные ограничения -> ожидаемая диагностика -> рекомендуемый endpoint фасада» для ошибок валидации и merge-конфликтов.

## 7) Что ещё осталось перенести в библиотеку

1. [x] Унификация примеров OpenAPI/README/feature-catalog вокруг новых apply-view контрактов как основного API для structured apply-эндпоинтов.
2. [x] Полное выравнивание всех endpoint-примеров demo-service (multipart/base64/json) под один стиль диагностических ответов и error-code описаний.
3. [x] Автоматическая генерация SVG по новым PlantUML диаграммам в CI (чтобы docs/plantuml и docs/plantuml/svg оставались синхронизированы).

## 8) Следующий цикл задач

1. [x] Добавить контрактные OpenAPI тесты, валидирующие присутствие apply-view методов в примерах structured endpoint-ов.
2. [x] Добавить smoke-проверку скрипта `generate-plantuml-svg.sh` в документационном пайплайне без Docker (через локальный JAR fallback).
3. [x] Уточнить в developer-guide пошаговую миграцию с `assembleProfile(...)`/`assembleBranch(...)` на `assemble*ApplyView(...)` для существующих интеграций.
