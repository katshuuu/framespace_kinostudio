package ru.mirea.kinostudio.fx.controller;

import javafx.fxml.FXML;
import javafx.scene.control.Label;
import ru.mirea.kinostudio.fx.AppContext;
import ru.mirea.kinostudio.util.DatabaseManager;

/**
 * Контроллер главного окна. Вкладки подключены через fx:include, у каждой свой контроллер —
 * поэтому «гигантского» контроллера нет: здесь только шапка и строка состояния.
 */
public class MainController {

    @FXML private Label dbLabel;

    @FXML
    private void initialize() {
        dbLabel.setText("БД: " + DatabaseManager.getUrl());
    }

    @FXML
    private void onRefresh() {
        AppContext.get().dataChanged();
    }
}
