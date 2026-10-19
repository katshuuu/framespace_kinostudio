package ru.mirea.kinostudio.fx.controller;

import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import ru.mirea.kinostudio.exception.BusinessException;
import ru.mirea.kinostudio.exception.EntityNotFoundException;
import ru.mirea.kinostudio.fx.AppContext;
import ru.mirea.kinostudio.fx.DbErrors;
import ru.mirea.kinostudio.fx.Dialogs;
import ru.mirea.kinostudio.fx.FormController;
import ru.mirea.kinostudio.fx.FormErrors;
import ru.mirea.kinostudio.model.Client;

import java.sql.SQLException;
import java.util.regex.Pattern;

/** Форма «Клиент»: добавление и редактирование. */
public class ClientDialogController implements FormController {

    private static final Pattern EMAIL = Pattern.compile("^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");
    private static final Pattern PHONE_CHARS = Pattern.compile("^[0-9+()\\s-]+$");

    private final AppContext ctx = AppContext.get();

    @FXML private TextField nameField;
    @FXML private TextField emailField;
    @FXML private TextField phoneField;
    @FXML private Label errorLabel;

    private FormErrors errors;
    private Client editing;

    @FXML
    private void initialize() {
        errors = new FormErrors(errorLabel);
    }

    /** null — создаём нового клиента; иначе редактируем переданного. */
    public void setClient(Client client) {
        this.editing = client;
        if (client != null) {
            nameField.setText(client.getFullName());
            emailField.setText(client.getEmail());
            phoneField.setText(client.getPhone());
        }
    }

    @Override
    public boolean save() {
        errors.reset();
        String name = nameField.getText().trim();
        String email = emailField.getText().trim();
        String phone = phoneField.getText().trim();

        if (name.isEmpty()) {
            errors.add(nameField, "ФИО: поле обязательно для заполнения.");
        } else if (name.length() > 150) {
            errors.add(nameField, "ФИО: не более 150 символов.");
        }

        if (email.isEmpty()) {
            errors.add(emailField, "Email: поле обязательно для заполнения.");
        } else if (email.length() > 150) {
            errors.add(emailField, "Email: не более 150 символов.");
        } else if (!EMAIL.matcher(email).matches()) {
            errors.add(emailField, "Email: введите адрес в формате name@example.com.");
        }

        if (phone.isEmpty()) {
            errors.add(phoneField, "Телефон: поле обязательно для заполнения.");
        } else if (!PHONE_CHARS.matcher(phone).matches()) {
            errors.add(phoneField, "Телефон: допустимы только цифры, пробелы, «+», «-» и скобки.");
        } else if (phone.replaceAll("\\D", "").length() < 10 || phone.length() > 30) {
            errors.add(phoneField, "Телефон: нужно минимум 10 цифр (не более 30 символов), например +7 900 111-22-33.");
        }

        if (errors.showIfAny()) return false;

        try {
            if (editing == null) {
                ctx.clientService.create(name, email, phone);
            } else {
                ctx.clientService.update(editing.getId(), name, email, phone);
            }
            return true;
        } catch (BusinessException | EntityNotFoundException e) {
            errors.fail(e.getMessage());
        } catch (SQLException e) {
            Dialogs.logDb(e);
            errors.fail(DbErrors.describe(e));
        }
        return false;
    }
}
