package ru.mirea.kinostudio.fx.controller;

import javafx.beans.binding.Bindings;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.input.MouseButton;
import ru.mirea.kinostudio.exception.EntityNotFoundException;
import ru.mirea.kinostudio.fx.AppContext;
import ru.mirea.kinostudio.fx.Converters;
import ru.mirea.kinostudio.fx.Dialogs;
import ru.mirea.kinostudio.fx.FormDialogs;
import ru.mirea.kinostudio.model.Client;
import ru.mirea.kinostudio.model.RequestView;
import ru.mirea.kinostudio.util.Labels;

import java.sql.SQLException;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/** Вкладка «Клиенты»: список, поиск, сортировка, CRUD и заявки выбранного клиента. */
public class ClientsController {

    private enum SortOption {
        NAME("ФИО: А → Я", Comparator.comparing((Client c) -> c.getFullName().toLowerCase(Locale.ROOT))),
        EMAIL("Email: А → Я", Comparator.comparing((Client c) -> c.getEmail().toLowerCase(Locale.ROOT))),
        NEWEST("Сначала новые", Comparator.comparing(Client::getId).reversed());

        final String label;
        final Comparator<Client> comparator;

        SortOption(String label, Comparator<Client> comparator) {
            this.label = label;
            this.comparator = comparator;
        }
    }

    private final AppContext ctx = AppContext.get();

    private final ObservableList<Client> all = FXCollections.observableArrayList();
    private final FilteredList<Client> filtered = new FilteredList<>(all, c -> true);
    private final SortedList<Client> sorted = new SortedList<>(filtered);
    private final ObservableList<RequestView> clientRequests = FXCollections.observableArrayList();
    private Map<Integer, List<RequestView>> requestsByClient = new HashMap<>();

    @FXML private TextField searchField;
    @FXML private ComboBox<SortOption> sortBox;
    @FXML private Label countLabel;
    @FXML private TableView<Client> table;
    @FXML private TableColumn<Client, Integer> idCol;
    @FXML private TableColumn<Client, String> nameCol;
    @FXML private TableColumn<Client, String> emailCol;
    @FXML private TableColumn<Client, String> phoneCol;
    @FXML private TableColumn<Client, Integer> countCol;
    @FXML private Button addButton;
    @FXML private Label readOnlyHint;
    @FXML private Button editButton;
    @FXML private Button deleteButton;
    @FXML private Label relatedTitle;
    @FXML private TableView<RequestView> relatedTable;
    @FXML private TableColumn<RequestView, String> rTitleCol;
    @FXML private TableColumn<RequestView, String> rDateCol;
    @FXML private TableColumn<RequestView, String> rStatusCol;
    @FXML private TableColumn<RequestView, String> rBudgetCol;

    @FXML
    private void initialize() {
        idCol.setCellValueFactory(c -> new SimpleObjectProperty<>(c.getValue().getId()));
        nameCol.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getFullName()));
        emailCol.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getEmail()));
        phoneCol.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getPhone()));
        countCol.setCellValueFactory(c -> new SimpleObjectProperty<>(
                requestsByClient.getOrDefault(c.getValue().getId(), List.of()).size()));
        countCol.getStyleClass().add("right");
        table.getColumns().forEach(c -> c.setSortable(false));
        table.setItems(sorted);
        table.setPlaceholder(new Label("Клиенты не найдены"));
        table.setOnMouseClicked(e -> {
            if (e.getButton() == MouseButton.PRIMARY && e.getClickCount() == 2
                    && table.getSelectionModel().getSelectedItem() != null) {
                onEdit();
            }
        });

        rTitleCol.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getTitle()));
        rDateCol.setCellValueFactory(c -> new SimpleStringProperty(Labels.date(c.getValue().getShootingDate())));
        rStatusCol.setCellValueFactory(c -> new SimpleStringProperty(Labels.status(c.getValue().getStatus())));
        rBudgetCol.setCellValueFactory(c -> new SimpleStringProperty(Labels.money(c.getValue().getBudget())));
        rBudgetCol.getStyleClass().add("right");
        relatedTable.getColumns().forEach(c -> c.setSortable(false));
        relatedTable.setItems(clientRequests);
        relatedTable.setPlaceholder(new Label("У клиента нет заявок"));

        sortBox.setItems(FXCollections.observableArrayList(SortOption.values()));
        sortBox.setConverter(Converters.of(o -> o.label));
        sortBox.valueProperty().addListener((o, a, b) -> sorted.setComparator(b == null ? null : b.comparator));
        sortBox.setValue(SortOption.NAME);

        searchField.textProperty().addListener((o, a, b) -> applyFilter());
        filtered.addListener((javafx.collections.ListChangeListener<Client>) c -> updateCount());

        var none = Bindings.isNull(table.getSelectionModel().selectedItemProperty());
        // Изменять справочник может только администратор; обычный пользователь — только просматривает
        var notAdmin = new javafx.beans.property.SimpleBooleanProperty(!ctx.isAdmin());
        addButton.setDisable(!ctx.isAdmin());
        editButton.disableProperty().bind(none.or(notAdmin));
        deleteButton.disableProperty().bind(none.or(notAdmin));
        readOnlyHint.setVisible(!ctx.isAdmin());
        readOnlyHint.setManaged(!ctx.isAdmin());
        table.getSelectionModel().selectedItemProperty().addListener((o, a, b) -> showRelated(b));
        showRelated(null);

        ctx.addListener(this::reload);
        reload();
    }

    private void reload() {
        Client sel = table.getSelectionModel().getSelectedItem();
        Integer keepId = sel == null ? null : sel.getId();
        try {
            List<RequestView> views = ctx.viewService.loadAll();
            requestsByClient = new HashMap<>();
            for (RequestView v : views) {
                requestsByClient.computeIfAbsent(v.getRequest().getClientId(), k -> new java.util.ArrayList<>()).add(v);
            }
            all.setAll(ctx.clientService.getAll());
        } catch (SQLException e) {
            Dialogs.dbError(Dialogs.ownerOf(table), e);
            return;
        }
        table.refresh(); // счётчик заявок берётся не из модели, поэтому перерисовываем ячейки
        updateCount();
        if (keepId != null) {
            sorted.stream().filter(c -> c.getId() == keepId).findFirst()
                    .ifPresent(c -> table.getSelectionModel().select(c));
        }
        showRelated(table.getSelectionModel().getSelectedItem());
    }

    private void applyFilter() {
        String q = searchField.getText() == null ? "" : searchField.getText().trim().toLowerCase(Locale.ROOT);
        filtered.setPredicate(c -> q.isEmpty()
                || c.getFullName().toLowerCase(Locale.ROOT).contains(q)
                || c.getEmail().toLowerCase(Locale.ROOT).contains(q)
                || c.getPhone().toLowerCase(Locale.ROOT).contains(q));
    }

    private void updateCount() {
        countLabel.setText("Показано " + filtered.size() + " из " + all.size());
    }

    private void showRelated(Client c) {
        if (c == null) {
            relatedTitle.setText("Заявки клиента (выберите клиента)");
            clientRequests.clear();
            return;
        }
        relatedTitle.setText("Заявки клиента: " + c.getFullName());
        clientRequests.setAll(requestsByClient.getOrDefault(c.getId(), List.of()));
    }

    @FXML
    private void onAdd() {
        if (!ctx.isAdmin()) return;
        if (FormDialogs.<ClientDialogController>show(Dialogs.ownerOf(table), "Новый клиент",
                "/fxml/client-dialog.fxml", c -> c.setClient(null))) {
            ctx.dataChanged();
        }
    }

    @FXML
    private void onEdit() {
        if (!ctx.isAdmin()) return;
        Client c = table.getSelectionModel().getSelectedItem();
        if (c == null) return;
        if (FormDialogs.<ClientDialogController>show(Dialogs.ownerOf(table), "Редактирование клиента",
                "/fxml/client-dialog.fxml", d -> d.setClient(c))) {
            ctx.dataChanged();
        }
    }

    @FXML
    private void onDelete() {
        if (!ctx.isAdmin()) return;
        Client c = table.getSelectionModel().getSelectedItem();
        if (c == null) return;
        int n = requestsByClient.getOrDefault(c.getId(), List.of()).size();
        String warning = n == 0 ? ""
                : "\n\nВместе с клиентом будут удалены его заявки: " + n + " шт. (каскадное удаление в БД).";
        if (!Dialogs.confirm(Dialogs.ownerOf(table),
                "Удалить клиента «" + c.getFullName() + "»?" + warning + "\nДействие нельзя отменить.", "Удалить")) {
            return;
        }
        try {
            ctx.clientService.delete(c.getId());
            ctx.dataChanged();
        } catch (EntityNotFoundException e) {
            Dialogs.error(Dialogs.ownerOf(table), e.getMessage());
            ctx.dataChanged();
        } catch (SQLException e) {
            Dialogs.dbError(Dialogs.ownerOf(table), e);
        }
    }
}
