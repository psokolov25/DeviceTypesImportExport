package ru.aritmos.dtt.model.canonical;

import java.util.Map;

/**
 * Каноническая модель происхождения шаблона (`template-origin.yml`).
 *
 * @param sourceKind тип источника (`PROFILE_JSON`, `BRANCH_EQUIPMENT_JSON`, ...)
 * @param sourceSummary краткое описание источника
 * @param metadata дополнительные поля происхождения
 */
public record CanonicalTemplateOrigin(
        String sourceKind,
        String sourceSummary,
        Map<String, Object> metadata
) {
}

