package ru.aritmos.dtt.demo.config;

import io.micronaut.context.annotation.Factory;
import jakarta.inject.Singleton;
import ru.aritmos.dtt.api.DeviceTemplateLibrary;
import ru.aritmos.dtt.api.DeviceTemplateLibraryFacade;

/**
 * Фабрика бинa фасада библиотеки DTT для demo-service.
 *
 * <p>Demo-модуль использует библиотеку как внешнюю зависимость через публичный фасад,
 * а не через прямую ручную сборку внутренних core-сервисов.</p>
 */
@Factory
public class DeviceTemplateLibraryFacadeFactory {

    /**
     * @return дефолтный фасад библиотеки DTT
     */
    @Singleton
    public DeviceTemplateLibraryFacade deviceTemplateLibraryFacade() {
        return DeviceTemplateLibrary.createDefaultFacade();
    }
}
