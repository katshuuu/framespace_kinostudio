package ru.mirea.kinostudio.fx;

import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ChoiceDialog;
import javafx.scene.layout.Region;
import javafx.stage.Window;

import java.sql.SQLException;
import java.util.List;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Готовые диалоговые окна: ошибка, предупреждение, информация, подтверждение, выбор.
 * Пользователь видит понятный текст, а подробности (stack trace) уходят только в журнал.
 */
public final class Dialogs {

    private static final Logger LOG = Logger.getLogger(Dialogs.class.getName());

    private Dialogs() {
    }

    public static Window ownerOf(Node node) {
        return node != null && node.getScene() != null ? node.getScene().getWindow() : null;
    }

    public static void error(Window owner, String message) {
        show(Alert.AlertType.ERROR, owner, "Ошибка", message);
    }

    public static void warning(Window owner, String message) {
        show(Alert.AlertType.WARNING, owner, "Проверьте данные", message);
    }

    public static void info(Window owner, String message) {
        show(Alert.AlertType.INFORMATION, owner, "Информация", message);
    }

    public static void dbError(Window owner, SQLException e) {
        logDb(e);
        error(owner, DbErrors.describe(e));
    }

    /** Записывает подробности ошибки БД в журнал (пользователю stack trace не показываем). */
    public static void logDb(SQLException e) {
        LOG.log(Level.SEVERE, "Ошибка базы данных (SQLState=" + e.getSQLState() + ")", e);
    }

    /** Подтверждение опасного действия (например, удаления). */
    public static boolean confirm(Window owner, String message, String confirmText) {
        ButtonType yes = new ButtonType(confirmText, javafx.scene.control.ButtonBar.ButtonData.OK_DONE);
        ButtonType no = new ButtonType("Отмена", javafx.scene.control.ButtonBar.ButtonData.CANCEL_CLOSE);
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION, message, yes, no);
        prepare(alert, owner, "Подтверждение");
        return alert.showAndWait().filter(b -> b == yes).isPresent();
    }

    /** Выбор одного варианта из списка. */
    public static Optional<String> choose(Window owner, String title, String header, List<String> options) {
        ChoiceDialog<String> dialog = new ChoiceDialog<>(options.get(0), options);
        dialog.initOwner(owner);
        dialog.setTitle(title);
        dialog.setHeaderText(header);
        dialog.setContentText("Статус:");
        dialog.getDialogPane().getStylesheets().add(FxApp.css());
        return dialog.showAndWait();
    }

    private static void show(Alert.AlertType type, Window owner, String title, String message) {
        Alert alert = new Alert(type, message, ButtonType.OK);
        prepare(alert, owner, title);
        alert.showAndWait();
    }

    private static void prepare(Alert alert, Window owner, String title) {
        if (owner != null) alert.initOwner(owner);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.getDialogPane().getStylesheets().add(FxApp.css());
        alert.getDialogPane().setMinHeight(Region.USE_PREF_SIZE);
    }
}
