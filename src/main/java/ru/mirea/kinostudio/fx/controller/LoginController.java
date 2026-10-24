package ru.mirea.kinostudio.fx.controller;

import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.stage.Stage;
import ru.mirea.kinostudio.exception.BusinessException;
import ru.mirea.kinostudio.fx.AppContext;
import ru.mirea.kinostudio.fx.Dialogs;
import ru.mirea.kinostudio.fx.FormErrors;
import ru.mirea.kinostudio.fx.FxApp;
import ru.mirea.kinostudio.model.User;

import java.sql.SQLException;

/** Окно входа и регистрации. Обычный пользователь регистрируется сам; администратор входит по данным из .env. */
public class LoginController {

    private final AppContext ctx = AppContext.get();

    @FXML private TextField loginField;
    @FXML private PasswordField passwordField;
    @FXML private Label loginError;

    @FXML private TextField regLoginField;
    @FXML private PasswordField regPasswordField;
    @FXML private PasswordField regConfirmField;
    @FXML private Label regError;

    private FormErrors loginErrors;
    private FormErrors regErrors;

    @FXML
    private void initialize() {
        loginErrors = new FormErrors(loginError);
        regErrors = new FormErrors(regError);
    }

    @FXML
    private void onLogin() {
        loginErrors.reset();
        String login = loginField.getText().trim();
        String password = passwordField.getText();
        if (login.isEmpty()) loginErrors.add(loginField, "Введите логин.");
        if (password.isEmpty()) loginErrors.add(passwordField, "Введите пароль.");
        if (loginErrors.showIfAny()) return;

        try {
            User user = ctx.authService.login(login, password);
            enter(user);
        } catch (BusinessException e) {
            loginErrors.add(passwordField, e.getMessage());
            loginErrors.showIfAny();
            passwordField.clear();
        } catch (SQLException e) {
            Dialogs.dbError(Dialogs.ownerOf(loginField), e);
        }
    }

    @FXML
    private void onRegister() {
        regErrors.reset();
        String login = regLoginField.getText().trim();
        String password = regPasswordField.getText();
        String confirm = regConfirmField.getText();
        if (login.isEmpty()) regErrors.add(regLoginField, "Введите логин.");
        if (password.isEmpty()) regErrors.add(regPasswordField, "Введите пароль.");
        if (confirm.isEmpty()) regErrors.add(regConfirmField, "Повторите пароль.");
        if (regErrors.showIfAny()) return;

        try {
            User user = ctx.authService.register(login, password, confirm);
            enter(user); // после успешной регистрации сразу входим
        } catch (BusinessException e) {
            regErrors.add(null, e.getMessage());
            regErrors.showIfAny();
        } catch (SQLException e) {
            Dialogs.dbError(Dialogs.ownerOf(regLoginField), e);
        }
    }

    private void enter(User user) {
        ctx.setCurrentUser(user);
        FxApp.showMain((Stage) loginField.getScene().getWindow());
    }
}
