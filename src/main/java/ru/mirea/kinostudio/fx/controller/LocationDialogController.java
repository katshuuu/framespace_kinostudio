package ru.mirea.kinostudio.fx.controller;

import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import ru.mirea.kinostudio.exception.BusinessException;
import ru.mirea.kinostudio.exception.EntityNotFoundException;
import ru.mirea.kinostudio.fx.AppContext;
import ru.mirea.kinostudio.fx.Converters;
import ru.mirea.kinostudio.fx.DbErrors;
import ru.mirea.kinostudio.fx.Dialogs;
import ru.mirea.kinostudio.fx.FormController;
import ru.mirea.kinostudio.fx.FormErrors;
import ru.mirea.kinostudio.model.Location;
import ru.mirea.kinostudio.model.LocationType;

import java.sql.SQLException;

/** Форма «Локация»: добавление и редактирование. */
public class LocationDialogController implements FormController {

    private static final int MAX_CAPACITY = 100_000;

    private final AppContext ctx = AppContext.get();

    @FXML private TextField nameField;
    @FXML private TextField addressField;
    @FXML private TextField capacityField;
    @FXML private ComboBox<LocationType> typeBox;
    @FXML private Label errorLabel;

    private FormErrors errors;
    private Location editing;

    @FXML
    private void initialize() {
        errors = new FormErrors(errorLabel);
        typeBox.setItems(FXCollections.observableArrayList(LocationType.values()));
        typeBox.setConverter(Converters.locationType());
        typeBox.setValue(LocationType.PAVILION);
    }

    public void setLocation(Location location) {
        this.editing = location;
        if (location != null) {
            nameField.setText(location.getName());
            addressField.setText(location.getAddress());
            capacityField.setText(String.valueOf(location.getCapacity()));
            typeBox.setValue(location.getType());
        }
    }

    @Override
    public boolean save() {
        errors.reset();
        String name = nameField.getText().trim();
        String address = addressField.getText().trim();
        String capacityText = capacityField.getText().trim();
        LocationType type = typeBox.getValue();

        if (name.isEmpty()) {
            errors.add(nameField, "Название: поле обязательно для заполнения.");
        } else if (name.length() > 150) {
            errors.add(nameField, "Название: не более 150 символов.");
        }

        if (address.isEmpty()) {
            errors.add(addressField, "Адрес: поле обязательно для заполнения.");
        } else if (address.length() > 250) {
            errors.add(addressField, "Адрес: не более 250 символов.");
        }

        int capacity = 0;
        if (capacityText.isEmpty()) {
            errors.add(capacityField, "Вместимость: поле обязательно для заполнения.");
        } else {
            try {
                capacity = Integer.parseInt(capacityText);
                if (capacity < 1 || capacity > MAX_CAPACITY) {
                    errors.add(capacityField, "Вместимость: введите целое число от 1 до " + MAX_CAPACITY + ".");
                }
            } catch (NumberFormatException e) {
                errors.add(capacityField, "Вместимость: введите целое число, например 300.");
            }
        }

        if (type == null) {
            errors.add(typeBox, "Тип: выберите тип локации.");
        }

        if (errors.showIfAny()) return false;

        try {
            if (editing == null) {
                ctx.locationService.create(name, address, capacity, type);
            } else {
                ctx.locationService.update(editing.getId(), name, address, capacity, type);
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
