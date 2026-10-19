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
 */
public class FxApp extends Application {

    @Override
    public void start(Stage stage) throws IOException {
        // Проверяем подключение к БД до показа окна: если базы нет — понятное сообщение, а не пустое окно
        try {
            AppContext.get().reloadReferences();
        } catch (SQLException e) {
            Dialogs.dbError(null, e);
            Platform.exit();
            return;
        }

        FXMLLoader loader = new FXMLLoader(FxApp.class.getResource("/fxml/main.fxml"));
        Parent root = loader.load();
        Scene scene = new Scene(root, 1200, 780);
        scene.getStylesheets().add(css());

        stage.setTitle("Киностудия — заявки на съёмку");
        stage.setMinWidth(1000);
        stage.setMinHeight(660);
        stage.setScene(scene);
        stage.show();
    }

    /** Адрес общего файла стилей (применяется и к главному окну, и к диалогам). */
    public static String css() {
        return FxApp.class.getResource("/css/app.css").toExternalForm();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
