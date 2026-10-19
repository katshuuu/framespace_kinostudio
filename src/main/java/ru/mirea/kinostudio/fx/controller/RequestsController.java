package ru.mirea.kinostudio.fx.controller;

import javafx.beans.binding.Bindings;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.input.MouseButton;
import javafx.stage.FileChooser;
import ru.mirea.kinostudio.exception.BusinessException;
import ru.mirea.kinostudio.exception.EntityNotFoundException;
import ru.mirea.kinostudio.fx.AppContext;
import ru.mirea.kinostudio.fx.Converters;
import ru.mirea.kinostudio.fx.Dialogs;
import ru.mirea.kinostudio.fx.FormDialogs;
import ru.mirea.kinostudio.model.Location;
import ru.mirea.kinostudio.model.RequestStatus;
import ru.mirea.kinostudio.model.RequestView;
import ru.mirea.kinostudio.service.RequestFilter;
import ru.mirea.kinostudio.util.CsvExporter;
import ru.mirea.kinostudio.util.Labels;
import ru.mirea.kinostudio.util.XlsxExporter;

import java.io.File;
import java.io.IOException;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

/**
 * Вкладка «Заявки»: таблица, поиск, фильтры, сортировка, CRUD, смена статуса, экспорт.
 * Контроллер отвечает только за интерфейс; данные берёт у сервисов, SQL здесь нет.
 */
public class RequestsController {

    /** Варианты сортировки в выпадающем списке. */
    private enum SortOption {
        DATE_ASC("Дата съёмки: сначала ближайшие", Comparator.comparing(RequestView::getShootingDate)),
        DATE_DESC("Дата съёмки: сначала поздние", Comparator.comparing(RequestView::getShootingDate).reversed()),
        BUDGET_DESC("Бюджет: по убыванию", Comparator.comparing(RequestView::getBudget).reversed()),
        BUDGET_ASC("Бюджет: по возрастанию", Comparator.comparing(RequestView::getBudget)),
        TITLE("Название: А → Я", Comparator.comparing((RequestView v) -> v.getTitle().toLowerCase(Locale.ROOT))),
        CLIENT("Клиент: А → Я", Comparator.comparing((RequestView v) -> v.getClientName().toLowerCase(Locale.ROOT)));

        final String label;
        final Comparator<RequestView> comparator;

        SortOption(String label, Comparator<RequestView> comparator) {
            this.label = label;
            this.comparator = comparator;
        }
    }

    private final AppContext ctx = AppContext.get();

    private final ObservableList<RequestView> all = FXCollections.observableArrayList();
    private final FilteredList<RequestView> filtered = new FilteredList<>(all, v -> true);
    private final SortedList<RequestView> sorted = new SortedList<>(filtered);

    @FXML private TextField searchField;
    @FXML private ComboBox<RequestStatus> statusFilter;
    @FXML private ComboBox<Location> locationFilter;
    @FXML private DatePicker fromPicker;
    @FXML private DatePicker toPicker;
    @FXML private ComboBox<SortOption> sortBox;
    @FXML private Label filterHint;
    @FXML private Label countLabel;

    @FXML private TableView<RequestView> table;
    @FXML private TableColumn<RequestView, Integer> idCol;
    @FXML private TableColumn<RequestView, String> titleCol;
    @FXML private TableColumn<RequestView, String> clientCol;
    @FXML private TableColumn<RequestView, String> locationCol;
    @FXML private TableColumn<RequestView, String> dateCol;
    @FXML private TableColumn<RequestView, Integer> durationCol;
    @FXML private TableColumn<RequestView, String> budgetCol;
    @FXML private TableColumn<RequestView, RequestStatus> statusCol;

    @FXML private Button editButton;
    @FXML private Button deleteButton;
    @FXML private Button statusButton;

    @FXML private Label detailTitle;
    @FXML private Label detailDescription;
    @FXML private Label detailClient;
    @FXML private Label detailLocation;

    @FXML
    private void initialize() {
        setupTable();
        setupFilters();
        setupSelection();
        ctx.addListener(this::reload);
        reload();
    }

    // ---------- настройка интерфейса ----------

    private void setupTable() {
        idCol.setCellValueFactory(c -> new javafx.beans.property.SimpleObjectProperty<>(c.getValue().getId()));
        titleCol.setCellValueFactory(c -> new javafx.beans.property.SimpleStringProperty(c.getValue().getTitle()));
        clientCol.setCellValueFactory(c -> new javafx.beans.property.SimpleStringProperty(c.getValue().getClientName()));
        locationCol.setCellValueFactory(c -> new javafx.beans.property.SimpleStringProperty(c.getValue().getLocationName()));
        dateCol.setCellValueFactory(c -> new javafx.beans.property.SimpleStringProperty(Labels.date(c.getValue().getShootingDate())));
        durationCol.setCellValueFactory(c -> new javafx.beans.property.SimpleObjectProperty<>(c.getValue().getDurationHours()));
        budgetCol.setCellValueFactory(c -> new javafx.beans.property.SimpleStringProperty(Labels.money(c.getValue().getBudget())));
        statusCol.setCellValueFactory(c -> new javafx.beans.property.SimpleObjectProperty<>(c.getValue().getStatus()));

        budgetCol.getStyleClass().add("right");
        durationCol.getStyleClass().add("right");
        statusCol.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(RequestStatus item, boolean empty) {
                super.updateItem(item, empty);
                getStyleClass().removeIf(s -> s.startsWith("status-"));
                if (empty || item == null) {
                    setText(null);
                } else {
                    setText(Labels.status(item));
                    getStyleClass().add("status-" + item.name().toLowerCase(Locale.ROOT).replace('_', '-'));
                }
            }
        });

        // Сортировка выбирается в выпадающем списке (а не кликом по заголовку)
        table.getColumns().forEach(c -> c.setSortable(false));
        table.setItems(sorted);
        table.setPlaceholder(new Label("Нет заявок, подходящих под условия"));
        table.setOnMouseClicked(e -> {
            if (e.getButton() == MouseButton.PRIMARY && e.getClickCount() == 2
                    && table.getSelectionModel().getSelectedItem() != null) {
                onEdit();
            }
        });
    }

    private void setupFilters() {
        statusFilter.setItems(FXCollections.observableArrayList(RequestStatus.values()));
        statusFilter.setConverter(Converters.status());
        locationFilter.setItems(ctx.locations());
        locationFilter.setConverter(Converters.location());
        fromPicker.setConverter(Converters.date());
        toPicker.setConverter(Converters.date());

        sortBox.setItems(FXCollections.observableArrayList(SortOption.values()));
        sortBox.setConverter(Converters.of(o -> o.label));
        sortBox.valueProperty().addListener((o, a, b) -> applySort());
        sortBox.setValue(SortOption.DATE_ASC);

        searchField.textProperty().addListener((o, a, b) -> applyFilter());
        statusFilter.valueProperty().addListener((o, a, b) -> applyFilter());
        locationFilter.valueProperty().addListener((o, a, b) -> applyFilter());
        fromPicker.valueProperty().addListener((o, a, b) -> applyFilter());
        toPicker.valueProperty().addListener((o, a, b) -> applyFilter());
        filtered.addListener((javafx.collections.ListChangeListener<RequestView>) c -> updateCount());
    }

    private void setupSelection() {
        var none = Bindings.isNull(table.getSelectionModel().selectedItemProperty());
        editButton.disableProperty().bind(none);
        deleteButton.disableProperty().bind(none);
        statusButton.disableProperty().bind(none);
        table.getSelectionModel().selectedItemProperty().addListener((o, a, b) -> showDetails(b));
        showDetails(null);
    }

    // ---------- данные ----------

    private void reload() {
        Integer keepId = table.getSelectionModel().getSelectedItem() == null
                ? null : table.getSelectionModel().getSelectedItem().getId();
        Location keepLocation = locationFilter.getValue();
        Integer keepLocationId = keepLocation == null ? null : keepLocation.getId();
        try {
            all.setAll(ctx.viewService.loadAll());
        } catch (SQLException e) {
            Dialogs.dbError(Dialogs.ownerOf(table), e);
            return;
        }
        // Справочник локаций обновился (новые объекты) — восстанавливаем выбор в фильтре
        if (keepLocationId != null) {
            ctx.locations().stream().filter(l -> l.getId() == keepLocationId)
                    .findFirst().ifPresent(locationFilter::setValue);
        }
        applyFilter();
        if (keepId != null) {
            sorted.stream().filter(v -> v.getId() == keepId).findFirst()
                    .ifPresent(v -> table.getSelectionModel().select(v));
        }
    }

    private void applyFilter() {
        LocalDate from = fromPicker.getValue();
        LocalDate to = toPicker.getValue();
        Location loc = locationFilter.getValue();
        RequestFilter f = new RequestFilter(searchField.getText(), statusFilter.getValue(),
                loc == null ? null : loc.getId(), from, to);
        if (f.hasInvalidDateRange()) {
            filterHint.setText("Дата «с» не может быть позже даты «по».");
            filterHint.setVisible(true);
            filterHint.setManaged(true);
            filtered.setPredicate(v -> false);
        } else {
            filterHint.setVisible(false);
            filterHint.setManaged(false);
            filtered.setPredicate(f::matches);
        }
        updateCount();
    }

    private void applySort() {
        SortOption o = sortBox.getValue();
        sorted.setComparator(o == null ? null : o.comparator);
    }

    private void updateCount() {
        countLabel.setText("Показано " + filtered.size() + " из " + all.size());
    }

    private void showDetails(RequestView v) {
        if (v == null) {
            detailTitle.setText("Выберите заявку в таблице");
            detailDescription.setText("");
            detailClient.setText("—");
            detailLocation.setText("—");
            return;
        }
        detailTitle.setText(v.getTitle());
        detailDescription.setText(v.getDescription());
        var c = v.getClient();
        detailClient.setText(c == null ? "клиент не найден"
                : c.getFullName() + "\n" + c.getEmail() + "\n" + c.getPhone());
        var l = v.getLocation();
        detailLocation.setText(l == null ? "локация не найдена"
                : l.getName() + " · " + Labels.type(l.getType()) + "\n" + l.getAddress()
                + "\nВместимость: " + l.getCapacity() + " чел.");
    }

    // ---------- обработчики кнопок ----------

    @FXML
    private void onReset() {
        searchField.clear();
        statusFilter.setValue(null);
        locationFilter.setValue(null);
        fromPicker.setValue(null);
        toPicker.setValue(null);
        fromPicker.getEditor().clear();
        toPicker.getEditor().clear();
        sortBox.setValue(SortOption.DATE_ASC);
    }

    @FXML
    private void onAdd() {
        boolean saved = FormDialogs.<RequestDialogController>show(Dialogs.ownerOf(table),
                "Новая заявка", "/fxml/request-dialog.fxml", c -> c.setRequest(null));
        if (saved) ctx.dataChanged();
    }

    @FXML
    private void onEdit() {
        RequestView v = table.getSelectionModel().getSelectedItem();
        if (v == null) return;
        boolean saved = FormDialogs.<RequestDialogController>show(Dialogs.ownerOf(table),
                "Редактирование заявки №" + v.getId(), "/fxml/request-dialog.fxml",
                c -> c.setRequest(v.getRequest()));
        if (saved) ctx.dataChanged();
    }

    @FXML
    private void onDelete() {
        RequestView v = table.getSelectionModel().getSelectedItem();
        if (v == null) return;
        if (!Dialogs.confirm(Dialogs.ownerOf(table),
                "Удалить заявку №" + v.getId() + " «" + v.getTitle() + "»?\nДействие нельзя отменить.",
                "Удалить")) {
            return;
        }
        try {
            ctx.requestService.delete(v.getId());
            ctx.dataChanged();
        } catch (EntityNotFoundException e) {
            Dialogs.error(Dialogs.ownerOf(table), e.getMessage());
            ctx.dataChanged();
        } catch (SQLException e) {
            Dialogs.dbError(Dialogs.ownerOf(table), e);
        }
    }

    @FXML
    private void onChangeStatus() {
        RequestView v = table.getSelectionModel().getSelectedItem();
        if (v == null) return;
        List<RequestStatus> allowed = List.copyOf(ctx.requestService.getAllowedTransitions(v.getStatus()));
        if (allowed.isEmpty()) {
            Dialogs.info(Dialogs.ownerOf(table), "Заявка в статусе «" + Labels.status(v.getStatus())
                    + "» — это конечный статус, изменить его нельзя.");
            return;
        }
        var choice = Dialogs.choose(Dialogs.ownerOf(table), "Смена статуса",
                "Заявка №" + v.getId() + ". Сейчас: " + Labels.status(v.getStatus()),
                allowed.stream().map(Labels::status).toList());
        if (choice.isEmpty()) return;
        RequestStatus target = allowed.stream().filter(s -> Labels.status(s).equals(choice.get()))
                .findFirst().orElse(null);
        if (target == null) return;
        try {
            ctx.requestService.changeStatus(v.getId(), target);
            ctx.dataChanged();
        } catch (BusinessException | EntityNotFoundException e) {
            Dialogs.error(Dialogs.ownerOf(table), e.getMessage());
        } catch (SQLException e) {
            Dialogs.dbError(Dialogs.ownerOf(table), e);
        }
    }

    @FXML
    private void onExport() {
        List<RequestView> rows = List.copyOf(sorted);
        if (rows.isEmpty()) {
            Dialogs.info(Dialogs.ownerOf(table), "Нет данных для экспорта: таблица пуста.");
            return;
        }
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Экспорт заявок");
        chooser.setInitialFileName("zayavki-" + LocalDate.now() + ".xlsx");
        FileChooser.ExtensionFilter xlsx = new FileChooser.ExtensionFilter("Excel (*.xlsx)", "*.xlsx");
        FileChooser.ExtensionFilter csv = new FileChooser.ExtensionFilter("CSV (*.csv)", "*.csv");
        chooser.getExtensionFilters().addAll(xlsx, csv);
        File file = chooser.showSaveDialog(Dialogs.ownerOf(table));
        if (file == null) return; // пользователь отменил выбор

        boolean asCsv = file.getName().toLowerCase(Locale.ROOT).endsWith(".csv")
                || (chooser.getSelectedExtensionFilter() == csv
                && !file.getName().toLowerCase(Locale.ROOT).endsWith(".xlsx"));
        if (!asCsv && !file.getName().toLowerCase(Locale.ROOT).endsWith(".xlsx")) {
            file = new File(file.getParentFile(), file.getName() + ".xlsx");
        } else if (asCsv && !file.getName().toLowerCase(Locale.ROOT).endsWith(".csv")) {
            file = new File(file.getParentFile(), file.getName() + ".csv");
        }
        try {
            if (asCsv) CsvExporter.exportRequests(rows, file.toPath());
            else XlsxExporter.exportRequests(rows, file.toPath());
            Dialogs.info(Dialogs.ownerOf(table), "Экспортировано заявок: " + rows.size() + "\n" + file.getAbsolutePath());
        } catch (IOException e) {
            Dialogs.error(Dialogs.ownerOf(table), "Не удалось сохранить файл: " + e.getMessage()
                    + "\nВозможно, он открыт в другой программе или нет прав на запись.");
        }
    }
}
