package ru.mirea.kinostudio.fx.controller;

import javafx.fxml.FXML;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import ru.mirea.kinostudio.exception.BusinessException;
import ru.mirea.kinostudio.exception.EntityNotFoundException;
import ru.mirea.kinostudio.fx.AppContext;
import ru.mirea.kinostudio.fx.Converters;
import ru.mirea.kinostudio.fx.DbErrors;
import ru.mirea.kinostudio.fx.Dialogs;
import ru.mirea.kinostudio.fx.FormController;
import ru.mirea.kinostudio.fx.FormErrors;
import ru.mirea.kinostudio.model.Client;
import ru.mirea.kinostudio.model.Location;
import ru.mirea.kinostudio.model.ShootingRequest;
import ru.mirea.kinostudio.util.Labels;

import java.math.BigDecimal;
import java.sql.SQLException;
import java.time.LocalDate;

/**
 * Форма «Заявка на съёмку»: добавление и редактирование.
 * Клиент и локация выбираются из выпадающих списков (а не вводом id) —
 * так пользователь не может сослаться на несуществующую запись.
 */
public class RequestDialogController implements FormController {

    private static final int MAX_HOURS = 240;
    private static final BigDecimal MAX_BUDGET = new BigDecimal("9999999999.99"); // NUMERIC(12,2)

    private final AppContext ctx = AppContext.get();

    @FXML private ComboBox<Client> clientBox;
    @FXML private ComboBox<Location> locationBox;
    @FXML private TextField titleField;
    @FXML private TextArea descriptionArea;
    @FXML private DatePicker datePicker;
    @FXML private TextField durationField;
    @FXML private TextField budgetField;
    @FXML private Label statusInfo;
    @FXML private Label errorLabel;

    private FormErrors errors;
    private ShootingRequest editing;

    @FXML
    private void initialize() {
        errors = new FormErrors(errorLabel);
        clientBox.setItems(ctx.clients());
        clientBox.setConverter(Converters.client());
        locationBox.setItems(ctx.locations());
        locationBox.setConverter(Converters.location());
        datePicker.setConverter(Converters.date());
        statusInfo.setText("Новая (статус меняется кнопкой «Сменить статус»)");
    }

    /** null — создаём новую заявку; иначе редактируем переданную. */
    public void setRequest(ShootingRequest request) {
        this.editing = request;
        if (request == null) return;
        ctx.clients().stream().filter(c -> c.getId() == request.getClientId())
                .findFirst().ifPresent(clientBox::setValue);
        ctx.locations().stream().filter(l -> l.getId() == request.getLocationId())
                .findFirst().ifPresent(locationBox::setValue);
        titleField.setText(request.getTitle());
        descriptionArea.setText(request.getDescription());
        datePicker.setValue(request.getShootingDate());
        durationField.setText(String.valueOf(request.getDurationHours()));
        budgetField.setText(request.getBudget().toPlainString());
        statusInfo.setText(Labels.status(request.getStatus()));
    }

    @Override
    public boolean save() {
        errors.reset();

        Client client = clientBox.getValue();
        Location location = locationBox.getValue();
        String title = titleField.getText().trim();
        String description = descriptionArea.getText().trim();
        LocalDate date = datePicker.getValue();

        if (client == null) errors.add(clientBox, "Клиент: выберите клиента из списка.");
        if (location == null) errors.add(locationBox, "Локация: выберите локацию из списка.");

        if (title.isEmpty()) {
            errors.add(titleField, "Название: поле обязательно для заполнения.");
        } else if (title.length() > 200) {
            errors.add(titleField, "Название: не более 200 символов.");
        }

        if (description.isEmpty()) {
            errors.add(descriptionArea, "Описание: поле обязательно для заполнения.");
        } else if (description.length() > 2000) {
            errors.add(descriptionArea, "Описание: не более 2000 символов.");
        }

        if (date == null) {
            String typed = datePicker.getEditor().getText();
            errors.add(datePicker, typed == null || typed.isBlank()
                    ? "Дата съёмки: укажите дату."
                    : "Дата съёмки: неверный формат, введите дд.мм.гггг (например, 25.12.2026).");
        }

        int duration = 0;
        String durationText = durationField.getText().trim();
        if (durationText.isEmpty()) {
            errors.add(durationField, "Длительность: поле обязательно для заполнения.");
        } else {
            try {
                duration = Integer.parseInt(durationText);
                if (duration < 1 || duration > MAX_HOURS) {
                    errors.add(durationField, "Длительность: введите число часов от 1 до " + MAX_HOURS + ".");
                }
            } catch (NumberFormatException e) {
                errors.add(durationField, "Длительность: введите целое число часов, например 8.");
            }
        }

        BigDecimal budget = BigDecimal.ZERO;
        String budgetText = budgetField.getText().trim().replace(" ", "").replace(',', '.');
        if (budgetText.isEmpty()) {
            errors.add(budgetField, "Бюджет: поле обязательно для заполнения.");
        } else {
            try {
                budget = new BigDecimal(budgetText);
                if (budget.signum() < 0) {
                    errors.add(budgetField, "Бюджет: не может быть отрицательным.");
                } else if (budget.scale() > 2) {
                    errors.add(budgetField, "Бюджет: не более 2 знаков после запятой.");
                } else if (budget.compareTo(MAX_BUDGET) > 0) {
                    errors.add(budgetField, "Бюджет: слишком большая сумма.");
                }
            } catch (NumberFormatException e) {
                errors.add(budgetField, "Бюджет: введите число, например 150000 или 150000.50.");
            }
        }

        if (errors.showIfAny()) return false;

        try {
            if (editing == null) {
                ctx.requestService.create(client.getId(), location.getId(), title, description,
                        date, duration, budget);
            } else {
                ctx.requestService.update(editing.getId(), client.getId(), location.getId(), title,
                        description, date, duration, budget);
            }
            return true;
        } catch (BusinessException | EntityNotFoundException e) {
            // Бизнес-правила КР №1: дата не в прошлом, клиент и локация существуют и т.д.
            errors.fail(e.getMessage());
        } catch (SQLException e) {
            Dialogs.logDb(e);
            errors.fail(DbErrors.describe(e));
        }
        return false;
    }
}
