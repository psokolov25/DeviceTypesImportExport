# Project TODO / План развития

Документ фиксирует технический анализ текущего состояния и ближайшие шаги развития.

## 1) Ключевые зоны развития

### 1.1 Архитектурное усиление canonical-layer
- [ ] Расширить типизированные canonical-проекции для более полного покрытия nested array/object схем в branch/profile сценариях.
- [ ] Формализовать контракт совместимости `formatVersion` + миграции для будущих breaking-изменений формата.
- [ ] Добавить отдельные тесты инвариантов round-trip для mixed nullable/object/array параметров.

### 1.2 Качество API и диагностика
- [x] Уточнить mapping исключений библиотеки в demo-service по единым HTTP-контрактам (400/409/422/500).
- [x] Добавить систематизированные error-codes для импорт/экспорт/assembly ошибок.
- [ ] Расширить описание troubleshooting-кейсов в OpenAPI примерах.

### 1.3 Надёжность CI и гигиена репозитория
- [x] Удалить случайно закоммиченные артефакты (`.class`, временные файлы) из `src/main/java`.
- [x] Добавить автоматическую проверку, блокирующую бинарные/временные файлы в дереве исходников demo-service.
- [x] Добавить аналогичную проверку для других модулей (library/test-support при добавлении).

### 1.4 Документация и onboarding
- [x] Добавить публичный TODO/roadmap документ с приоритизацией.
- [ ] Добавить таблицу "сценарий -> рекомендуемый API метода фасада" для быстрого выбора интеграционного пути.
- [ ] Добавить отдельный раздел "частые ошибки интеграции" с примерами payload и expected behavior.

## 2) Что начато в рамках текущей итерации

В этой итерации начата реализация блока **«Надёжность CI и гигиена репозитория»**:

1. Удалены нецелевые файлы из исходников demo-service.
2. Добавлен тест `SourceTreeHygieneTest`, который не позволит повторно добавить `.class` и временные артефакты в `src/main/java`.
3. В demo-service введены системные error-codes (`INVALID_ARGUMENT`, `DTT_FORMAT_ERROR`, `TEMPLATE_*`, `INTERNAL_ERROR`) для стабильной машинной обработки ошибок клиентами API.
4. Добавлен аналогичный `SourceTreeHygieneTest` для `device-template-library`.
5. Добавлен canonical round-trip тест для nullable + nested array/object metadata (`DefaultCanonicalTemplateMapperTest`).
6. Добавлен тест проекционного инварианта `Array` без default value: сохраняется `items`-схема в profile projection (`DefaultCanonicalProjectionMapperTest`).

## 3) Приоритеты следующей итерации

1. Добавить расширенные round-trip тесты nested nullable/object/array схем.
2. Расширить документацию по troubleshooting и интеграционным anti-pattern.
