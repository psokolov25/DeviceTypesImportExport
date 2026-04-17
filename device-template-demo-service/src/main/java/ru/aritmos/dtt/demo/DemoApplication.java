package ru.aritmos.dtt.demo;

import io.micronaut.runtime.Micronaut;

/**
 * Точка входа demo-service на Micronaut.
 * <p>
 * Класс используется для запуска HTTP-сервиса, который демонстрирует
 * сценарии валидации, инспекции, импорта и экспорта DTT через публичный API библиотеки.
 */
public final class DemoApplication {

    private DemoApplication() {
        // utility
    }

    /**
     * Запускает Micronaut-приложение demo-service.
     *
     * @param args аргументы командной строки
     */
    public static void main(String[] args) {
        Micronaut.run(DemoApplication.class, args);
    }
}
