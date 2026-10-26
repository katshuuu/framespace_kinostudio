package ru.mirea.kinostudio.fx;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;
import java.sql.SQLException;

/**
 * Главный класс JavaFX-приложения.
 * Stage — окно, Scene — его содержимое, Node — любой элемент внутри сцены (дерево узлов).
 * Сначала в окно загружается сцена входа, после успешного входа — главная сцена.
 */
public class FxApp extends Application {

    @Override
    public void start(Stage stage) {
        // Проверяем подключение к БД до показа окна: если базы нет — понятное сообщение, а не пустое окно
        try {
            AppContext.get().authService.init();
        } catch (SQLException e) {
            Dialogs.dbError(null, e);
            Platform.exit();
            return;
        }
        stage.setTitle("Киностудия — заявки на съёмку");
        showLogin(stage);
        stage.show();
    }

    /** Сцена входа и регистрации. */
    public static void showLogin(Stage stage) {
        Scene scene = load("/fxml/login.fxml", 480, 620, stage);
        if (scene == null) return;
        stage.setMinWidth(440);
        stage.setMinHeight(560);
        stage.setScene(scene);
        stage.sizeToScene();
        stage.centerOnScreen();
    }

    /** Главная сцена (вызывается после успешного входа). */
    public static void showMain(Stage stage) {
        AppContext ctx = AppContext.get();
        ctx.clearListeners();
        try {
            ctx.reloadReferences();
        } catch (SQLException e) {
            Dialogs.dbError(Dialogs.ownerOf(stage.getScene().getRoot()), e);
            return;
        }
        Scene scene = load("/fxml/main.fxml", 1200, 780, stage);
        if (scene == null) return;
        stage.setMinWidth(1000);
        stage.setMinHeight(660);
        stage.setScene(scene);
        stage.sizeToScene();
        stage.centerOnScreen();
    }

    private static Scene load(String fxml, double width, double height, Stage stage) {
        try {
            Parent root = new FXMLLoader(FxApp.class.getResource(fxml)).load();
            Scene scene = new Scene(root, width, height);
            scene.getStylesheets().add(css());
            return scene;
        } catch (IOException e) {
            Dialogs.error(stage, "Не удалось загрузить окно: " + e.getMessage());
            return null;
        }
    }

    /** Адрес общего файла стилей (применяется и к главному окну, и к диалогам). */
    public static String css() {
        return FxApp.class.getResource("/css/app.css").toExternalForm();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
