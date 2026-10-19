package ru.mirea.kinostudio.fx;

import javafx.event.ActionEvent;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.DialogPane;
import javafx.stage.Window;

import java.io.IOException;
import java.util.function.Consumer;

/**
 * Открывает форму из FXML в модальном диалоге с кнопками «Сохранить» / «Отмена».
 * Кнопка «Сохранить» вызывает controller.save(): если данные неверны, окно не закрывается.
 */
public final class FormDialogs {

    private FormDialogs() {
    }

    /** @return true, если пользователь успешно сохранил данные */
    public static <C extends FormController> boolean show(Window owner, String title, String fxml,
                                                          Consumer<C> init) {
        try {
            FXMLLoader loader = new FXMLLoader(FormDialogs.class.getResource(fxml));
            Parent content = loader.load();
            C controller = loader.getController();
            if (init != null) init.accept(controller);

            Dialog<ButtonType> dialog = new Dialog<>();
            if (owner != null) dialog.initOwner(owner);
            dialog.setTitle(title);

            DialogPane pane = dialog.getDialogPane();
            pane.setContent(content);
            pane.getStylesheets().add(FxApp.css());

            ButtonType save = new ButtonType("Сохранить", ButtonBar.ButtonData.OK_DONE);
            ButtonType cancel = new ButtonType("Отмена", ButtonBar.ButtonData.CANCEL_CLOSE);
            pane.getButtonTypes().addAll(save, cancel);

            Button saveButton = (Button) pane.lookupButton(save);
            saveButton.getStyleClass().add("primary");
            saveButton.addEventFilter(ActionEvent.ACTION, e -> {
                if (!controller.save()) e.consume(); // ошибки валидации: не закрываем окно
            });

            return dialog.showAndWait().filter(b -> b == save).isPresent();
        } catch (IOException e) {
            Dialogs.error(owner, "Не удалось открыть форму: " + e.getMessage());
            return false;
        }
    }
}
