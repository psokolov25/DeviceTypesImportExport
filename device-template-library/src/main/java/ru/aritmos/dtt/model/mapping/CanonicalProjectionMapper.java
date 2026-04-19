package ru.aritmos.dtt.model.mapping;

import ru.aritmos.dtt.model.canonical.CanonicalBranchProjection;
import ru.aritmos.dtt.model.canonical.CanonicalDeviceTypeTemplate;
import ru.aritmos.dtt.model.canonical.CanonicalProfileProjection;

/**
 * Маппер канонического шаблона в прикладные проекции profile/branch.
 */
public interface CanonicalProjectionMapper {

    /**
     * Проецирует канонический шаблон в `deviceTypeParamValues` profile JSON.
     *
     * @param template канонический шаблон
     * @return profile-проекция
     */
    CanonicalProfileProjection toProfileProjection(CanonicalDeviceTypeTemplate template);

    /**
     * Проецирует канонический шаблон в branch-контекст.
     *
     * @param template канонический шаблон
     * @param kind значение поля `type` для branch deviceType
     * @return branch-проекция
     */
    CanonicalBranchProjection toBranchProjection(CanonicalDeviceTypeTemplate template, String kind);
}

