package ru.mirea.kinostudio.fx.controller;

import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.stage.Stage;
import ru.mirea.kinostudio.fx.AppContext;
import ru.mirea.kinostudio.fx.Dialogs;
import ru.mirea.kinostudio.fx.FxApp;
import ru.mirea.kinostudio.model.User;
import ru.mirea.kinostudio.util.DatabaseManager;

/**
 * Контроллер главного окна. Вкладки подключены через fx:include, у каждой свой контроллер —
 * поэтому «гигантского» контроллера нет: здесь только шапка (пользователь, выход) и строка состояния.
 */
public class MainController {

    @FXML private Label dbLabel;
    @FXML private Label userLabel;

    @FXML
    private void initialize() {
        dbLabel.setText("БД: " + DatabaseManager.getUrl());
        User u = AppContext.get().currentUser();
        if (u != null) {
            userLabel.setText(u.login() + (u.isAdmin() ? " · администратор" : " · пользователь"));
        }
    }

    @FXML
    private void onRefresh() {
        AppContext.get().dataChanged();
    }

    @FXML
    private void onLogout() {
        if (!Dialogs.confirm(Dialogs.ownerOf(dbLabel), "Выйти из системы?", "Выйти")) return;
        AppContext.get().logout();
        FxApp.showLogin((Stage) dbLabel.getScene().getWindow());
    }
}
