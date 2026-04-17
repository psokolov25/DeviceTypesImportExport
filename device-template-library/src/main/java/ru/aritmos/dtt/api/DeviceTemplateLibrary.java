package ru.aritmos.dtt.api;

/**
 * Точка входа для приложений, которые подключают библиотеку DTT как зависимость.
 */
public final class DeviceTemplateLibrary {

    private DeviceTemplateLibrary() {
    }

    /**
     * Создаёт дефолтный фасад библиотеки с базовыми реализациями сервисов.
     *
     * @return фасад для работы через методный API
     */
    public static DeviceTemplateLibraryFacade createDefaultFacade() {
        return new DefaultDeviceTemplateLibraryFacade();
    }

    /**
     * Создаёт builder фасада для подмены отдельных реализаций сервисов.
     *
     * @return builder фасада
     */
    public static DeviceTemplateLibraryFacadeBuilder createFacadeBuilder() {
        return new DeviceTemplateLibraryFacadeBuilder();
    }
}
