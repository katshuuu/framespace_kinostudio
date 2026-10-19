package ru.mirea.kinostudio.fx;

import javafx.scene.control.Control;
import javafx.scene.control.Label;

import java.util.ArrayList;
import java.util.List;

/**
 * Накопитель ошибок валидации формы: подсвечивает неверные поля красной рамкой (CSS-класс «invalid»)
 * и выводит список понятных сообщений в метку внизу формы.
 */
public final class FormErrors {

    private final Label target;
    private final List<String> messages = new ArrayList<>();
    private final List<Control> marked = new ArrayList<>();

    public FormErrors(Label target) {
        this.target = target;
        reset();
    }

    public void reset() {
        messages.clear();
        marked.forEach(c -> c.getStyleClass().remove("invalid"));
        marked.clear();
        target.setText("");
        target.setVisible(false);
        target.setManaged(false);
    }

    public void add(Control field, String message) {
        messages.add(message);
        if (field != null && !field.getStyleClass().contains("invalid")) {
            field.getStyleClass().add("invalid");
            marked.add(field);
        }
    }

    public boolean hasErrors() {
        return !messages.isEmpty();
    }

    /** Показывает накопленные сообщения; возвращает true, если ошибки были. */
    public boolean showIfAny() {
        if (messages.isEmpty()) return false;
        display(String.join("\n", messages));
        return true;
    }

    /** Показывает одиночную ошибку (например, отказ бизнес-правила или БД). */
    public void fail(String message) {
        display(message);
    }

    private void display(String text) {
        target.setText(text);
        target.setVisible(true);
        target.setManaged(true);
    }
}
