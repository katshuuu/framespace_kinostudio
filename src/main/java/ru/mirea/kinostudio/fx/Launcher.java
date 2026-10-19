package ru.mirea.kinostudio.fx;

import javafx.application.Application;

/**
 * Точка входа для запуска из обычного (fat) jar: главный класс не должен наследовать Application,
 * иначе JavaFX не находит модули при запуске командой java -jar.
 */
public final class Launcher {
    private Launcher() {
    }

    public static void main(String[] args) {
        Application.launch(FxApp.class, args);
    }
}
